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

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Endereços", description = "Endpoints para gerenciamento de endereços")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os endereços")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de endereços retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<AddressResponseDTO>> findAll() {
        return ResponseEntity.ok(addressService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar endereço por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AddressResponseDTO> findById(
            @Parameter(description = "ID do endereço", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.findById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Buscar endereços por ID do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereços encontrados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<AddressResponseDTO>> findByUserId(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(addressService.findByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Criar novo endereço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Endereço criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AddressResponseDTO> create(@RequestBody AddressRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar endereço")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AddressResponseDTO> update(
            @Parameter(description = "ID do endereço", example = "1")
            @PathVariable Long id,
            @RequestBody AddressRequestDTO dto) {
        return ResponseEntity.ok(addressService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar endereço")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Endereço deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do endereço", example = "1")
            @PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
