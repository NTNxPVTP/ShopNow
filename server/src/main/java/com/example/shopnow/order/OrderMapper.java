package com.example.shopnow.order;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDetail;
import com.example.shopnow.order.rest.dto.OrderDetailResponse;
import com.example.shopnow.shared.GenericMapper;

@Mapper(componentModel = "spring")
public interface OrderMapper extends GenericMapper<Order, OrderDetail>{
    
    @Mapping(target = "status" ,constant = "IN_PROCESS")
    @Mapping(target = "totalPrice" ,ignore=true)
    @Mapping(target = "addressShipping" ,ignore=true)
    @Mapping(target = "phoneNumber" ,ignore=true)
    @Mapping(target = "customerName",ignore=true)
    Order fromCreateOrderRequestToOrder(CreateOrderRequest request);
    
    @Mapping(target = "status" ,constant = "IN_PROCESS")
    @Mapping(target = "totalPrice" ,ignore=true)
    @Mapping(target = "addressShipping" ,ignore=true)
    @Mapping(target = "phoneNumber" ,ignore=true)
    @Mapping(target = "customerName",ignore=true)
    OrderDetailResponse fromOrderToResponse(Order order);
}
