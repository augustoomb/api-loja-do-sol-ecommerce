package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Address;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Cart;
import com.augustoomb.api_loja_do_sol_ecommerce.model.CartItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Order;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderStatus;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.model.ShippingAddress;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.AddressRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartItemRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.OrderRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.ProductRepository;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CartService cartService;
    private final StockService stockService;
    private final StripeService stripeService;
    private final OrderPaymentService orderPaymentService;

    public CheckoutService(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                           ProductRepository productRepository, AddressRepository addressRepository,
                           CartService cartService, StockService stockService,
                           StripeService stripeService, OrderPaymentService orderPaymentService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.cartService = cartService;
        this.stockService = stockService;
        this.stripeService = stripeService;
        this.orderPaymentService = orderPaymentService;
    }

    @Transactional
    public CheckoutResponseDTO createCheckout(User user, CheckoutRequestDTO dto) {
        Cart cart = cartService.getCart(user);
        List<CartItem> items = cartItemRepository.findByCartIdOrderById(cart.getId());
        if (items.isEmpty()) {
            throw new BusinessException("O carrinho está vazio");
        }

        ShippingAddress shippingAddress = resolveShippingAddress(user, dto.getAddressId());

        BigDecimal subtotal = BigDecimal.ZERO;
        Order order = new Order(user, shippingAddress, BigDecimal.ZERO);
        for (CartItem item : items) {
            Product product = loadAvailableProduct(item.getProduct().getId());
            if (item.getQuantity() > product.getStock()) {
                throw new BusinessException("Estoque insuficiente para o produto: " + product.getName()
                        + " (disponível: " + product.getStock() + ")");
            }
            order.getItems().add(new OrderItem(order, product, item.getQuantity()));
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setSubtotal(subtotal);
        order.setTotal(subtotal);
        orderRepository.save(order);

        // GERA A URL ONDE O USUARIO DEVERA FAZER O PAGAMENTO
        CheckoutResponseDTO session = stripeService.createCheckoutSession(order.getId(), order.getTotal());
        order.setStripeSessionId(session.getSessionId());
        orderRepository.save(order);

        cartItemRepository.deleteByCartId(cart.getId());
        log.info("Checkout criado para o usuário {} (id={}): pedido {}, sessão {}",
                user.getEmail(), user.getId(), order.getId(), session.getSessionId());
        return session;
    }

    // Sem @Transactional: cada operação usa transações próprias, permitindo que a conclusão
    // do pagamento (baixa de estoque + status PAGO) rode em transação isolada e, em caso de
    // falha de estoque, o pedido seja cancelado sem arrastar rollback para o save do status.
    public void handleWebhook(String payload, String signature) {
        StripeService.SessionInfo info = stripeService.verifyWebhook(payload, signature);
        Order order = orderRepository.findByStripeSessionId(info.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado para a sessão: " + info.getSessionId()));
        if (order.getStatus() != OrderStatus.PENDENTE) {
            log.warn("Webhook ignorado: pedido {} não está PENDENTE (status atual={})",
                    order.getId(), order.getStatus());
            return; // idempotente: ignora webhooks duplicados
        }
        order.setPaymentMethod(info.getPaymentMethod());
        orderRepository.save(order);
        log.info("Webhook de pagamento recebido: pedido {} (método={})", order.getId(), info.getPaymentMethod());
        try {
            orderPaymentService.completePayment(order.getId());
        } catch (BusinessException e) {
            log.error("Falha ao baixar estoque do pedido {}. Cancelando o pedido.", order.getId(), e);
            orderPaymentService.cancelPayment(order.getId());
            stripeService.refund(info.getSessionId());
        }
    }

    private ShippingAddress resolveShippingAddress(User user, Long addressId) {
        Address address;
        if (addressId != null) {
            address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com id: " + addressId));
            if (!address.getUser().getId().equals(user.getId())) {
                throw new ResourceNotFoundException("Endereço não encontrado com id: " + addressId);
            }
        } else {
            address = user.getAddresses().stream().filter(Address::isPrimary).findFirst().orElse(null);
            if (address == null) {
                throw new BusinessException("Nenhum endereço de entrega encontrado. Informe um addressId ou cadastre um endereço principal.");
            }
        }
        return new ShippingAddress(address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getCity(), address.getState(), address.getZipcode());
    }

    private Product loadAvailableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + productId));
        if (!product.isEnabled()) {
            throw new BusinessException("Produto indisponível: " + product.getName());
        }
        return product;
    }
}
