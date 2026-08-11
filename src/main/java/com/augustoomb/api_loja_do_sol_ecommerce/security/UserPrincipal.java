package com.augustoomb.api_loja_do_sol_ecommerce.security;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.augustoomb.api_loja_do_sol_ecommerce.model.User;

/*
O Spring Security não conhece diretamente a entidade User do JPA.
O UserPrincipal encapsula o modelo User para que o framework saiba extrair
 login (getEmail()), senha (getPassword()), permissões (getAuthorities()) e status da conta.
 */

/*
No RequestLoggingInterceptor, o trecho (auth.getPrincipal() instanceof UserPrincipal principal)
usa essa classe para ler com segurança e em memória o getId() do usuário sem precisar fazer novas consultas no banco de dados.
 */

public class UserPrincipal implements UserDetails { // implementação da interface UserDetails nativa do Spring Security

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
