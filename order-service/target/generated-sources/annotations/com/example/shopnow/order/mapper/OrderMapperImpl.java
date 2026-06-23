package com.example.shopnow.order.mapper;

import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.domain.models.SubOrder;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderSummaryDTO;
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
    date = "2026-06-23T17:51:35+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Autowired
    private SubOrderMapper subOrderMapper;

    @Override
    public OrderDTO toDto(Order entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        OrderStatus status = null;
        BigDecimal totalPrice = null;
        String addressShipping = null;
        String phoneNumber = null;
        String customerName = null;
        List<SubOrderDTO> subOrders = null;
        LocalDateTime createdAt = null;

        id = entity.getId();
        status = entity.getStatus();
        totalPrice = entity.getTotalPrice();
        addressShipping = entity.getAddressShipping();
        phoneNumber = entity.getPhoneNumber();
        customerName = entity.getCustomerName();
        subOrders = subOrderSetToSubOrderDTOList( entity.getSubOrders() );
        createdAt = entity.getCreatedAt();

        OrderDTO orderDTO = new OrderDTO( id, status, totalPrice, addressShipping, phoneNumber, customerName, subOrders, createdAt );

        return orderDTO;
    }

    @Override
    public List<OrderDTO> toDtoList(List<Order> entities) {
        if ( entities == null ) {
            return null;
        }

        List<OrderDTO> list = new ArrayList<OrderDTO>( entities.size() );
        for ( Order order : entities ) {
            list.add( toDto( order ) );
        }

        return list;
    }

    @Override
    public Order fromRequestToOrder(CreateOrderRequest request) {
        if ( request == null ) {
            return null;
        }

        Order.OrderBuilder order = Order.builder();

        order.addressShipping( request.addressShipping() );
        order.phoneNumber( request.phoneNumber() );
        order.customerName( request.customerName() );

        return order.build();
    }

    @Override
    public OrderSummaryDTO toSummaryDTO(Order order) {
        if ( order == null ) {
            return null;
        }

        List<SubOrderSummaryDTO> subOrders = null;
        UUID id = null;
        OrderStatus status = null;
        BigDecimal totalPrice = null;
        String addressShipping = null;
        String phoneNumber = null;
        String customerName = null;
        LocalDateTime createdAt = null;

        subOrders = subOrderSetToSubOrderSummaryDTOList( order.getSubOrders() );
        id = order.getId();
        status = order.getStatus();
        totalPrice = order.getTotalPrice();
        addressShipping = order.getAddressShipping();
        phoneNumber = order.getPhoneNumber();
        customerName = order.getCustomerName();
        createdAt = order.getCreatedAt();

        OrderSummaryDTO orderSummaryDTO = new OrderSummaryDTO( id, status, totalPrice, addressShipping, phoneNumber, customerName, subOrders, createdAt );

        return orderSummaryDTO;
    }

    protected List<SubOrderDTO> subOrderSetToSubOrderDTOList(Set<SubOrder> set) {
        if ( set == null ) {
            return null;
        }

        List<SubOrderDTO> list = new ArrayList<SubOrderDTO>( set.size() );
        for ( SubOrder subOrder : set ) {
            list.add( subOrderMapper.toDto( subOrder ) );
        }

        return list;
    }

    protected List<SubOrderSummaryDTO> subOrderSetToSubOrderSummaryDTOList(Set<SubOrder> set) {
        if ( set == null ) {
            return null;
        }

        List<SubOrderSummaryDTO> list = new ArrayList<SubOrderSummaryDTO>( set.size() );
        for ( SubOrder subOrder : set ) {
            list.add( subOrderMapper.toSummaryDTO( subOrder ) );
        }

        return list;
    }
}
