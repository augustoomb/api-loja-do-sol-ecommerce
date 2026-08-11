package com.augustoomb.api_loja_do_sol_ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddToCartRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.OrderResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.RegisterRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.model.MovementType;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderStatus;
import com.augustoomb.api_loja_do_sol_ecommerce.model.PaymentMethod;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CategoryRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.StockMovementRepository;

@SpringBootTest
@Transactional
class OrderIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartService cartService;
    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StockMovementRepository stockMovementRepository;

    private Category category;
    private User user;

    @BeforeEach
    void setUp() {
        category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new Category("Categoria Vendas", "criada em teste")));

        RegisterRequestDTO register = new RegisterRequestDTO();
        register.setName("Cliente Vendas");
        register.setEmail("cliente-vendas-" + System.nanoTime() + "@test.com");
        register.setPassword("senha123");

        AddressRequestDTO address = new AddressRequestDTO();
        address.setStreet("Rua das Flores");
        address.setNumber("100");
        address.setNeighborhood("Centro");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipcode("01000-000");
        address.setPrimary(true);
        register.setAddresses(Collections.singleton(address));

        var created = userService.register(register);
        user = userRepository.findByEmail(created.getEmail()).orElseThrow();
    }

    @Test
    void checkoutFlowCompletesPaymentAndDeductsStock() {
        ProductResponseDTO product = createProduct("SKU-VENDA", 10);

        AddToCartRequestDTO add = new AddToCartRequestDTO();
        add.setProductId(product.getId());
        add.setQuantity(3);
        cartService.addItem(user, add);

        CheckoutResponseDTO session = checkoutService.createCheckout(user, new CheckoutRequestDTO());
        assertNotNull(session.getSessionId());
        assertEquals(0, cartService.getCartResponse(user).getItems().size());

        checkoutService.handleWebhook(
                "{\"sessionId\":\"" + session.getSessionId() + "\",\"paymentMethod\":\"PIX\"}", null);

        List<OrderResponseDTO> orders = orderService.listMyOrders(user);
        assertEquals(1, orders.size());
        OrderResponseDTO order = orders.get(0);
        assertEquals(OrderStatus.PAGO, order.getStatus());
        assertEquals(PaymentMethod.PIX, order.getPaymentMethod());
        assertEquals(new BigDecimal("150.00"), order.getTotal());
        assertEquals(1, order.getItems().size());
        assertEquals("SKU-VENDA", order.getItems().get(0).getProductSku());

        assertEquals(7, productService.findById(product.getId()).getStock());
        assertEquals(1, stockMovementRepository.findByProductIdOrderByCreatedAtDesc(product.getId()).size());
        assertEquals(MovementType.SAIDA,
                stockMovementRepository.findByProductIdOrderByCreatedAtDesc(product.getId()).get(0).getMovementType());
    }

    @Test
    void pendingOrderCanBeCancelledWithoutTouchingStock() {
        ProductResponseDTO product = createProduct("SKU-CANCELA", 5);

        AddToCartRequestDTO add = new AddToCartRequestDTO();
        add.setProductId(product.getId());
        add.setQuantity(1);
        cartService.addItem(user, add);

        checkoutService.createCheckout(user, new CheckoutRequestDTO());

        OrderResponseDTO order = orderService.listMyOrders(user).get(0);
        orderService.cancelOrder(user, order.getId(), false);

        assertEquals(OrderStatus.CANCELADO, orderService.getOrder(user, order.getId(), false).getStatus());
        assertEquals(5, productService.findById(product.getId()).getStock());
        assertEquals(0, stockMovementRepository.findByProductIdOrderByCreatedAtDesc(product.getId()).size());
    }

    @Test
    void checkoutIsRejectedWhenCartIsEmpty() {
        CheckoutRequestDTO dto = new CheckoutRequestDTO();
        org.junit.jupiter.api.Assertions.assertThrows(
                com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException.class,
                () -> checkoutService.createCheckout(user, dto));
    }

    private ProductResponseDTO createProduct(String sku, int stock) {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Produto " + sku);
        dto.setSku(sku);
        dto.setPrice(new BigDecimal("50.00"));
        dto.setStock(stock);
        dto.setMinimumStock(0);
        dto.setEnabled(true);
        dto.setCategoryId(category.getId());
        return productService.create(dto);
    }
}
