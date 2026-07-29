package com.augustoomb.api_loja_do_sol_ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String apiDescription = """
                API REST do e-commerce Loja do Sol.
                Gerenciamento de usuários, endereços, telefones e perfis de acesso.
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("API Loja do Sol - E-commerce")
                        .version("0.0.1-SNAPSHOT")
                        .description(apiDescription)
                        .contact(new Contact()
                                .name("Augusto Barbosa"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Servidor local de desenvolvimento"));
    }
}
