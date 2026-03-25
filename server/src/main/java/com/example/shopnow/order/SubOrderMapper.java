package com.example.shopnow.order;

import org.mapstruct.Mapper;

import com.example.shopnow.order.models.SubOrder;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.shared.GenericMapper;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface SubOrderMapper extends GenericMapper<SubOrder, SubOrderDTO> {
    
}
