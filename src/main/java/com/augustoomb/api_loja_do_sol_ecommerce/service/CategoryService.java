package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.CategoryRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CategoryResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponseDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public CategoryResponseDTO findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
        return toResponseDTO(category);
    }

    public CategoryResponseDTO findByName(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com nome: " + name));
        return toResponseDTO(category);
    }

    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        Category category = new Category(dto.getName(), dto.getDescription());
        return toResponseDTO(categoryRepository.save(category));
    }

    public CategoryResponseDTO update(Long id, CategoryRequestDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        return toResponseDTO(categoryRepository.save(category));
    }

    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
        categoryRepository.delete(category);
    }

    private CategoryResponseDTO toResponseDTO(Category category) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }
}
