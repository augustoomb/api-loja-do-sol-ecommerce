package com.augustoomb.api_loja_do_sol_ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.service.PhoneService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/phones")
@Tag(name = "Telefones", description = "Endpoints para gerenciamento de telefones")
public class PhoneController {

    private final PhoneService phoneService;

    public PhoneController(PhoneService phoneService) {
        this.phoneService = phoneService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os telefones")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de telefones retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<PhoneResponseDTO>> findAll() {
        return ResponseEntity.ok(phoneService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar telefone por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telefone encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Telefone não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PhoneResponseDTO> findById(
            @Parameter(description = "ID do telefone", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(phoneService.findById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Buscar telefones por ID do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telefones encontrados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<PhoneResponseDTO>> findByUserId(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(phoneService.findByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Criar novo telefone")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Telefone criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PhoneResponseDTO> create(@RequestBody PhoneRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phoneService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar telefone")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telefone atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Telefone não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PhoneResponseDTO> update(
            @Parameter(description = "ID do telefone", example = "1")
            @PathVariable Long id,
            @RequestBody PhoneRequestDTO dto) {
        return ResponseEntity.ok(phoneService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar telefone")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Telefone deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Telefone não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do telefone", example = "1")
            @PathVariable Long id) {
        phoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
