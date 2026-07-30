package com.augustoomb.api_loja_do_sol_ecommerce.dto;

import com.augustoomb.api_loja_do_sol_ecommerce.model.RoleName;

public class RoleRequestDTO {

    private RoleName name;

    public RoleName getName() {
        return name;
    }

    public void setName(RoleName name) {
        this.name = name;
    }
}
