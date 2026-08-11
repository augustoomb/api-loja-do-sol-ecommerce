package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Order;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.OrderStatus;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.OrderRepository;

@Service
public class OrderPaymentService {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentService.class);

    private final OrderRepository orderRepository;
    private final StockService stockService;

    public OrderPaymentService(OrderRepository orderRepository, StockService stockService) {
        this.orderRepository = orderRepository;
        this.stockService = stockService;
    }

    @Transactional
    public void completePayment(Long orderId) {
        Order order = loadOrder(orderId);
        for (OrderItem item : order.getItems()) {
            StockMovementRequestDTO movement = new StockMovementRequestDTO();
            movement.setQuantity(item.getQuantity());
            movement.setReason("Venda");
            movement.setReference(String.valueOf(order.getId()));
            stockService.recordWithdrawal(item.getProduct().getId(), movement, null);
        }
        order.setStatus(OrderStatus.PAGO);
        orderRepository.save(order);
        log.info("Pedido {} pago (pagamento confirmado)", orderId);
    }

    @Transactional
    public void cancelPayment(Long orderId) {
        Order order = loadOrder(orderId);
        order.setStatus(OrderStatus.CANCELADO);
        order.setCanceledAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Pedido {} cancelado por falha de estoque", orderId);
    }

    private Order loadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com id: " + orderId));
    }
}
