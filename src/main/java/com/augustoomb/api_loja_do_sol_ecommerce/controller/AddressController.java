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
import com.augustoomb.api_loja_do_sol_ecommerce.model.RoleName;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Endereços", description = "Endpoints para gerenciamento de endereços")
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    public AddressController(AddressService addressService, UserRepository userRepository) {
        this.addressService = addressService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Listar endereços", description = "Lista todos os endereços (ADMIN) ou os endereços do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de endereços retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<AddressResponseDTO>> findAll(@AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        return ResponseEntity.ok(addressService.findAll(user, isAdmin(user)));
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
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        return ResponseEntity.ok(addressService.findById(id, user, isAdmin(user)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Buscar endereços por ID do usuário",
            description = "ADMIN pode buscar de qualquer usuário; cliente autenticado apenas os próprios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereços encontrados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereços não encontrados"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<AddressResponseDTO>> findByUserId(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        return ResponseEntity.ok(addressService.findByUserId(userId, user, isAdmin(user)));
    }

    @PostMapping
    @Operation(summary = "Criar novo endereço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Endereço criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<AddressResponseDTO> create(
            @RequestBody AddressRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(dto, user, isAdmin(user)));
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
            @RequestBody AddressRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        return ResponseEntity.ok(addressService.update(id, dto, user, isAdmin(user)));
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
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUser(userDetails);
        addressService.delete(id, user, isAdmin(user));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    private User currentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }
}
