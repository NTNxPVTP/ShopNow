package com.example.shopnow.order;

import org.mapstruct.Mapper;
import com.example.shopnow.order.models.OrderDetail;
import com.example.shopnow.order.rest.dto.OrderDetailDTO;
import com.example.shopnow.shared.GenericMapper;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper extends GenericMapper<OrderDetail, OrderDetailDTO> {

}
