package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.RoleRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.RoleResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Role;
import com.augustoomb.api_loja_do_sol_ecommerce.model.RoleName;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.RoleRepository;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleResponseDTO> findAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public RoleResponseDTO findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada com id: " + id));
        return toResponseDTO(role);
    }

    public RoleResponseDTO findByName(RoleName name) {
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada com nome: " + name));
        return toResponseDTO(role);
    }

    public RoleResponseDTO create(RoleRequestDTO dto) {
        Role role = new Role(dto.getName());
        return toResponseDTO(roleRepository.save(role));
    }

    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada com id: " + id));
        roleRepository.delete(role);
    }

    private RoleResponseDTO toResponseDTO(Role role) {
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(role.getId());
        dto.setName(role.getName().name());
        return dto;
    }
}
