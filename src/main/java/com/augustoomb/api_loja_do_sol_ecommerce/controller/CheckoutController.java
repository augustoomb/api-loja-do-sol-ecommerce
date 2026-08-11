package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.service.CheckoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/checkout")
@Tag(name = "Checkout", description = "Endpoints para finalização da compra")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final UserRepository userRepository;

    public CheckoutController(CheckoutService checkoutService, UserRepository userRepository) {
        this.checkoutService = checkoutService;
        this.userRepository = userRepository;
    }

    // DEVOLVE A URL ONDE O USUARIO DEVE EFETUAR O PAGAMENTO
    @PostMapping
    @Operation(summary = "Criar sessão de checkout",
            description = "Cria o pedido PENDENTE com o carrinho atual e a sessão de pagamento no Stripe (ou simulada)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sessão de checkout criada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto ou endereço não encontrado"),
            @ApiResponse(responseCode = "422", description = "Carrinho vazio, estoque insuficiente ou sem endereço de entrega"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<CheckoutResponseDTO> createCheckout(
            @RequestBody CheckoutRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkoutService.createCheckout(currentUser(userDetails), dto));
    }

    private User currentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }
}
