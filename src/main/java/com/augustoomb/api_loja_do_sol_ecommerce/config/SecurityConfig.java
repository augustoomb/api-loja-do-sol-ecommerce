package com.augustoomb.api_loja_do_sol_ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // VAI SER CARREGADA NA INICIALIZAÇAO DA APLICAÇAO
@EnableWebSecurity // Ativa os recursos de segurança do Spring Security na aplicação e permite personalizar o fluxo das requisições web.
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // DESABILITA POIS SO E UTIL EM APLICAÇOES QUE USAM COOKIES DE NAVEGADOR
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // TODO: Ajustar depois para permitir somente req. autenticadas
                .formLogin(form -> form.disable()) // Desativa a página de login em HTML gerada automaticamente pelo Spring Security.
                .httpBasic(basic -> basic.disable()); // Desativa a autenticação do tipo HTTP Basic (aquela janela nativa do navegador que pede usuário e senha via cabeçalho HTTP).
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
