package com.augustoomb.api_loja_do_sol_ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.OrderResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ShipOrderRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Order;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderStatus;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.model.ShippingAddress;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private StockService stockService;
    @Mock
    private StripeService stripeService;

    @InjectMocks
    private OrderService orderService;

    private User owner;
    private User other;
    private Product product;

    @BeforeEach
    void setUp() {
        owner = new User("Dono", "dono@test.com", "x");
        owner.setId(1L);
        other = new User("Outro", "outro@test.com", "x");
        other.setId(2L);
        Category category = new Category("Eletrônicos", null);
        category.setId(1L);
        product = new Product("Mouse Gamer", "desc", new BigDecimal("50.00"), 10, null, category);
        product.setId(5L);
        product.setSku("MOUSE-GAMER");
    }

    private Order pendingOrder() {
        Order order = new Order(owner,
                new ShippingAddress("Rua A", "10", null, "Centro", "São Paulo", "SP", "01000-000"),
                BigDecimal.TEN);
        order.setId(1L);
        return order;
    }

    private Order paidOrder() {
        Order order = pendingOrder();
        order.setStatus(OrderStatus.PAGO);
        order.setStripeSessionId("cs_real_1");
        order.getItems().add(new OrderItem(order, product, 2));
        return order;
    }

    @Test
    void getOrderHidesOtherUsersOrders() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrder(other, 1L, false));
    }

    @Test
    void getOrderAllowsOwnerAndAdmin() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));

        assertEquals(OrderStatus.PENDENTE, orderService.getOrder(owner, 1L, false).getStatus());
        assertEquals(OrderStatus.PENDENTE, orderService.getOrder(other, 1L, true).getStatus());
    }

    @Test
    void cancelPendingOrderByOwner() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDTO response = orderService.cancelOrder(owner, 1L, false);

        assertEquals(OrderStatus.CANCELADO, response.getStatus());
        assertNotNull(response.getCanceledAt());
        verify(stripeService, never()).refund(any());
        verify(stockService, never()).recordEntry(anyLong(), any(StockMovementRequestDTO.class), any());
    }

    @Test
    void cancelOrderByNonOwnerIsHidden() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));

        assertThrows(ResourceNotFoundException.class, () -> orderService.cancelOrder(other, 1L, false));
    }

    @Test
    void cancelPaidOrderByAdminRestocksAndRefunds() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(paidOrder()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDTO response = orderService.cancelOrder(owner, 1L, true);

        assertEquals(OrderStatus.CANCELADO, response.getStatus());
        verify(stripeService).refund("cs_real_1");
        verify(stockService).recordEntry(eq(5L), any(StockMovementRequestDTO.class), isNull());
    }

    @Test
    void cancelPaidOrderByOwnerRequiresAdmin() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(paidOrder()));

        assertThrows(BusinessException.class, () -> orderService.cancelOrder(owner, 1L, false));
    }

    @Test
    void cancelShippedOrderIsRejected() {
        Order order = pendingOrder();
        order.setStatus(OrderStatus.ENVIADO);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> orderService.cancelOrder(owner, 1L, false));
    }

    @Test
    void shipRejectsNonPaidOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder()));
        ShipOrderRequestDTO dto = new ShipOrderRequestDTO();
        dto.setTrackingCode("BR123");

        assertThrows(BusinessException.class, () -> orderService.shipOrder(1L, dto));
    }

    @Test
    void shipMarksPaidOrderAsShipped() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(paidOrder()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        ShipOrderRequestDTO dto = new ShipOrderRequestDTO();
        dto.setTrackingCode("BR123");

        OrderResponseDTO response = orderService.shipOrder(1L, dto);

        assertEquals(OrderStatus.ENVIADO, response.getStatus());
        assertEquals("BR123", response.getTrackingCode());
    }
}
