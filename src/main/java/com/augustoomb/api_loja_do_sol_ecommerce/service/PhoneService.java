package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Phone;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.PhoneRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;

@Service
public class PhoneService {

    private final PhoneRepository phoneRepository;
    private final UserRepository userRepository;

    public PhoneService(PhoneRepository phoneRepository, UserRepository userRepository) {
        this.phoneRepository = phoneRepository;
        this.userRepository = userRepository;
    }

    public List<PhoneResponseDTO> findAll() {
        return phoneRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public PhoneResponseDTO findById(Long id) {
        Phone phone = phoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Telefone não encontrado com id: " + id));
        return toResponseDTO(phone);
    }

    public List<PhoneResponseDTO> findByUserId(Long userId) {
        return phoneRepository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public PhoneResponseDTO create(PhoneRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + dto.getUserId()));

        Phone phone = new Phone();
        phone.setDdd(dto.getDdd());
        phone.setNumber(dto.getNumber());
        phone.setPrimary(dto.isPrimary());
        phone.setUser(user);

        return toResponseDTO(phoneRepository.save(phone));
    }

    public PhoneResponseDTO update(Long id, PhoneRequestDTO dto) {
        Phone phone = phoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Telefone não encontrado com id: " + id));

        phone.setDdd(dto.getDdd());
        phone.setNumber(dto.getNumber());
        phone.setPrimary(dto.isPrimary());

        return toResponseDTO(phoneRepository.save(phone));
    }

    public void delete(Long id) {
        Phone phone = phoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Telefone não encontrado com id: " + id));
        phoneRepository.delete(phone);
    }

    private PhoneResponseDTO toResponseDTO(Phone phone) {
        PhoneResponseDTO dto = new PhoneResponseDTO();
        dto.setId(phone.getId());
        dto.setDdd(phone.getDdd());
        dto.setNumber(phone.getNumber());
        dto.setPrimary(phone.isPrimary());
        return dto;
    }
}
