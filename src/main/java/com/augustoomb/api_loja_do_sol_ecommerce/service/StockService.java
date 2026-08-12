package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockAdjustmentRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockSummaryDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.MovementType;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.model.StockMovement;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.ProductRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.StockMovementRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;

    public StockService(ProductRepository productRepository,
                        StockMovementRepository stockMovementRepository,
                        ProductService productService) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productService = productService;
    }

    @Transactional // GARANTE ATOMICIDADE (OU TUDO DO BLOCO FUNCIONA, OU TUDO EH DESFEITO)
    @CacheEvict(cacheNames = "products", allEntries = true) // estoque mudou → invalidar catálogo cacheado
    public StockMovementResponseDTO recordEntry(Long productId, StockMovementRequestDTO dto, User user) {
        int quantity = validatePositive(dto.getQuantity());
        Product product = loadProduct(productId);
        productRepository.increaseStock(productId, quantity);
        product = loadProduct(productId);
        StockMovement movement = new StockMovement(MovementType.ENTRADA, quantity, dto.getReason(),
                dto.getReference(), product, user);
        StockMovementResponseDTO response = toResponseDTO(stockMovementRepository.save(movement));
        log.info("Estoque: entrada de {} unidade(s) no produto {} (id={}) motivo='{}' por {}",
                quantity, product.getName(), productId, dto.getReason(), userName(user));
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true) // estoque mudou → invalidar catálogo cacheado
    public StockMovementResponseDTO recordWithdrawal(Long productId, StockMovementRequestDTO dto, User user) {
        int quantity = validatePositive(dto.getQuantity());
        Product product = loadProduct(productId);
        int updated = productRepository.decreaseStockIfAvailable(productId, quantity);
        if (updated == 0) {
            throw new BusinessException("Estoque insuficiente para o produto: " + product.getName());
        }
        product = loadProduct(productId);
        StockMovement movement = new StockMovement(MovementType.SAIDA, quantity, dto.getReason(),
                dto.getReference(), product, user);
        StockMovementResponseDTO response = toResponseDTO(stockMovementRepository.save(movement));
        log.info("Estoque: saída de {} unidade(s) do produto {} (id={}) motivo='{}' por {}",
                quantity, product.getName(), productId, dto.getReason(), userName(user));
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true) // estoque mudou → invalidar catálogo cacheado
    public StockMovementResponseDTO recordAdjustment(Long productId, StockAdjustmentRequestDTO dto, User user) {
        if (dto.getNewStock() < 0) {
            throw new BusinessException("O novo estoque não pode ser negativo");
        }
        Product product = loadProduct(productId);
        int delta = dto.getNewStock() - product.getStock();
        productRepository.setStock(productId, dto.getNewStock());
        product = loadProduct(productId);
        StockMovement movement = new StockMovement(MovementType.AJUSTE, delta, dto.getReason(),
                dto.getReference(), product, user);
        StockMovementResponseDTO response = toResponseDTO(stockMovementRepository.save(movement));
        log.info("Estoque: ajuste do produto {} (id={}) para {} unidade(s) (delta={}) motivo='{}' por {}",
                product.getName(), productId, dto.getNewStock(), delta, dto.getReason(), userName(user));
        return response;
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> findMovementsByProduct(Long productId) {
        loadProduct(productId);
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> findMovements(Long productId, MovementType type,
                                                        LocalDateTime from, LocalDateTime to) {
        return stockMovementRepository.findAll(buildSpecification(productId, type, from, to)).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findLowStock() {
        return productRepository.findByStockLessThanEqualMinimumStock().stream()
                .map(productService::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockSummaryDTO getSummary() {
        StockSummaryDTO dto = new StockSummaryDTO();
        dto.setTotalProducts(productRepository.count());
        dto.setTotalUnitsInStock(productRepository.sumStock());
        dto.setLowStockProducts(productRepository.countLowStock());
        dto.setOutOfStockProducts(productRepository.countByStock(0));
        return dto;
    }

    private Specification<StockMovement> buildSpecification(Long productId, MovementType type,
                                                            LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (productId != null) {
                predicates.add(criteriaBuilder.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("movementType"), type));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + productId));
    }

    private int validatePositive(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("A quantidade deve ser maior que zero");
        }
        return quantity;
    }

    private String userName(User user) {
        return user != null ? user.getName() : "sistema";
    }

    private StockMovementResponseDTO toResponseDTO(StockMovement movement) {
        StockMovementResponseDTO dto = new StockMovementResponseDTO();
        dto.setId(movement.getId());
        dto.setMovementType(movement.getMovementType());
        dto.setQuantity(movement.getQuantity());
        dto.setReason(movement.getReason());
        dto.setReference(movement.getReference());
        dto.setProductId(movement.getProduct().getId());
        dto.setProductName(movement.getProduct().getName());
        if (movement.getUser() != null) {
            dto.setUserId(movement.getUser().getId());
            dto.setUserName(movement.getUser().getName());
        }
        dto.setCreatedAt(movement.getCreatedAt());
        return dto;
    }
}
