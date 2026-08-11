package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.OrderItemResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.OrderResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ShipOrderRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Order;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderStatus;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final StripeService stripeService;

    public OrderService(OrderRepository orderRepository, StockService stockService, StripeService stripeService) {
        this.orderRepository = orderRepository;
        this.stockService = stockService;
        this.stripeService = stripeService;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> listMyOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> listAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(User user, Long orderId, boolean admin) {
        Order order = loadOrder(orderId);
        if (!admin && !order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Pedido não encontrado com id: " + orderId);
        }
        return toResponseDTO(order);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(User user, Long orderId, boolean admin) {
        Order order = loadOrder(orderId);
        if (!admin && !order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Pedido não encontrado com id: " + orderId);
        }
        if (order.getStatus() == OrderStatus.CANCELADO) {
            throw new BusinessException("O pedido já está cancelado");
        }
        if (order.getStatus() == OrderStatus.ENVIADO) {
            throw new BusinessException("O pedido já foi enviado e não pode ser cancelado");
        }
        if (order.getStatus() == OrderStatus.PAGO) {
            if (!admin) {
                throw new BusinessException("Somente um administrador pode cancelar um pedido já pago");
            }
            restock(order);
            stripeService.refund(order.getStripeSessionId());
        }
        order.setStatus(OrderStatus.CANCELADO);
        order.setCanceledAt(LocalDateTime.now());
        return toResponseDTO(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDTO shipOrder(Long orderId, ShipOrderRequestDTO dto) {
        Order order = loadOrder(orderId);
        if (order.getStatus() != OrderStatus.PAGO) {
            throw new BusinessException("Somente pedidos pagos podem ser enviados");
        }
        order.setTrackingCode(dto.getTrackingCode());
        order.setStatus(OrderStatus.ENVIADO);
        return toResponseDTO(orderRepository.save(order));
    }

    private void restock(Order order) {
        for (OrderItem item : order.getItems()) {
            StockMovementRequestDTO movement = new StockMovementRequestDTO();
            movement.setQuantity(item.getQuantity());
            movement.setReason("Cancelamento de venda");
            movement.setReference(String.valueOf(order.getId()));
            stockService.recordEntry(item.getProduct().getId(), movement, null);
        }
    }

    private Order loadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com id: " + orderId));
    }

    private OrderResponseDTO toResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus());
        dto.setSubtotal(order.getSubtotal());
        dto.setTotal(order.getTotal());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setTrackingCode(order.getTrackingCode());
        if (order.getShippingAddress() != null) {
            dto.setStreet(order.getShippingAddress().getStreet());
            dto.setNumber(order.getShippingAddress().getNumber());
            dto.setComplement(order.getShippingAddress().getComplement());
            dto.setNeighborhood(order.getShippingAddress().getNeighborhood());
            dto.setCity(order.getShippingAddress().getCity());
            dto.setState(order.getShippingAddress().getState());
            dto.setZipcode(order.getShippingAddress().getZipcode());
        }
        dto.setItems(order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .map(this::toItemResponseDTO)
                .collect(Collectors.toList()));
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCanceledAt(order.getCanceledAt());
        return dto;
    }

    private OrderItemResponseDTO toItemResponseDTO(OrderItem item) {
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProductName());
        dto.setProductSku(item.getProductSku());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }
}
