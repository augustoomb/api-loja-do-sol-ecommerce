package com.augustoomb.api_loja_do_sol_ecommerce.security;

import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;


// SERVE PARA VERIFICAR AS PERMISSOE SDO USUARIO AUTENTICADO

/*
CustomUserDetailsService — o Spring Security sabe autenticar,
mas não sabe de onde vêm os usuários (ele foi desenhado para funcionar com UserDetails).
Essa classe implementa UserDetailsService.loadUserByUsername(email) e serve de ponte:
busca o User no banco pelo email e converte em um UserDetails
(com senha BCrypt + roles como SimpleGrantedAuthority).
Assim, tanto o filtro JWT quanto uma futura autenticação por AuthenticationManager
usam a mesma fonte de usuários.
 */

/*
Explicaçao:
é o Balcão de Cadastro. O segurança da porta tem a pulseira,
mas precisa checar no sistema do evento quem é a pessoa daquela pulseira
e quais permissões ela tem (VIP, Comum, Admin).
 */

/*
O Spring Security possui suas próprias estruturas internas para entender o que é um usuário (UserDetails)
 e o que é uma permissão (GrantedAuthority). Ele não sabe como é a sua entidade User no banco de dados.

Esta classe faz a ponte (tradução) entre a tabela de usuários e o que o Spring entende.
 */

@Service
public class CustomUserDetailsService implements UserDetailsService { // // Implementa a interface nativa do Spring Security

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email) // Usa seu UserRepository para buscar o usuário pelo e-mail.
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        // Cria e retorna uma instância do org.springframework.security.core.userdetails.User (uma classe pronta do Spring) contendo e-mail, senha criptografada, se a conta está ativa e a lista de permissões.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                user.getRoles().stream() // Converte as roles (ex: ROLE_ADMIN, ROLE_USER) em objetos SimpleGrantedAuthority, que é o formato de permissões do Spring.
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .collect(Collectors.toList()));
    }
}
