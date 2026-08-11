package com.augustoomb.api_loja_do_sol_ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Address;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Cart;
import com.augustoomb.api_loja_do_sol_ecommerce.model.CartItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Order;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderStatus;
import com.augustoomb.api_loja_do_sol_ecommerce.model.PaymentMethod;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.model.ShippingAddress;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.AddressRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartItemRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.OrderRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CartService cartService;
    @Mock
    private StockService stockService;
    @Mock
    private StripeService stripeService;
    @Mock
    private OrderPaymentService orderPaymentService;

    @InjectMocks
    private CheckoutService checkoutService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User("Cliente", "cliente@test.com", "x");
        user.setId(1L);
        Category category = new Category("Eletrônicos", null);
        category.setId(1L);
        product = new Product("Mouse Gamer", "desc", new BigDecimal("50.00"), 10, null, category);
        product.setId(5L);
        product.setSku("MOUSE-GAMER");
    }

    @Test
    void createCheckoutRejectsEmptyCart() {
        Cart cart = new Cart(user);
        cart.setId(10L);
        when(cartService.getCart(user)).thenReturn(cart);
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> checkoutService.createCheckout(user, new CheckoutRequestDTO()));
        verify(stripeService, never()).createCheckoutSession(anyLong(), any());
    }

    @Test
    void createCheckoutCreatesOrderAndSessionAndClearsCart() {
        Cart cart = new Cart(user);
        cart.setId(10L);
        CartItem item = new CartItem(cart, product, 2);
        Address address = new Address("Rua A", "10", null, "Centro", "São Paulo", "SP", "01000-000", user);
        address.setPrimary(true);
        user.getAddresses().add(address);

        when(cartService.getCart(user)).thenReturn(cart);
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of(item));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(1L);
            }
            return order;
        });

        CheckoutResponseDTO session = new CheckoutResponseDTO();
        session.setSessionId("cs_simulate_1");
        session.setUrl("http://localhost:5173/pedidos/1");
        when(stripeService.createCheckoutSession(anyLong(), any(BigDecimal.class))).thenReturn(session);

        CheckoutResponseDTO response = checkoutService.createCheckout(user, new CheckoutRequestDTO());

        assertEquals("cs_simulate_1", response.getSessionId());
        verify(stripeService).createCheckoutSession(anyLong(), eq(new BigDecimal("100.00")));
        verify(cartItemRepository).deleteByCartId(10L);
    }

    @Test
    void createCheckoutRejectsInsufficientStock() {
        Cart cart = new Cart(user);
        cart.setId(10L);
        CartItem item = new CartItem(cart, product, 99);
        Address address = new Address("Rua A", "10", null, "Centro", "São Paulo", "SP", "01000-000", user);
        address.setPrimary(true);
        user.getAddresses().add(address);

        when(cartService.getCart(user)).thenReturn(cart);
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of(item));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> checkoutService.createCheckout(user, new CheckoutRequestDTO()));
        verify(stripeService, never()).createCheckoutSession(anyLong(), any());
    }

    @Test
    void createCheckoutRejectsWithoutAddress() {
        Cart cart = new Cart(user);
        cart.setId(10L);
        CartItem item = new CartItem(cart, product, 1);

        when(cartService.getCart(user)).thenReturn(cart);
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of(item));

        assertThrows(BusinessException.class, () -> checkoutService.createCheckout(user, new CheckoutRequestDTO()));
    }

    @Test
    void handleWebhookCompletesPaymentWhenPending() {
        Order order = new Order(user,
                new ShippingAddress("Rua A", "10", null, "Centro", "São Paulo", "SP", "01000-000"),
                new BigDecimal("100.00"));
        order.setId(1L);
        order.setStripeSessionId("cs_simulate_1");
        order.getItems().add(new OrderItem(order, product, 2));

        StripeService.SessionInfo info = new StripeService.SessionInfo("cs_simulate_1", PaymentMethod.PIX);
        when(stripeService.verifyWebhook(anyString(), any())).thenReturn(info);
        when(orderRepository.findByStripeSessionId("cs_simulate_1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        checkoutService.handleWebhook("{\"sessionId\":\"cs_simulate_1\",\"paymentMethod\":\"PIX\"}", null);

        verify(orderPaymentService).completePayment(1L);
        verify(orderPaymentService, never()).cancelPayment(anyLong());
        assertEquals(PaymentMethod.PIX, order.getPaymentMethod());
    }

    @Test
    void handleWebhookIgnoresAlreadyPaidOrder() {
        Order order = new Order(user,
                new ShippingAddress("Rua A", "10", null, "Centro", "São Paulo", "SP", "01000-000"),
                BigDecimal.ZERO);
        order.setId(1L);
        order.setStripeSessionId("cs_simulate_1");
        order.setStatus(OrderStatus.PAGO);

        StripeService.SessionInfo info = new StripeService.SessionInfo("cs_simulate_1", PaymentMethod.PIX);
        when(stripeService.verifyWebhook(anyString(), any())).thenReturn(info);
        when(orderRepository.findByStripeSessionId("cs_simulate_1")).thenReturn(Optional.of(order));

        checkoutService.handleWebhook("{}", null);

        verify(orderPaymentService, never()).completePayment(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handleWebhookCancelsOrderAndRefundsWhenStockFails() {
        Order order = new Order(user,
                new ShippingAddress("Rua A", "10", null, "Centro", "São Paulo", "SP", "01000-000"),
                BigDecimal.ZERO);
        order.setId(1L);
        order.setStripeSessionId("cs_simulate_1");

        StripeService.SessionInfo info = new StripeService.SessionInfo("cs_simulate_1", PaymentMethod.PIX);
        when(stripeService.verifyWebhook(anyString(), any())).thenReturn(info);
        when(orderRepository.findByStripeSessionId("cs_simulate_1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new BusinessException("Estoque insuficiente")).when(orderPaymentService).completePayment(1L);

        checkoutService.handleWebhook("{}", null);

        verify(orderPaymentService).cancelPayment(1L);
        verify(stripeService).refund("cs_simulate_1");
    }
}
