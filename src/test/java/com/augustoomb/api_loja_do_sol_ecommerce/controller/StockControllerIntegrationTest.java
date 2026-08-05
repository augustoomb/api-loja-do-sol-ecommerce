package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class StockControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    @Test
    void adminCanManageStockEndToEnd() throws Exception {
        String adminToken = login(adminEmail, adminPassword);

        String categoryId = createResource("/api/categories", adminToken,
                "{\"name\":\"Categoria Teste\",\"description\":\"via teste\"}");

        String productId = createResource("/api/products", adminToken,
                "{\"name\":\"Produto Teste\",\"sku\":\"SKU-CONTROLLER\",\"price\":150.00,"
                        + "\"stock\":0,\"minimumStock\":2,\"enabled\":true,\"categoryId\":" + categoryId + "}");

        expectStatus("/api/products/" + productId + "/stock/entries", "POST", adminToken,
                "{\"quantity\":10,\"reason\":\"Compra inicial\"}", 201);
        expectStatus("/api/products/" + productId + "/stock/withdrawals", "POST", adminToken,
                "{\"quantity\":4,\"reason\":\"Venda\"}", 201);
        expectStatus("/api/products/" + productId + "/stock/adjustments", "POST", adminToken,
                "{\"newStock\":8,\"reason\":\"Inventário\"}", 201);

        expectStatus("/api/products/" + productId + "/stock/withdrawals", "POST", adminToken,
                "{\"quantity\":999,\"reason\":\"Venda\"}", 422);

        assertEquals(3, readArray("/api/products/" + productId + "/stock/movements", adminToken).size());
        expectStatus("/api/products/low-stock", "GET", adminToken, null, 200);
        expectStatus("/api/stock/movements?type=ENTRADA", "GET", adminToken, null, 200);
        expectStatus("/api/stock/movements?productId=" + productId + "&type=SAIDA", "GET", adminToken, null, 200);
        expectStatus("/api/stock/summary", "GET", adminToken, null, 200);
    }

    @Test
    void clientIsForbiddenFromStockOperations() throws Exception {
        String clientEmail = "cliente-teste-" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente\",\"email\":\"" + clientEmail + "\",\"password\":\"senha123\"}"))
                .andExpect(status().isCreated());
        String clientToken = login(clientEmail, "senha123");

        expectStatus("/api/stock/summary", "GET", clientToken, null, 403);
        expectStatus("/api/stock/movements", "GET", clientToken, null, 403);

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
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

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
