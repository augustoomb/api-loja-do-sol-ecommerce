package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.model.Address;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.AddressRepository;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Address> findAll() {
        return addressRepository.findAll();
    }

    public Address findById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado com id: " + id));
    }

    public List<Address> findByUserId(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    public Address create(Address address) {
        return addressRepository.save(address);
    }

    public Address update(Long id, Address addressDetails) {
        Address address = findById(id);
        address.setStreet(addressDetails.getStreet());
        address.setNumber(addressDetails.getNumber());
        address.setComplement(addressDetails.getComplement());
        address.setNeighborhood(addressDetails.getNeighborhood());
        address.setCity(addressDetails.getCity());
        address.setState(addressDetails.getState());
        address.setZipcode(addressDetails.getZipcode());
        address.setPrimary(addressDetails.isPrimary());
        return addressRepository.save(address);
    }

    public void delete(Long id) {
        Address address = findById(id);
        addressRepository.delete(address);
    }
}
