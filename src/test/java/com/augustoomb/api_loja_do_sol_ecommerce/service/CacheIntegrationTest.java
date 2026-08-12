package com.augustoomb.api_loja_do_sol_ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.ProductResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.StockMovementRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CategoryRepository;

/**
 * Testa o comportamento do cache Redis do catálogo.
 *
 * Requisito: o Redis precisa estar rodando (ex.: `docker compose up -d redis`).
 * O @Transactional garante o rollback no final: nada é gravado de verdade no banco.
 */
@SpringBootTest
@Transactional
class CacheIntegrationTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private StockService stockService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CacheManager cacheManager;

    private Cache productsCache() {
        return cacheManager.getCache("products");
    }

    @Test
    void productReadIsCachedAndEvictedOnUpdate() {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new Category("Categoria Cache", "criada em teste")));

        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Produto Cache");
        dto.setSku("SKU-CACHE-" + System.nanoTime());
        dto.setPrice(new BigDecimal("10.00"));
        dto.setStock(5);
        dto.setMinimumStock(1);
        dto.setEnabled(true);
        dto.setCategoryId(category.getId());

        ProductResponseDTO created = productService.create(dto); // create invalida o cache
        assertNull(productsCache().get(created.getId()));        // nada em cache para este id

        productService.findById(created.getId());                // primeira leitura -> popula o cache
        awaitCacheContains(created.getId());                     // o valor passa a existir no Redis

        productService.findAll();                                // listagem também é cacheada (chave "all")
        awaitCacheContains("all");

        dto.setPrice(new BigDecimal("99.99"));
        productService.update(created.getId(), dto);             // update invalida o cache (inclusive a listagem)
        awaitCacheEmpty(created.getId());
        awaitCacheEmpty("all");

        assertEquals(new BigDecimal("99.99"),
                productService.findById(created.getId()).getPrice()); // leitura fresca do banco
    }

    @Test
    void stockChangeEvictsCachedProduct() {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new Category("Categoria Estoque Cache", "criada em teste")));

        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Produto Estoque Cache");
        dto.setSku("SKU-ESTOQUE-" + System.nanoTime());
        dto.setPrice(new BigDecimal("20.00"));
        dto.setStock(0);
        dto.setMinimumStock(0);
        dto.setEnabled(true);
        dto.setCategoryId(category.getId());

        ProductResponseDTO created = productService.create(dto);

        productService.findById(created.getId()); // popula o cache com stock = 0
        awaitCacheContains(created.getId());

        StockMovementRequestDTO entry = new StockMovementRequestDTO();
        entry.setQuantity(7);
        entry.setReason("Teste de invalidação por estoque");
        stockService.recordEntry(created.getId(), entry, null); // entrada de estoque invalida o cache

        awaitCacheEmpty(created.getId());                              // cache esvaziado
        assertEquals(7, productService.findById(created.getId()).getStock()); // leitura fresca
    }

    /**
     * Nesta versão do stack (Spring Data Redis 4.x + Lettuce), os comandos de ESCRITA
     * do cache chegam ao Redis de forma NÃO-BLOQUEANTE: o put/evict pode "aterrissar"
     * alguns milissegundos depois da chamada retornar.
     *
     * Na API real isso não causa problema — a próxima requisição HTTP chega muito
     * depois do comando ter aterrissado. Mas no teste, que lê no MESMO thread e
     * imediatamente após escrever, é preciso aguardar a condição (polling) para
     * eliminar a corrida e tornar a asserção determinística.
     */
    private void awaitCacheContains(Object key) {
        await(() -> productsCache().get(key) != null,
                "o produto " + key + " deveria estar no cache do Redis");
    }

    private void awaitCacheEmpty(Object key) {
        await(() -> productsCache().get(key) == null,
                "o produto " + key + " deveria ter sido removido do cache do Redis");
    }

    private void await(Supplier<Boolean> condition, String message) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrompido durante a espera do cache", e);
            }
        }
        fail(message);
    }
}
