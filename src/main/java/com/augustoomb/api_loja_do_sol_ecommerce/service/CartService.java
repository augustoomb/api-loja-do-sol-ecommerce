package com.augustoomb.api_loja_do_sol_ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.augustoomb.api_loja_do_sol_ecommerce.dto.AddToCartRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CartItemResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.CartResponseDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.dto.UpdateCartItemRequestDTO;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.BusinessException;
import com.augustoomb.api_loja_do_sol_ecommerce.exception.ResourceNotFoundException;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Cart;
import com.augustoomb.api_loja_do_sol_ecommerce.model.CartItem;
import com.augustoomb.api_loja_do_sol_ecommerce.model.Product;
import com.augustoomb.api_loja_do_sol_ecommerce.model.User;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartItemRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.CartRepository;
import com.augustoomb.api_loja_do_sol_ecommerce.repository.ProductRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    // Transação de escrita: getCart cria o carrinho (INSERT) quando o usuário ainda não possui um.
    @Transactional
    public CartResponseDTO getCartResponse(User user) {
        return toResponseDTO(getCart(user));
    }

    @Transactional
    public CartResponseDTO addItem(User user, AddToCartRequestDTO dto) {
        int quantity = validateQuantity(dto.getQuantity());
        if (dto.getProductId() == null) {
            throw new BusinessException("O produto é obrigatório");
        }
        Product product = loadAvailableProduct(dto.getProductId());
        ensureStock(product, quantity);

        Cart cart = getCart(user);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> new CartItem(cart, product, 0));
        int newQuantity = item.getQuantity() + quantity;
        ensureStock(product, newQuantity);
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        return toResponseDTO(cart);
    }

    @Transactional
    public CartResponseDTO updateItem(User user, Long productId, UpdateCartItemRequestDTO dto) {
        int quantity = validateQuantity(dto.getQuantity());
        Cart cart = getCart(user);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado no carrinho para o produto: " + productId));
        ensureStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return toResponseDTO(cart);
    }

    @Transactional
    public CartResponseDTO removeItem(User user, Long productId) {
        Cart cart = getCart(user);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado no carrinho para o produto: " + productId));
        cartItemRepository.delete(item);
        return toResponseDTO(cart);
    }

    @Transactional
    public CartResponseDTO clear(User user) {
        Cart cart = getCart(user);
        cartItemRepository.deleteByCartId(cart.getId());
        return toResponseDTO(cart);
    }

    @Transactional
    public Cart getCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private CartResponseDTO toResponseDTO(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderById(cart.getId());
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());
        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            CartItemResponseDTO itemDTO = new CartItemResponseDTO();
            itemDTO.setProductId(item.getProduct().getId());
            itemDTO.setProductName(item.getProduct().getName());
            itemDTO.setProductSku(item.getProduct().getSku());
            itemDTO.setUnitPrice(item.getProduct().getPrice());
            itemDTO.setQuantity(item.getQuantity());
            BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemDTO.setSubtotal(subtotal);
            itemDTOs.add(itemDTO);
            totalQuantity += item.getQuantity();
            total = total.add(subtotal);
        }
        dto.setItems(itemDTOs);
        dto.setTotalQuantity(totalQuantity);
        dto.setTotal(total);
        return dto;
    }

    private Product loadAvailableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + productId));
        if (!product.isEnabled()) {
            throw new BusinessException("Produto indisponível: " + product.getName());
        }
        return product;
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new BusinessException("Estoque insuficiente para o produto: " + product.getName()
                    + " (disponível: " + product.getStock() + ")");
        }
    }

    private int validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("A quantidade deve ser maior que zero");
        }
        return quantity;
    }
}
