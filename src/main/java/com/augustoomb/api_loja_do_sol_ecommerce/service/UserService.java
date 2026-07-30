package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.RoleResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.UserRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.UserResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.EmailAlreadyInUseException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Address;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Phone;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
        return toResponseDTO(user);
    }

    public UserResponseDTO findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
        return toResponseDTO(user);
    }

    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyInUseException("Email já está em uso: " + dto.getEmail());
        }

        User user = new User(dto.getName(), dto.getEmail(), dto.getPassword());

        if (dto.getAddresses() != null) {
            user.setAddresses(dto.getAddresses().stream()
                    .map(addrDTO -> toAddressEntity(addrDTO, user))
                    .collect(Collectors.toSet()));
        }

        if (dto.getPhones() != null) {
            user.setPhones(dto.getPhones().stream()
                    .map(phoneDTO -> toPhoneEntity(phoneDTO, user))
                    .collect(Collectors.toSet()));
        }

        return toResponseDTO(userRepository.save(user));
    }

    public UserResponseDTO update(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        return toResponseDTO(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
        userRepository.delete(user);
    }

    private UserResponseDTO toResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(role -> {
                        RoleResponseDTO roleDTO = new RoleResponseDTO();
                        roleDTO.setId(role.getId());
                        roleDTO.setName(role.getName().name());
                        return roleDTO;
                    })
                    .collect(Collectors.toSet()));
        }

        if (user.getAddresses() != null) {
            dto.setAddresses(user.getAddresses().stream()
                    .map(addr -> {
                        AddressResponseDTO addrDTO = new AddressResponseDTO();
                        addrDTO.setId(addr.getId());
                        addrDTO.setStreet(addr.getStreet());
                        addrDTO.setNumber(addr.getNumber());
                        addrDTO.setComplement(addr.getComplement());
                        addrDTO.setNeighborhood(addr.getNeighborhood());
                        addrDTO.setCity(addr.getCity());
                        addrDTO.setState(addr.getState());
                        addrDTO.setZipcode(addr.getZipcode());
                        addrDTO.setPrimary(addr.isPrimary());
                        return addrDTO;
                    })
                    .collect(Collectors.toSet()));
        }

        if (user.getPhones() != null) {
            dto.setPhones(user.getPhones().stream()
                    .map(phone -> {
                        PhoneResponseDTO phoneDTO = new PhoneResponseDTO();
                        phoneDTO.setId(phone.getId());
                        phoneDTO.setDdd(phone.getDdd());
                        phoneDTO.setNumber(phone.getNumber());
                        phoneDTO.setPrimary(phone.isPrimary());
                        return phoneDTO;
                    })
                    .collect(Collectors.toSet()));
        }

        return dto;
    }

    private Address toAddressEntity(AddressRequestDTO dto, User user) {
        Address address = new Address();
        address.setStreet(dto.getStreet());
        address.setNumber(dto.getNumber());
        address.setComplement(dto.getComplement());
        address.setNeighborhood(dto.getNeighborhood());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipcode(dto.getZipcode());
        address.setPrimary(dto.isPrimary());
        address.setUser(user);
        return address;
    }

    private Phone toPhoneEntity(PhoneRequestDTO dto, User user) {
        Phone phone = new Phone();
        phone.setDdd(dto.getDdd());
        phone.setNumber(dto.getNumber());
        phone.setPrimary(dto.isPrimary());
        phone.setUser(user);
        return phone;
    }
}
