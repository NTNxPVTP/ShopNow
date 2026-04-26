package com.example.shopnow.order.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderSummaryDTO;
import com.example.shopnow.shared.GenericMapper;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.shared.PageInfo;

@Mapper(componentModel = "spring", uses = { SubOrderMapper.class })
public interface OrderMapper extends GenericMapper<Order, OrderDTO> {
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subOrders", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    Order fromRequestToOrder(CreateOrderRequest request);

    @Mapping(target = "subOrders", source = "subOrders")
    OrderSummaryDTO toSummaryDTO(Order order);

    default PageResponse<OrderSummaryDTO> toSummaryPageResponse(Page<Order> page) {
        if (page == null) {
            return null;
        }

        // MapStruct sẽ sử dụng hàm toSummaryDTO bên dưới để map list
        List<OrderSummaryDTO> items = page.getContent().stream()
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
