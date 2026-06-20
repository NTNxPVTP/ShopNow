package com.example.shopnow.product.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name must not be blank") String name) {
}
