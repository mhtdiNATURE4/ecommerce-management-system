package com.market.ecommerce.controller;

import com.market.ecommerce.dto.CategoryRequest;
import com.market.ecommerce.dto.CategoryResponse;
import com.market.ecommerce.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // تم إضافة حماية الصلاحيات لضمان أن الإدارة فقط من تستطيع إضافة قسم
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse res = categoryService.createCategory(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(res.id())
                .toUri();
        return ResponseEntity.created(location).body(res);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/paged")
    public ResponseEntity<org.springframework.data.domain.Page<CategoryResponse>> getAllCategoriesPaged(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(categoryService.getAllCategoriesPaged(pageable));
    }
}