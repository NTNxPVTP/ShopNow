package com.example.shopnow.product.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name must not be blank") String name) {
}
