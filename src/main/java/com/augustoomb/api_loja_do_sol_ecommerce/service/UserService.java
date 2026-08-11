package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddressResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.PhoneResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.RegisterRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.RoleResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.UserRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.UserResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.EmailAlreadyInUseException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Address;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Phone;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Role;
import com.augustoomb.api_loja_do_sol_ecommerce.model.RoleName;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartItemRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.OrderRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.RoleRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.StockMovementRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockMovementRepository stockMovementRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       OrderRepository orderRepository, CartRepository cartRepository,
                       CartItemRepository cartItemRepository, StockMovementRepository stockMovementRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.stockMovementRepository = stockMovementRepository;
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
        RoleName roleName = resolveRoleName(dto);
        return createUser(dto.getName(), dto.getEmail(), dto.getPassword(),
                dto.getAddresses(), dto.getPhones(), roleName);
    }

    public UserResponseDTO register(RegisterRequestDTO dto) {
        return createUser(dto.getName(), dto.getEmail(), dto.getPassword(),
                dto.getAddresses(), dto.getPhones(), RoleName.ROLE_USER);
    }

    private RoleName resolveRoleName(UserRequestDTO dto) {
        if (dto.getRoleName() == null || dto.getRoleName().isBlank()) {
            throw new BusinessException("É obrigatório informar a role do usuário");
        }

        try {
            return RoleName.valueOf(dto.getRoleName());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Role inválida: " + dto.getRoleName());
        }
    }

    private UserResponseDTO createUser(String name, String email, String password,
            Set<AddressRequestDTO> addresses, Set<PhoneRequestDTO> phones, RoleName roleName) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException("Email já está em uso: " + email);
        }

        User user = new User(name, email, passwordEncoder.encode(password));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada: " + roleName));
        user.setRoles(Collections.singleton(role));

        if (addresses != null) {
            user.setAddresses(addresses.stream()
                    .map(addrDTO -> toAddressEntity(addrDTO, user))
                    .collect(Collectors.toSet()));
        }

        if (phones != null) {
            user.setPhones(phones.stream()
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

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));

        if (orderRepository.existsByUserId(id)) {
            throw new BusinessException("O usuário possui pedidos e não pode ser excluído (histórico de vendas). "
                    + "Considere desabilitá-lo.");
        }

        cartRepository.findByUserId(id).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            cartRepository.delete(cart);
        });

        stockMovementRepository.disassociateUser(id);

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
