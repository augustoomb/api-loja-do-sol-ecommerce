package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.OrderResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ShipOrderRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Pedidos (Admin)", description = "Endpoints administrativos de pedidos")
public class AdminOrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public AdminOrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todos os pedidos", description = "Retorna todos os pedidos da loja do mais recente para o mais antigo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos retornados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<OrderResponseDTO>> listAllOrders() {
        return ResponseEntity.ok(orderService.listAllOrders());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Buscar pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<OrderResponseDTO> getOrder(
            @Parameter(description = "ID do pedido", example = "1")
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        return ResponseEntity.ok(orderService.getOrder(user, orderId, true));
    }

    @PatchMapping("/{orderId}/ship")
    @Operation(summary = "Marcar pedido como enviado", description = "Altera o status de PAGO para ENVIADO e grava o código de rastreio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido marcado como enviado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Pedido não está pago"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<OrderResponseDTO> shipOrder(
            @Parameter(description = "ID do pedido", example = "1")
            @PathVariable Long orderId,
            @RequestBody ShipOrderRequestDTO dto) {
        return ResponseEntity.ok(orderService.shipOrder(orderId, dto));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancelar pedido",
            description = "Cancela um pedido. Pedidos PAGOS são reembolsados no Stripe e o estoque é devolvido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Status não permite cancelamento"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @Parameter(description = "ID do pedido", example = "1")
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        return ResponseEntity.ok(orderService.cancelOrder(user, orderId, true));
    }

    private User currentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }
}
