package com.example.shopnow.product.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.shopnow.product.CategoryService;
import com.example.shopnow.product.rest.dto.CategoryResponse;
import com.example.shopnow.product.rest.dto.CreateCategoryRequest;
import com.example.shopnow.user.models.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("hasRole('ADMIN')") // Only allow users with ADMIN role to create categories
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CreateCategoryRequest request, @AuthenticationPrincipal User user
    ) {
        System.out.println("Authenticated user: " + user.getUsername() + ", Role: " + user.getRole());
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }
}
