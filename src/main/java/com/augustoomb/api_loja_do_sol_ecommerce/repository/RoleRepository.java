package com.augustoomb.api_loja_do_sol_ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.augustoomb.api_loja_do_sol_ecommerce.model.Role;
import com.augustoomb.api_loja_do_sol_ecommerce.model.RoleName;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
