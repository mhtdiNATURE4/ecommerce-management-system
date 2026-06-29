package com.market.ecommerce.service;

import com.market.ecommerce.dto.ProductRequest;
import com.market.ecommerce.dto.ProductResponse;
import com.market.ecommerce.entity.Category;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.exception.ResourceNotFoundException;
import com.market.ecommerce.repository.CategoryRepository;
import com.market.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product createProduct(ProductRequest request) {

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "القسم غير موجود برقم معرف: "
                                        + request.categoryId()
                        )
                );

        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .imageUrl(request.imageUrl())
                .category(category)
                .build();

        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAllWithCategory();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProductResponse> getAllProductsPaged(org.springframework.data.domain.Pageable pageable) {
        return productRepository.findAllWithCategory(pageable).map(this::toDto);
    }

    public Product getProductById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "المنتج غير موجود برقم معرف: " + id
                        )
                );
    }

    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "لا يمكن الحذف، المنتج غير موجود برقم معرف: " + id
            );
        }

        productRepository.deleteById(id);
    }

    /* DTO mapping helpers */
    public ProductResponse toDto(Product p) {
        Long categoryId = null;
        String categoryName = null;
        if (p.getCategory() != null) {
            categoryId = p.getCategory().getId();
            categoryName = p.getCategory().getName();
        }

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getImageUrl(),
                categoryName,
                categoryId
        );
    }

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsDto() {
        return getAllProducts().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProductsDto(int threshold) {
        return productRepository.findByStockLessThanEqualOrderByStockAsc(threshold).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProductsDto() {
        return getLowStockProductsDto(DEFAULT_LOW_STOCK_THRESHOLD);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductByIdDto(Long id) {
        Product p = getProductById(id);
        return toDto(p);
    }
}
