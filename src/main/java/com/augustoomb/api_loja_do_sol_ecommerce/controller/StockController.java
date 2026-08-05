package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockAdjustmentRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockSummaryDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.model.MovementType;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.service.StockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Estoque", description = "Endpoints para controle de estoque e movimentações")
public class StockController {

    private final StockService stockService;
    private final UserRepository userRepository;

    public StockController(StockService stockService, UserRepository userRepository) {
        this.stockService = stockService;
        this.userRepository = userRepository;
    }

    @PostMapping("/products/{productId}/stock/entries")
    @Operation(summary = "Registrar entrada de estoque",
            description = "Adiciona unidades ao saldo do produto e registra a movimentação")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entrada registrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "422", description = "Quantidade inválida"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<StockMovementResponseDTO> recordEntry(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long productId,
            @RequestBody StockMovementRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockService.recordEntry(productId, dto, currentUser(userDetails)));
    }

    @PostMapping("/products/{productId}/stock/withdrawals")
    @Operation(summary = "Registrar saída de estoque",
            description = "Remove unidades do saldo do produto e registra a movimentação")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Saída registrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "422", description = "Quantidade inválida ou estoque insuficiente"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<StockMovementResponseDTO> recordWithdrawal(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long productId,
            @RequestBody StockMovementRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockService.recordWithdrawal(productId, dto, currentUser(userDetails)));
    }

    @PostMapping("/products/{productId}/stock/adjustments")
    @Operation(summary = "Ajustar estoque",
            description = "Corrige o saldo para o valor informado e registra a movimentação com o delta calculado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ajuste registrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "422", description = "Novo estoque inválido"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<StockMovementResponseDTO> recordAdjustment(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long productId,
            @RequestBody StockAdjustmentRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockService.recordAdjustment(productId, dto, currentUser(userDetails)));
    }

    @GetMapping("/products/{productId}/stock/movements")
    @Operation(summary = "Histórico de movimentações do produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimentações retornadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<StockMovementResponseDTO>> findMovementsByProduct(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long productId) {
        return ResponseEntity.ok(stockService.findMovementsByProduct(productId));
    }

    @GetMapping("/products/low-stock")
    @Operation(summary = "Listar produtos com estoque baixo",
            description = "Retorna os produtos cujo saldo é menor ou igual ao estoque mínimo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de produtos com estoque baixo"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ProductResponseDTO>> findLowStock() {
        return ResponseEntity.ok(stockService.findLowStock());
    }

    @GetMapping("/stock/movements")
    @Operation(summary = "Listar movimentações de estoque",
            description = "Retorna as movimentações com filtros opcionais por produto, tipo e período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimentações retornadas com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<StockMovementResponseDTO>> findMovements(
            @Parameter(description = "ID do produto", example = "1")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Tipo de movimentação (ENTRADA, SAIDA, AJUSTE)")
            @RequestParam(required = false) MovementType type,
            @Parameter(description = "Data inicial (ISO, ex: 2026-08-01T00:00:00)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Data final (ISO, ex: 2026-08-31T23:59:59)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(stockService.findMovements(productId, type, from, to));
    }

    @GetMapping("/stock/summary")
    @Operation(summary = "Resumo do estoque",
            description = "Retorna totais de produtos, unidades em estoque, produtos com estoque baixo e zerados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<StockSummaryDTO> getSummary() {
        return ResponseEntity.ok(stockService.getSummary());
    }

    private User currentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }
}
