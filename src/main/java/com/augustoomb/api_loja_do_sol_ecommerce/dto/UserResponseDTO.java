package com.augustoomb.api_loja_do_sol_ecommerce.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class UserResponseDTO {

    private Long id;
    private String name;
    private String email;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<RoleResponseDTO> roles;
    private Set<AddressResponseDTO> addresses;
    private Set<PhoneResponseDTO> phones;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<RoleResponseDTO> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleResponseDTO> roles) {
        this.roles = roles;
    }

    public Set<AddressResponseDTO> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<AddressResponseDTO> addresses) {
        this.addresses = addresses;
    }

    public Set<PhoneResponseDTO> getPhones() {
        return phones;
    }

    public void setPhones(Set<PhoneResponseDTO> phones) {
        this.phones = phones;
    }
}
