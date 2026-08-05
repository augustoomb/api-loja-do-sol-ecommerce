package com.augustoomb.api_loja_do_sol_ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByEnabledTrue();

    List<Product> findByNameContainingIgnoreCase(String name);

    Optional<Product> findBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.stock <= p.minimumStock")
    List<Product> findByStockLessThanEqualMinimumStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock <= p.minimumStock")
    long countLowStock();

    // COALESCE: serve para retornar o primeiro valor não nulo de uma lista de argumentos/expressões.
    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p")
    long sumStock();

    long countByStock(int stock);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity, p.version = p.version + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity, p.version = p.version + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.stock >= :quantity")
    int decreaseStockIfAvailable(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = :newStock, p.version = p.version + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    int setStock(@Param("id") Long id, @Param("newStock") int newStock);
}

// Consultas anotadas com @Query e @Modifying são executadas diretamente no banco de dados,
// ignorando a memória/cache do JPA