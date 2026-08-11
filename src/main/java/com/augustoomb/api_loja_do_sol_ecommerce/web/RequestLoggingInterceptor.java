package com.augustoomb.api_loja_do_sol_ecommerce.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import com.augustoomb.api_loja_do_sol_ecommerce.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// (O medidor de performance do Controller)
// Diferente do Filter, um HandlerInterceptor roda dentro do contexto do Spring MVC, bem ao redor dos seus @RestController.
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) { // (Antes do Controller):
        request.setAttribute("startTime", System.currentTimeMillis()); // Salva o timestamp atual (startTime) nas propriedades da requisição (HttpServletRequest.setAttribute) para calcular a duração no final.
        Long userId = extractUserId(); // Tenta extrair o ID do usuário autenticado no Spring Security e ...
        if (userId != null) {
            MDC.put(RequestIdFilter.USER_ID_MDC_KEY, userId.toString()); // ... coloca o userId no MDC.
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, // (Depois do Controller responder):
                                Exception ex) {
        Long start = (Long) request.getAttribute("startTime");
        long duration = start != null ? System.currentTimeMillis() - start : -1; // Calcula a duração exata da requisição
        Long userId = extractUserId();
        if (ex != null) { // Dispara o log definitivo do ciclo da requisição (METODO, URI, STATUS HTTP, TEMPO EM MS, USER ID). Se houve exceção unhandled, loga como ERROR com a stacktrace; caso contrário, loga como INFO.
            log.error("{} {} -> {} ({}ms) user={} erro={}", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration, userId, ex.getMessage(), ex);
        } else {
            log.info("{} {} -> {} ({}ms) user={}", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration, userId);
        }
    }

    private Long extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getId();
    }
}
