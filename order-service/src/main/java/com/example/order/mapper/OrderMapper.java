package com.example.order.mapper;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.dto.ShipmentDto;
import com.example.common.dto.ShipmentItemDto;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.Shipment;
import com.example.order.model.ShipmentItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class, ShipmentMapper.class})
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "shipments", source = "shipments")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    OrderDto toDto(Order order);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "totalAmount", source = "totalAmount")
    Order toEntity(OrderDto orderDto);

    List<OrderDto> toDtoList(List<Order> orders);
}
