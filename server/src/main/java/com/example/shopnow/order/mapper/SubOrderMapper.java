package com.example.shopnow.order.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import com.example.shopnow.order.models.SubOrder;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import com.example.shopnow.shared.GenericMapper;
import com.example.shopnow.shared.PageInfo;
import com.example.shopnow.shared.PageResponse;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface SubOrderMapper extends GenericMapper<SubOrder, SubOrderDTO> {
    SubOrderSummaryDTO toSummaryDTO(SubOrder subOrder);

    default PageResponse<SubOrderSummaryDTO> toSummaryPageResponse(Page<SubOrder> page) {
        if (page == null) {
            return null;
        }

        List<SubOrderSummaryDTO> items = page.getContent().stream()
                .map(this::toSummaryDTO)
                .toList();

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
