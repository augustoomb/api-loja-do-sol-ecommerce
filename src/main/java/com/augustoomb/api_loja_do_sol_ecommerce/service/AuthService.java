package com.augustoomb.api_loja_do_sol_ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.LoginRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.LoginResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.InvalidCredentialsException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.security.JwtService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Tentativa de login falhou: usuário não encontrado: {}", dto.getEmail());
                    return new InvalidCredentialsException("Credenciais inválidas");
                });

        if (!user.isEnabled()) {
            log.warn("Tentativa de login de usuário desabilitado: {}", dto.getEmail());
            throw new InvalidCredentialsException("Usuário desabilitado");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("Tentativa de login falhou: senha inválida para {}", dto.getEmail());
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRoles());
        log.info("Login realizado com sucesso: {} (id={})", user.getEmail(), user.getId());
        return new LoginResponseDTO(token, "Bearer", jwtService.getExpiration());
    }
}
