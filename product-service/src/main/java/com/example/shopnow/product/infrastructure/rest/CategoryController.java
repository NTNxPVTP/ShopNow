package com.example.shopnow.product.infrastructure.rest;

import com.example.shopnow.product.application.services.CategoryService;
import org.springframework.http.ResponseEntity;
import com.example.shopnow.user.api.AuthUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.shopnow.product.application.dto.CategoryResponse;
import com.example.shopnow.product.application.dto.CreateCategoryRequest;
import com.example.shopnow.user.api.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CreateCategoryRequest request, @AuthUser AuthenticatedUser user) {
        System.out.println("Authenticated user: " + user.getEmail() + ", Role: " + user.getRole());
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }
}
