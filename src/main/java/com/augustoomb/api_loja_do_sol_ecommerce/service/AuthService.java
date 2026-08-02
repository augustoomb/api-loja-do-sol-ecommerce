package com.augustoomb.api_loja_do_sol_ecommerce.service;

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
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Usuário desabilitado");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRoles());
        return new LoginResponseDTO(token, "Bearer", jwtService.getExpiration());
    }
}
