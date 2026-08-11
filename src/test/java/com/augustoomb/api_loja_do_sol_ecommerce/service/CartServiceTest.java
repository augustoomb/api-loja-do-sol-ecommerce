package com.augustoomb.api_loja_do_sol_ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddToCartRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CartResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.UpdateCartItemRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Cart;
import com.augustoomb.api_loja_do_sol_ecommerce.model.CartItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Category;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartItemRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User("Cliente", "cliente@test.com", "x");
        user.setId(1L);
        cart = new Cart(user);
        cart.setId(10L);
        Category category = new Category("Eletrônicos", null);
        category.setId(1L);
        product = new Product("Mouse Gamer", "desc", new BigDecimal("50.00"), 10, null, category);
        product.setId(5L);
        product.setSku("MOUSE-GAMER");
    }

    @Test
    void getCartCreatesWhenMissing() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartIdOrderById(any())).thenReturn(List.of());

        CartResponseDTO dto = cartService.getCartResponse(user);

        verify(cartRepository).save(any(Cart.class));
        assertEquals(0, dto.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, dto.getTotal());
    }

    @Test
    void addItemAddsNewItemAndComputesTotals() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 5L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartIdOrderById(10L))
                .thenReturn(List.of(new CartItem(cart, product, 2)));

        AddToCartRequestDTO dto = new AddToCartRequestDTO();
        dto.setProductId(5L);
        dto.setQuantity(2);

        CartResponseDTO response = cartService.addItem(user, dto);

        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getTotalQuantity());
        assertEquals(new BigDecimal("100.00"), response.getTotal());
    }

    @Test
    void addItemIncrementsExistingItemQuantity() {
        CartItem existing = new CartItem(cart, product, 3);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 5L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(existing);
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of(existing));

        AddToCartRequestDTO dto = new AddToCartRequestDTO();
        dto.setProductId(5L);
        dto.setQuantity(4);

        cartService.addItem(user, dto);

        assertEquals(7, existing.getQuantity());
    }

    @Test
    void addItemRejectsQuantityAboveStock() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        AddToCartRequestDTO dto = new AddToCartRequestDTO();
        dto.setProductId(5L);
        dto.setQuantity(99);

        assertThrows(BusinessException.class, () -> cartService.addItem(user, dto));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItemRejectsNonPositiveQuantity() {
        AddToCartRequestDTO dto = new AddToCartRequestDTO();
        dto.setProductId(5L);
        dto.setQuantity(0);

        assertThrows(BusinessException.class, () -> cartService.addItem(user, dto));
        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    void updateItemChangesQuantity() {
        CartItem existing = new CartItem(cart, product, 3);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 5L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(existing);
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of(existing));

        UpdateCartItemRequestDTO dto = new UpdateCartItemRequestDTO();
        dto.setQuantity(6);

        cartService.updateItem(user, 5L, dto);

        assertEquals(6, existing.getQuantity());
    }

    @Test
    void removeItemDeletesFromCart() {
        CartItem existing = new CartItem(cart, product, 3);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 5L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of());

        cartService.removeItem(user, 5L);

        verify(cartItemRepository).delete(existing);
    }

    @Test
    void clearDeletesAllItems() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderById(10L)).thenReturn(List.of());

        cartService.clear(user);

        verify(cartItemRepository).deleteByCartId(10L);
    }
}
