package com.example.shopnow.product;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.models.Category;
import com.example.shopnow.product.rest.dto.CategoryResponse;
import com.example.shopnow.product.rest.dto.CreateCategoryRequest;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedName = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DomainException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = new Category();
        category.setName(normalizedName);

        Category savedCategory = categoryRepository.save(category);
        return new CategoryResponse(savedCategory.getId(), savedCategory.getName());
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }
}
