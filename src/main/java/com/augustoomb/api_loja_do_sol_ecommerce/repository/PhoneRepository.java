package com.augustoomb.api_loja_do_sol_ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.augustoomb.api_loja_do_sol_ecommerce.model.Phone;

@Repository
public interface PhoneRepository extends JpaRepository<Phone, Long> {

    List<Phone> findByUserId(Long userId);
}
