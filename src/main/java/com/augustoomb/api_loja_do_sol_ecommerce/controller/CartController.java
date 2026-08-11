package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddToCartRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CartResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.UpdateCartItemRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Carrinho", description = "Endpoints do carrinho de compras do usuário autenticado")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Ver carrinho", description = "Retorna os itens do carrinho e o total calculado no servidor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrinho retornado com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<CartResponseDTO> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.getCartResponse(currentUser(userDetails)));
    }

    @PostMapping("/items")
    @Operation(summary = "Adicionar item ao carrinho")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item adicionado ao carrinho"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "422", description = "Quantidade inválida ou estoque insuficiente"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<CartResponseDTO> addItem(
            @RequestBody AddToCartRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(currentUser(userDetails), dto));
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Atualizar quantidade de um item do carrinho")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado no carrinho"),
            @ApiResponse(responseCode = "422", description = "Quantidade inválida ou estoque insuficiente"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<CartResponseDTO> updateItem(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long productId,
            @RequestBody UpdateCartItemRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.updateItem(currentUser(userDetails), productId, dto));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remover item do carrinho")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado no carrinho"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<CartResponseDTO> removeItem(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.removeItem(currentUser(userDetails), productId));
    }

    @DeleteMapping
    @Operation(summary = "Esvaziar carrinho")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrinho esvaziado com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<CartResponseDTO> clear(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.clear(currentUser(userDetails)));
    }

    private User currentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }
}
