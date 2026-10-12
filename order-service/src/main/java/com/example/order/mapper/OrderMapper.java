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
    @Mapping(target = "customerEmail", source = "customerEmail")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    OrderDto toDto(Order order);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "totalAmount", source = "totalAmount")
    Order toEntity(OrderDto orderDto);

    List<OrderDto> toDtoList(List<Order> orders);

    // Custom mapping for items - to be implemented in service layer
    @Named("mapItems")
    default List<OrderItemDto> mapItems(List<OrderItem> items) {
        if (items == null) return List.of();
        return items.stream().map(OrderItemMapper.INSTANCE::toDto).toList();
    }

    @Named("mapShipments")
    default List<ShipmentDto> mapShipments(List<Shipment> shipments) {
        if (shipments == null) return List.of();
        return shipments.stream().map(ShipmentMapper.INSTANCE::toDto).toList();
    }
}
