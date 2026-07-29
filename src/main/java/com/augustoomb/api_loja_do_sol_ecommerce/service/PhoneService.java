package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.model.Phone;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.PhoneRepository;

@Service
public class PhoneService {

    private final PhoneRepository phoneRepository;

    public PhoneService(PhoneRepository phoneRepository) {
        this.phoneRepository = phoneRepository;
    }

    public List<Phone> findAll() {
        return phoneRepository.findAll();
    }

    public Phone findById(Long id) {
        return phoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Telefone não encontrado com id: " + id));
    }

    public List<Phone> findByUserId(Long userId) {
        return phoneRepository.findByUserId(userId);
    }

    public Phone create(Phone phone) {
        return phoneRepository.save(phone);
    }

    public Phone update(Long id, Phone phoneDetails) {
        Phone phone = findById(id);
        phone.setDdd(phoneDetails.getDdd());
        phone.setNumber(phoneDetails.getNumber());
        phone.setPrimary(phoneDetails.isPrimary());
        return phoneRepository.save(phone);
    }

    public void delete(Long id) {
        Phone phone = findById(id);
        phoneRepository.delete(phone);
    }
}
