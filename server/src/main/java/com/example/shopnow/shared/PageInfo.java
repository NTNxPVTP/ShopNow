package com.example.shopnow.shared;

import lombok.Builder;

@Builder
public record PageInfo(
                int pageNumber,
                int pageSize,
                int totalPages,
                boolean isLast,
                long totalElements) {
}