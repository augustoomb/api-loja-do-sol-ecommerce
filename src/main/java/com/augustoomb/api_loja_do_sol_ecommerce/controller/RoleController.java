package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.RoleRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.RoleResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.service.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Perfis", description = "Endpoints para gerenciamento de perfis")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os perfis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de perfis retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<RoleResponseDTO>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar perfil por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<RoleResponseDTO> findById(
            @Parameter(description = "ID do perfil", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Criar novo perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<RoleResponseDTO> create(@RequestBody RoleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do perfil", example = "1")
            @PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
