package com.example.shopnow.order;

import org.mapstruct.Mapper;
import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.OrderDetail;
import com.example.shopnow.shared.GenericMapper;

@Mapper(componentModel = "spring")
public interface OrderMapper extends GenericMapper<Order, OrderDetail>{
    
}
