package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Address;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.AddressRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressResponseDTO> findAll(User current, boolean admin) {
        if (admin) {
            return addressRepository.findAll().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
        }
        return addressRepository.findByUserId(current.getId()).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public AddressResponseDTO findById(Long id, User current, boolean admin) {
        Address address = loadOwned(id, current, admin);
        return toResponseDTO(address);
    }

    public List<AddressResponseDTO> findByUserId(Long userId, User current, boolean admin) {
        if (!admin && !current.getId().equals(userId)) {
            throw new ResourceNotFoundException("Endereços não encontrados para o usuário: " + userId);
        }
        return addressRepository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public AddressResponseDTO create(AddressRequestDTO dto, User current, boolean admin) {
        User user = admin ? userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + dto.getUserId()))
                : current;

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

        return toResponseDTO(addressRepository.save(address));
    }

    public AddressResponseDTO update(Long id, AddressRequestDTO dto, User current, boolean admin) {
        Address address = loadOwned(id, current, admin);

        address.setStreet(dto.getStreet());
        address.setNumber(dto.getNumber());
        address.setComplement(dto.getComplement());
        address.setNeighborhood(dto.getNeighborhood());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipcode(dto.getZipcode());
        address.setPrimary(dto.isPrimary());

        return toResponseDTO(addressRepository.save(address));
    }

    public void delete(Long id, User current, boolean admin) {
        Address address = loadOwned(id, current, admin);
        addressRepository.delete(address);
    }

    private Address loadOwned(Long id, User current, boolean admin) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com id: " + id));
        if (!admin && !address.getUser().getId().equals(current.getId())) {
            throw new ResourceNotFoundException("Endereço não encontrado com id: " + id);
        }
        return address;
    }

    private AddressResponseDTO toResponseDTO(Address address) {
        AddressResponseDTO dto = new AddressResponseDTO();
        dto.setId(address.getId());
        dto.setStreet(address.getStreet());
        dto.setNumber(address.getNumber());
        dto.setComplement(address.getComplement());
        dto.setNeighborhood(address.getNeighborhood());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setZipcode(address.getZipcode());
        dto.setPrimary(address.isPrimary());
        return dto;
    }
}
