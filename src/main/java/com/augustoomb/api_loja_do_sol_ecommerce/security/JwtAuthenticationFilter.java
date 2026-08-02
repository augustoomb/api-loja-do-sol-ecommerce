package com.augustoomb.api_loja_do_sol_ecommerce.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


// SERVE PARA AUTENTICAR O USUARIO COM O TOKEN QUE ELE ENVIOU NA REQUISIÇAO

/*
 JwtAuthenticationFilter: anotada com @Component, também registrada pelo scan.
 O único lugar que a usa é o SecurityConfig.java:22, que a injeta no construtor
  e a coloca na cadeia com addFilterBefore(...) — Spring chama doFilterInternal
  automaticamente em cada requisição.
 */


/*
Explicaçao:
A cada requisição, ele para o usuário, verifica se ele tem uma pulseira válida
 (o Token JWT) e decide se deixa ele entrar ou não.
 */

// 1. Herda de OncePerRequestFilter para ter certeza de que esse filtro
// roda APENAS UMA VEZ por requisição HTTP.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            String email = jwtService.extractEmail(token);

            // Se o e-mail existe e o usuário ainda não está autenticado nesta requisição e o token é válido:
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtService.isTokenValid(token)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email); // Usa o userDetailsService para buscar os dados completos do usuário no banco.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken( // Cria um novo objeto de autenticação do Spring
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication); // guarda as informaçoes no SecurityContextHolder
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

/*
O que é o SecurityContextHolder?
    É a "memória temporária" do Spring para a requisição atual.
    Quando você coloca a autenticação ali, o Spring passa a saber:
    "Pronto, durante esta requisição, o usuário X está autenticado e tem a permissão Y".
 */