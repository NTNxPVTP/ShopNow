package com.example.shopnow.shared;

import java.util.List;

import lombok.Builder;

public record PageResponse<T>(
                List<T> items,
                PageInfo pageInfo) {
}

@Builder
record PageInfo(
                int pageNumber,
                int pageSize,
                int totalPages,
                boolean isLast,
                long totalElements) {
}
