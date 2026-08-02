package com.augustoomb.api_loja_do_sol_ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.augustoomb.api_loja_do_sol_ecommerce.security.JwtAuthenticationFilter;

@Configuration // VAI SER CARREGADA NA INICIALIZAÇAO DA APLICAÇAO
@EnableWebSecurity // Ativa os recursos de segurança do Spring Security na aplicação e permite personalizar o fluxo das requisições web.
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // DESABILITA POIS SO E UTIL EM APLICAÇOES QUE USAM COOKIES DE NAVEGADOR
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // API REST não mantém sessão no servidor
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll() // Login e registro público
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // Docs públicas
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").authenticated() // Leitura de produtos/categorias: ADMIN e USER
                        .anyRequest().hasRole("ADMIN")) // Restante: somente ADMIN
                .formLogin(form -> form.disable()) // Desativa a página de login em HTML gerada automaticamente pelo Spring Security.
                .httpBasic(basic -> basic.disable()) // Desativa a autenticação do tipo HTTP Basic (aquela janela nativa do navegador que pede usuário e senha via cabeçalho HTTP).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
