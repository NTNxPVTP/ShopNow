package com.example.shopnow.order.mapper;

import com.example.shopnow.order.domain.models.OrderDetail;
import com.example.shopnow.order.rest.dto.OrderDetailDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-25T13:47:13+0700",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class OrderDetailMapperImpl implements OrderDetailMapper {

    @Override
    public OrderDetailDTO toDto(OrderDetail entity) {
        if ( entity == null ) {
            return null;
        }

        String productName = null;
        BigDecimal price = null;
        Integer quantity = null;

        productName = entity.getProductName();
        price = entity.getPrice();
        quantity = entity.getQuantity();

        OrderDetailDTO orderDetailDTO = new OrderDetailDTO( productName, price, quantity );

        return orderDetailDTO;
    }

    @Override
    public List<OrderDetailDTO> toDtoList(List<OrderDetail> entities) {
        if ( entities == null ) {
            return null;
        }

        List<OrderDetailDTO> list = new ArrayList<OrderDetailDTO>( entities.size() );
        for ( OrderDetail orderDetail : entities ) {
            list.add( toDto( orderDetail ) );
        }

        return list;
    }
}
