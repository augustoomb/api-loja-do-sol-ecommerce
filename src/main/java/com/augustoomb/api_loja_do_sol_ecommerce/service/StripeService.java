package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.CheckoutResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final boolean simulate;
    private final String frontendUrl;
    private final String webhookSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripeService(@Value("${app.stripe.secret-key:}") String secretKey,
                         @Value("${app.stripe.webhook-secret:}") String webhookSecret,
                         @Value("${app.stripe.simulate:true}") boolean simulate,
                         @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.webhookSecret = webhookSecret;
        this.simulate = simulate;
        this.frontendUrl = frontendUrl;
        Stripe.apiKey = secretKey;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public CheckoutResponseDTO createCheckoutSession(Long orderId, BigDecimal total) {
        if (simulate) {
            CheckoutResponseDTO dto = new CheckoutResponseDTO();
            dto.setOrderId(orderId);
            dto.setSessionId("cs_simulate_" + orderId);
            dto.setUrl(frontendUrl + "/pedidos/" + orderId);
            return dto;
        }
        Long amount = total.movePointRight(2).longValueExact();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/pedidos/" + orderId + "?success=true")
                .setCancelUrl(frontendUrl + "/carrinho")
                .putMetadata("orderId", String.valueOf(orderId))
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("brl")
                                .setUnitAmount(amount)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Pedido #" + orderId)
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            Session session = Session.create(params);
            CheckoutResponseDTO dto = new CheckoutResponseDTO();
            dto.setOrderId(orderId);
            dto.setSessionId(session.getId());
            dto.setUrl(session.getUrl());
            return dto;
        } catch (StripeException e) {
            log.error("Falha ao criar a sessão de checkout no Stripe para o pedido {}", orderId, e);
            throw new BusinessException("Falha ao criar a sessão de pagamento no Stripe");
        }
    }

    public SessionInfo verifyWebhook(String payload, String signature) {
        if (simulate) {
            try {
                JsonNode json = objectMapper.readTree(payload);
                String sessionId = json.path("sessionId").asText(null);
                if (sessionId == null || sessionId.isBlank()) {
                    throw new BusinessException("Payload de simulação inválido: sessionId ausente");
                }
                String method = json.path("paymentMethod").asText(null);
                return new SessionInfo(sessionId, resolvePaymentMethod(method));
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException("Payload de simulação inválido");
            }
        }
        if (signature == null || signature.isBlank() || webhookSecret == null || webhookSecret.isBlank()) {
            throw new BusinessException("Assinatura do webhook ausente ou chave não configurada");
        }
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            StripeObject data = event.getDataObjectDeserializer().deserializeUnsafe();
            if (!(data instanceof Session session)) {
                throw new BusinessException("Evento sem sessão válida");
            }
            String method = session.getPaymentMethodTypes() == null || session.getPaymentMethodTypes().isEmpty()
                    ? null
                    : session.getPaymentMethodTypes().get(0);
            return new SessionInfo(session.getId(), resolvePaymentMethod(method));
        } catch (StripeException e) {
            log.error("Falha ao validar a assinatura do webhook", e);
            throw new BusinessException("Falha ao validar a assinatura do webhook");
        }
    }

    public void refund(String sessionId) {
        if (simulate) {
            log.info("Modo simulado: reembolso ignorado para a sessão {}", sessionId);
            return;
        }
        try {
            Session session = Session.retrieve(sessionId);
            String paymentIntent = session.getPaymentIntent();
            if (paymentIntent != null) {
                Refund.create(RefundCreateParams.builder().setPaymentIntent(paymentIntent).build());
            }
        } catch (StripeException e) {
            log.error("Falha ao reembolsar a sessão {}", sessionId, e);
        }
    }

    private PaymentMethod resolvePaymentMethod(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return switch (type.toLowerCase()) {
            case "pix" -> PaymentMethod.PIX;
            case "boleto" -> PaymentMethod.BOLETO;
            default -> PaymentMethod.CARTAO;
        };
    }

    public static class SessionInfo {

        private final String sessionId;
        private final PaymentMethod paymentMethod;

        public SessionInfo(String sessionId, PaymentMethod paymentMethod) {
            this.sessionId = sessionId;
            this.paymentMethod = paymentMethod;
        }

        public String getSessionId() {
            return sessionId;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }
    }
}
