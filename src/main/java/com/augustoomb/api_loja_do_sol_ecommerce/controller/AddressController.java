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

import com.augustoomb.api_loja_do_sol_ecommerce.model.Address;
import com.augustoomb.api_loja_do_sol_ecommerce.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Endereços", description = "Endpoints para gerenciamento de endereços dos usuários")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os endereços", description = "Retorna uma lista com todos os endereços cadastrados no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de endereços retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<Address>> findAll() {
        return ResponseEntity.ok(addressService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar endereço por ID", description = "Retorna um endereço específico pelo seu identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Address> findById(
            @Parameter(description = "ID do endereço a ser buscado", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.findById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Listar endereços por usuário", description = "Retorna todos os endereços associados a um usuário específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de endereços do usuário retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<Address>> findByUserId(
            @Parameter(description = "ID do usuário para buscar seus endereços", example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(addressService.findByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Criar novo endereço", description = "Cadastra um novo endereço vinculado a um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Endereço criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Address> create(@RequestBody Address address) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(address));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar endereço", description = "Atualiza os dados de um endereço existente pelo seu identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Address> update(
            @Parameter(description = "ID do endereço a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody Address address) {
        return ResponseEntity.ok(addressService.update(id, address));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar endereço", description = "Remove um endereço do sistema pelo seu identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Endereço deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do endereço a ser deletado", example = "1")
            @PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
