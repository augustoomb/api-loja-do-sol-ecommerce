package com.augustoomb.api_loja_do_sol_ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.augustoomb.api_loja_do_sol_ecommerce.web.RequestLoggingInterceptor;

// classe de configuração que avisa o Spring para ativar o RequestLoggingInterceptor

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor())
                .addPathPatterns("/**") // Ativa o log de requisições para todas as URLs da API
                .excludePathPatterns( // Ignora rotas de infraestrutura e documentação
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html");
    }
}
