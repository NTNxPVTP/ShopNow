package com.example.shopnow.shared;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        PageInfo pageInfo) {
}
