package com.augustoomb.api_loja_do_sol_ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockAdjustmentRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockSummaryDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.model.MovementType;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.model.StockMovement;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.ProductRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private ProductService productService;

    @InjectMocks
    private StockService stockService;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = new Category("Eletrônicos", null);
        category.setId(1L);
        product = new Product("Teclado Mecânico", "desc", new BigDecimal("150.00"), 6, null, category);
        product.setId(1L);
        product.setMinimumStock(2);
    }

    @Test
    void recordEntryIncrementsStockAndSavesMovement() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.increaseStock(1L, 10)).thenReturn(1);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementRequestDTO dto = new StockMovementRequestDTO();
        dto.setQuantity(10);
        dto.setReason("Compra inicial");

        StockMovementResponseDTO response = stockService.recordEntry(1L, dto, null);

        verify(productRepository).increaseStock(1L, 10);
        assertEquals(MovementType.ENTRADA, response.getMovementType());
        assertEquals(10, response.getQuantity());
        assertEquals("Compra inicial", response.getReason());
        assertEquals(1L, response.getProductId());
        assertEquals("Teclado Mecânico", response.getProductName());
    }

    @Test
    void recordWithdrawalDecrementsStockAndSavesMovement() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.decreaseStockIfAvailable(1L, 4)).thenReturn(1);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementRequestDTO dto = new StockMovementRequestDTO();
        dto.setQuantity(4);
        dto.setReason("Venda");

        StockMovementResponseDTO response = stockService.recordWithdrawal(1L, dto, null);

        verify(productRepository).decreaseStockIfAvailable(1L, 4);
        assertEquals(MovementType.SAIDA, response.getMovementType());
        assertEquals(4, response.getQuantity());
    }

    @Test
    void recordWithdrawalThrowsWhenBalanceIsNotEnough() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.decreaseStockIfAvailable(1L, 99)).thenReturn(0);

        StockMovementRequestDTO dto = new StockMovementRequestDTO();
        dto.setQuantity(99);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.recordWithdrawal(1L, dto, null));

        assertEquals("Estoque insuficiente para o produto: Teclado Mecânico", ex.getMessage());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void recordAdjustmentSavesSignedDelta() {
        product.setStock(6);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.setStock(1L, 8)).thenReturn(1);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockAdjustmentRequestDTO dto = new StockAdjustmentRequestDTO();
        dto.setNewStock(8);
        dto.setReason("Inventário");

        StockMovementResponseDTO response = stockService.recordAdjustment(1L, dto, null);

        verify(productRepository).setStock(1L, 8);
        assertEquals(MovementType.AJUSTE, response.getMovementType());
        assertEquals(2, response.getQuantity());
    }

    @Test
    void recordAdjustmentRejectsNegativeNewStock() {
        StockAdjustmentRequestDTO dto = new StockAdjustmentRequestDTO();
        dto.setNewStock(-1);

        assertThrows(BusinessException.class, () -> stockService.recordAdjustment(1L, dto, null));
        verify(productRepository, never()).setStock(anyLong(), anyInt());
    }

    @Test
    void recordEntryRejectsNonPositiveQuantity() {
        StockMovementRequestDTO dto = new StockMovementRequestDTO();
        dto.setQuantity(0);

        assertThrows(BusinessException.class, () -> stockService.recordEntry(1L, dto, null));
        verify(productRepository, never()).increaseStock(anyLong(), anyInt());
    }

    @Test
    void recordEntryThrowsWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        StockMovementRequestDTO dto = new StockMovementRequestDTO();
        dto.setQuantity(1);

        assertThrows(ResourceNotFoundException.class, () -> stockService.recordEntry(99L, dto, null));
    }

    @Test
    void findLowStockMapsThroughProductService() {
        when(productRepository.findByStockLessThanEqualMinimumStock()).thenReturn(List.of(product));
        ProductResponseDTO mapped = new ProductResponseDTO();
        when(productService.toResponseDTO(product)).thenReturn(mapped);

        List<ProductResponseDTO> lowStock = stockService.findLowStock();

        assertEquals(1, lowStock.size());
        assertSame(mapped, lowStock.get(0));
    }

    @Test
    void getSummaryAggregatesCounts() {
        when(productRepository.count()).thenReturn(5L);
        when(productRepository.sumStock()).thenReturn(42L);
        when(productRepository.countLowStock()).thenReturn(2L);
        when(productRepository.countByStock(0)).thenReturn(1L);

        StockSummaryDTO summary = stockService.getSummary();

        assertEquals(5, summary.getTotalProducts());
        assertEquals(42, summary.getTotalUnitsInStock());
        assertEquals(2, summary.getLowStockProducts());
        assertEquals(1, summary.getOutOfStockProducts());
    }

    @Test
    void findMovementsDelegatesToRepositoryWithSpecification() {
        StockMovement movement = new StockMovement(MovementType.ENTRADA, 10, "Compra", null, product, null);
        when(stockMovementRepository.findAll(any(Specification.class))).thenReturn(List.of(movement));

        List<StockMovementResponseDTO> result =
                stockService.findMovements(1L, MovementType.ENTRADA, null, null);

        assertEquals(1, result.size());
        assertEquals(MovementType.ENTRADA, result.get(0).getMovementType());
        assertEquals(10, result.get(0).getQuantity());
    }
}
