package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.augustoomb.api_loja_do_sol_ecommerce.service.CheckoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


// rodar no terminal para receber notificaçao (se estiver em uso local):
    // stripe listen --forward-to http://localhost:8080/api/payments/webhook

// rota eh chamada quando ha movimentaçao de pagamento no link que foi gerado no checkout
@RestController
@RequestMapping("/api/payments/webhook")
@Tag(name = "Webhook", description = "Endpoint de notificação de pagamento do Stripe")
public class WebhookController {

    private final CheckoutService checkoutService;

    public WebhookController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    @Operation(summary = "Receber evento de pagamento",
            description = "Endpoint chamado pelo Stripe (ou pela simulação local) ao concluir um checkout. " +
                    "A assinatura Stripe-Signature é validada fora do modo simulado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento processado com sucesso"),
            @ApiResponse(responseCode = "422", description = "Assinatura inválida ou pedido não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        checkoutService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
