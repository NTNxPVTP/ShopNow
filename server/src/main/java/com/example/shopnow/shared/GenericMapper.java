package com.example.shopnow.shared;

import java.util.List;

import org.springframework.data.domain.Page;

public interface GenericMapper<E, D> {
    D toDto(E entity);

    List<D> toDtoList(List<E> entities);

    default PageResponse<D> toPageResponse(Page<E> page) {
        List<D> items = toDtoList(page.getContent());

        PageInfo pageInfo = PageInfo.builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .totalElements(page.getTotalElements())
                .build();

        return new PageResponse<>(items, pageInfo);
    }
}
