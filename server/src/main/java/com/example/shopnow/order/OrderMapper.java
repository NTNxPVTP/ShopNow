package com.example.shopnow.order;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.shared.GenericMapper;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface OrderMapper extends GenericMapper<Order, OrderDTO>{
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "orderDetails", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    Order fromRequestToOrder(CreateOrderRequest request);

}
