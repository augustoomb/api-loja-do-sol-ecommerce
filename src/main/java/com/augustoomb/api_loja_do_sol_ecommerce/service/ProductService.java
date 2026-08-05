package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.CategoryResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CategoryRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResponseDTO> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));
        return toResponseDTO(product);
    }

    public List<ProductResponseDTO> findByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findByEnabledTrue() {
        return productRepository.findByEnabledTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findByNameContaining(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO create(ProductRequestDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + dto.getCategoryId()));

        String sku = resolveSku(dto.getSku(), null, dto.getName());
        ensureSkuAvailable(sku, null);

        Product product = new Product(dto.getName(), dto.getDescription(), dto.getPrice(),
                dto.getStock(), dto.getImageUrl(), category);
        product.setSku(sku);
        product.setMinimumStock(Math.max(dto.getMinimumStock(), 0));

        return toResponseDTO(productRepository.save(product));
    }

    public ProductResponseDTO update(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + dto.getCategoryId()));

        String sku = resolveSku(dto.getSku(), product, dto.getName());
        ensureSkuAvailable(sku, id);

        product.setName(dto.getName());
        product.setSku(sku);
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setMinimumStock(Math.max(dto.getMinimumStock(), 0));
        product.setImageUrl(dto.getImageUrl());
        product.setEnabled(dto.isEnabled());
        product.setCategory(category);

        return toResponseDTO(productRepository.save(product));
    }

    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));
        productRepository.delete(product);
    }

    public ProductResponseDTO toResponseDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setMinimumStock(product.getMinimumStock());
        dto.setImageUrl(product.getImageUrl());
        dto.setEnabled(product.isEnabled());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        CategoryResponseDTO categoryDTO = new CategoryResponseDTO();
        categoryDTO.setId(product.getCategory().getId());
        categoryDTO.setName(product.getCategory().getName());
        categoryDTO.setDescription(product.getCategory().getDescription());
        dto.setCategory(categoryDTO);

        return dto;
    }

    private String resolveSku(String requestedSku, Product current, String name) {
        if (requestedSku == null || requestedSku.isBlank()) {
            if (current != null && current.getSku() != null && !current.getSku().isBlank()) {
                return current.getSku();
            }
            return generateSku(name);
        }
        return requestedSku.trim().toUpperCase();
    }

    private void ensureSkuAvailable(String sku, Long exceptId) {
        productRepository.findBySku(sku).ifPresent(existing -> {
            if (exceptId == null || !existing.getId().equals(exceptId)) {
                throw new BusinessException("Já existe um produto com o SKU: " + sku);
            }
        });
    }

    private String generateSku(String name) {
        String base = Normalizer.normalize(name == null ? "PRODUTO" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("^[-]+|[-]+$", "")
                .toUpperCase();
        if (base.isBlank()) {
            base = "PRODUTO";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
