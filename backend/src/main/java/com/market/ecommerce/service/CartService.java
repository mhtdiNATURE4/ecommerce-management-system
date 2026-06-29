package com.market.ecommerce.service;

import com.market.ecommerce.dto.AddToCartRequest;
import com.market.ecommerce.dto.CartItemResponse;
import com.market.ecommerce.entity.CartItem;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.exception.BadRequestException;
import com.market.ecommerce.exception.ResourceNotFoundException;
import com.market.ecommerce.repository.CartItemRepository;
import com.market.ecommerce.repository.ProductRepository;
import com.market.ecommerce.repository.UserRepository;
import com.market.ecommerce.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository,
                       UserRepository userRepository,
                       ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartItem addToCart(AddToCartRequest request) {

        String email = SecurityUtils.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("المنتج غير موجود"));

        CartItem cartItem =
                cartItemRepository.findByUserAndProduct(user, product).orElse(null);

        int newTotalQuantity = request.quantity();

        if (cartItem != null) {
            newTotalQuantity += cartItem.getQuantity();
        }

        if (product.getStock() < newTotalQuantity) {
            throw new BadRequestException(
                    "المخزون غير كافٍ. المتاح: " + product.getStock()
            );
        }

        if (cartItem != null) {
            cartItem.setQuantity(newTotalQuantity);
        } else {
            cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.quantity())
                    .build();
        }

        return cartItemRepository.save(cartItem);
    }

    @Transactional
    public CartItem updateQuantity(Long id, Integer quantity) {

        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("عنصر السلة غير موجود"));
        
        // ownership check: ensure current user owns this cart item
        String email = SecurityUtils.getCurrentUserEmail();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));
        if (item.getUser() == null || !item.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("غير مصرح بتعديل عنصر السلة");
        }
        if (quantity < 1) {
            throw new BadRequestException("الكمية يجب أن تكون أكبر من صفر");
        }

        if (item.getProduct().getStock() < quantity) {
            throw new BadRequestException(
                    "المخزون غير كافٍ. المتاح: " + item.getProduct().getStock()
            );
        }

        item.setQuantity(quantity);

        return cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long id) {

        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("عنصر السلة غير موجود"));

        // ownership check: ensure current user owns this cart item
        String email = SecurityUtils.getCurrentUserEmail();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));
        if (item.getUser() == null || !item.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("غير مصرح بحذف عنصر السلة");
        }

        cartItemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getUserCart() {

        String email = SecurityUtils.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));

        return cartItemRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getUserCartDto() {
        return getUserCart().stream().map(this::toDto).collect(Collectors.toList());
    }

    /* DTO mapping helpers */
    public CartItemResponse toDto(CartItem it) {
        BigDecimal unitPrice = it.getProduct().getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(it.getQuantity()));
        return new CartItemResponse(
                it.getId(),
                it.getProduct().getId(),
                it.getProduct().getName(),
                it.getQuantity(),
                unitPrice,
                totalPrice
        );
    }
}
