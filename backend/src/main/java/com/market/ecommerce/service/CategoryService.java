package com.market.ecommerce.service;

import com.market.ecommerce.dto.CategoryRequest;
import com.market.ecommerce.dto.CategoryResponse;
import com.market.ecommerce.entity.Category;
import com.market.ecommerce.exception.BadRequestException;
import com.market.ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    private CategoryResponse toDto(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}