package com.example.shopnow.order.mapper;

import com.example.shopnow.order.domain.models.OrderDetail;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.domain.models.SubOrder;
import com.example.shopnow.order.rest.dto.OrderDetailDTO;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-25T13:47:14+0700",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class SubOrderMapperImpl implements SubOrderMapper {

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    public SubOrderDTO toDto(SubOrder entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        UUID shopId = null;
        OrderStatus status = null;
        BigDecimal totalPrice = null;
        List<OrderDetailDTO> orderDetails = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = entity.getId();
        shopId = entity.getShopId();
        status = entity.getStatus();
        totalPrice = entity.getTotalPrice();
        orderDetails = orderDetailSetToOrderDetailDTOList( entity.getOrderDetails() );
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        SubOrderDTO subOrderDTO = new SubOrderDTO( id, shopId, status, totalPrice, orderDetails, createdAt, updatedAt );

        return subOrderDTO;
    }

    @Override
    public List<SubOrderDTO> toDtoList(List<SubOrder> entities) {
        if ( entities == null ) {
            return null;
        }

        List<SubOrderDTO> list = new ArrayList<SubOrderDTO>( entities.size() );
        for ( SubOrder subOrder : entities ) {
            list.add( toDto( subOrder ) );
        }

        return list;
    }

    @Override
    public SubOrderSummaryDTO toSummaryDTO(SubOrder subOrder) {
        if ( subOrder == null ) {
            return null;
        }

        UUID id = null;
        UUID shopId = null;
        OrderStatus status = null;
        Integer totalPrice = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = subOrder.getId();
        shopId = subOrder.getShopId();
        status = subOrder.getStatus();
        if ( subOrder.getTotalPrice() != null ) {
            totalPrice = subOrder.getTotalPrice().intValue();
        }
        createdAt = subOrder.getCreatedAt();
        updatedAt = subOrder.getUpdatedAt();

        SubOrderSummaryDTO subOrderSummaryDTO = new SubOrderSummaryDTO( id, shopId, status, totalPrice, createdAt, updatedAt );

        return subOrderSummaryDTO;
    }

    protected List<OrderDetailDTO> orderDetailSetToOrderDetailDTOList(Set<OrderDetail> set) {
        if ( set == null ) {
            return null;
        }

        List<OrderDetailDTO> list = new ArrayList<OrderDetailDTO>( set.size() );
        for ( OrderDetail orderDetail : set ) {
            list.add( orderDetailMapper.toDto( orderDetail ) );
        }

        return list;
    }
}
