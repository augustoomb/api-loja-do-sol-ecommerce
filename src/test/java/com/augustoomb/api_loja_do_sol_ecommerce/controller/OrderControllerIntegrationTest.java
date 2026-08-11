package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    @Test
    void fullCheckoutFlowUntilShipment() throws Exception {
        String adminToken = login(adminEmail, adminPassword);
        String clientToken = registerAndLoginClient("cliente-flow-");
        String categoryId = createResource("/api/categories", adminToken,
                "{\"name\":\"Categoria Pedido\",\"description\":\"via teste\"}");
        String productId = createResource("/api/products", adminToken,
                "{\"name\":\"Produto Pedido\",\"sku\":\"SKU-PEDIDO\",\"price\":50.00,"
                        + "\"stock\":10,\"minimumStock\":0,\"enabled\":true,\"categoryId\":" + categoryId + "}");

        expectStatus("/api/cart/items", "POST", clientToken,
                "{\"productId\":" + productId + ",\"quantity\":2}", 201);

        String checkoutBody = mockMvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(checkoutBody).get("sessionId").asText();

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sessionId + "\",\"paymentMethod\":\"PIX\"}"))
                .andExpect(status().isOk());

        JsonNode orders = readArray("/api/orders", clientToken);
        assertEquals(1, orders.size());
        assertEquals("PAGO", orders.get(0).get("status").asText());
        assertEquals("PIX", orders.get(0).get("paymentMethod").asText());
        assertEquals(0, new BigDecimal(orders.get(0).get("total").asText())
                .compareTo(new BigDecimal("100.00")));
        assertEquals("Rua A", orders.get(0).get("street").asText());
        String orderId = orders.get(0).get("id").asText();

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingCode\":\"BR123\"}"))
                .andExpect(status().isOk());

        JsonNode shipped = readJson("/api/orders/" + orderId, clientToken);
        assertEquals("ENVIADO", shipped.get("status").asText());
        assertEquals("BR123", shipped.get("trackingCode").asText());
    }

    @Test
    void ordersAreIsolatedBetweenUsersAndAdminOnly() throws Exception {
        String adminToken = login(adminEmail, adminPassword);
        String clientOne = registerAndLoginClient("cliente-um-");
        String clientTwo = registerAndLoginClient("cliente-dois-");
        String categoryId = createResource("/api/categories", adminToken,
                "{\"name\":\"Categoria Isolamento\",\"description\":\"via teste\"}");
        String productId = createResource("/api/products", adminToken,
                "{\"name\":\"Produto Isolado\",\"sku\":\"SKU-ISOLADO\",\"price\":30.00,"
                        + "\"stock\":5,\"minimumStock\":0,\"enabled\":true,\"categoryId\":" + categoryId + "}");

        expectStatus("/api/cart/items", "POST", clientOne,
                "{\"productId\":" + productId + ",\"quantity\":1}", 201);
        String checkoutBody = mockMvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + clientOne)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(checkoutBody).get("sessionId").asText();
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sessionId + "\",\"paymentMethod\":\"CARTAO\"}"))
                .andExpect(status().isOk());

        String orderId = readArray("/api/orders", clientOne).get(0).get("id").asText();

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + clientTwo))
                .andExpect(status().isNotFound());

        expectStatus("/api/admin/orders", "GET", clientTwo, null, 403);
        expectStatus("/api/admin/orders", "GET", adminToken, null, 200);
        expectStatus("/api/orders/" + orderId + "/cancel", "POST", adminToken, null, 200);
    }

    private String registerAndLoginClient(String prefix) throws Exception {
        String email = prefix + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente\",\"email\":\"" + email + "\",\"password\":\"senha123\","
                                + "\"addresses\":[{\"street\":\"Rua A\",\"number\":\"10\","
                                + "\"neighborhood\":\"Centro\",\"city\":\"São Paulo\",\"state\":\"SP\","
                                + "\"zipcode\":\"01000-000\",\"primary\":true}]}"))
                .andExpect(status().isCreated());
        return login(email, "senha123");
    }

    private String createResource(String url, String token, String json) throws Exception {
        String body = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void expectStatus(String url, String method, String token, String json, int expected)
            throws Exception {
        var request = "GET".equals(method)
                ? get(url)
                : post(url);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        mockMvc.perform(request).andExpect(status().is(expected));
    }

    private JsonNode readArray(String url, String token) throws Exception {
        String body = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode readJson(String url, String token) throws Exception {
        String body = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
