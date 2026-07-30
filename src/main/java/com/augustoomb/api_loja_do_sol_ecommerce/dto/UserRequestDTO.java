package com.augustoomb.api_loja_do_sol_ecommerce.dto;

import java.util.Set;

public class UserRequestDTO {

    private String name;
    private String email;
    private String password;
    private Set<AddressRequestDTO> addresses;
    private Set<PhoneRequestDTO> phones;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<AddressRequestDTO> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<AddressRequestDTO> addresses) {
        this.addresses = addresses;
    }

    public Set<PhoneRequestDTO> getPhones() {
        return phones;
    }

    public void setPhones(Set<PhoneRequestDTO> phones) {
        this.phones = phones;
    }
}
