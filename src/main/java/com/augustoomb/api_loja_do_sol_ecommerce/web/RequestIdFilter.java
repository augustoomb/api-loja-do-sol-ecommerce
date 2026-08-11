package com.augustoomb.api_loja_do_sol_ecommerce.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // é a primeira coisa que executa assim que uma requisição bate na API, antes mesmo do Spring Security ou dos Controllers. (A primeira barreira HTTP)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String USER_ID_MDC_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER); // verifica se o cliente (como um frontend ou API Gateway) já enviou um header X-Request-Id. Se não veio nada, ele gera um UUID único.
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID_MDC_KEY, requestId); // Ao colocar requestId no MDC, absolutamente qualquer log disparado durante essa requisição (seja no filtro, no controller ou no repositório) imprimirá automaticamente esse ID.
        response.setHeader(REQUEST_ID_HEADER, requestId); // Insere o X-Request-Id no header de resposta da API para que o cliente saiba qual ID foi gerado.
        try { // Se o Spring Security rejeitar a requisição antes de chegar ao Controller (ex: token inválido ou falta de permissão), o interceptor não rodaria. Por isso, este filtro verifica o status no final da cadeia e loga um WARN para requisições não autorizadas.
            filterChain.doFilter(request, response);
            if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED
                    || response.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
                log.warn("Requisição sem autorização: {} {} -> {}", request.getMethod(), request.getRequestURI(),
                        response.getStatus());
            }
        } finally { // Se não limpar o MDC no bloco finally, a thread será reusada em outra requisição carregando o requestId antigo, contaminando os logs futuros.
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }
}
