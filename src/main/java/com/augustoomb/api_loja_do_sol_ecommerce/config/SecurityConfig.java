package com.augustoomb.api_loja_do_sol_ecommerce.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.augustoomb.api_loja_do_sol_ecommerce.security.JwtAuthenticationFilter;

@Configuration // VAI SER CARREGADA NA INICIALIZAÇAO DA APLICAÇAO
@EnableWebSecurity // Ativa os recursos de segurança do Spring Security na aplicação e permite personalizar o fluxo das requisições web.
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // DESABILITA POIS SO E UTIL EM APLICAÇOES QUE USAM COOKIES DE NAVEGADOR
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // API REST não mantém sessão no servidor
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll() // Login e registro público
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // Docs públicas
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll() // Healthcheck e métricas públicos: o Prometheus raspa o /actuator/prometheus pela rede interna do Docker; demais endpoints do actuator ficam restritos a ADMIN
                        .requestMatchers("/api/payments/webhook").permitAll() // Webhook do Stripe: a assinatura é validada no serviço
                        .requestMatchers("/api/products/low-stock", "/api/products/*/stock/**", "/api/stock/**").hasRole("ADMIN") // Operações e consultas de estoque: somente ADMIN
                        .requestMatchers("/api/admin/orders/**").hasRole("ADMIN") // Gestão administrativa de pedidos: somente ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").authenticated() // Leitura de produtos/categorias: ADMIN e USER
                        .requestMatchers("/api/addresses/**").authenticated() // Endereços: ADMIN e USER (propriedade validada no serviço)
                        .requestMatchers("/api/cart/**", "/api/checkout/**", "/api/orders/**").authenticated() // Carrinho, checkout e pedidos: ADMIN e USER
                        .anyRequest().hasRole("ADMIN")) // Restante: somente ADMIN
                .formLogin(form -> form.disable()) // Desativa a página de login em HTML gerada automaticamente pelo Spring Security.
                .httpBasic(basic -> basic.disable()) // Desativa a autenticação do tipo HTTP Basic (aquela janela nativa do navegador que pede usuário e senha via cabeçalho HTTP).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(","))); // PEGA allowedOrigins do .env e parte o texto. Define exatamente quais origens (domínios/portas do frontend) têm autorização para chamar a API.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // Libera as ações HTTP padrão. Options e pra uso interno do navegador.
        config.setAllowedHeaders(List.of("*")); // * autoriza o frontend a enviar qualquer cabeçalho na requisição (Content-Type, Authorization, X-Custom-Header, etc.).
        config.setMaxAge(3600L); // Define por quanto tempo (em segundos) o navegador pode guardar em cache a resposta de permissão CORS. 3600L=1hora; o navegador fará a requisição de verificação (OPTIONS) apenas 1 vez por hora para cada endpoint. Isso economiza requisições desnecessárias ao servidor e deixa o frontend mais rápido.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // aplica todas essas regras de CORS para todas as rotas/endpoints
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
