package com.example.order.mapper;

import com.example.common.dto.OrderItemDto;
import com.example.order.model.OrderItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    OrderItemMapper INSTANCE = Mappers.getMapper(OrderItemMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "variantId", source = "variantId")
    @Mapping(target = "skuCode", source = "skuCode")
    @Mapping(target = "productName", source = "productName")
    @Mapping(target = "quantity", source = "quantityOrdered")
    @Mapping(target = "quantityShipped", source = "quantityShipped")
    @Mapping(target = "price", source = "unitPrice")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatusToDto")
    OrderItemDto toDto(OrderItem orderItem);

@BeanMapping(ignoreByDefault = true)
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "variantId", source = "variantId")
    @Mapping(target = "skuCode", source = "skuCode")
    @Mapping(target = "productName", source = "productName")
    @Mapping(target = "quantityOrdered", source = "quantity")
    @Mapping(target = "quantityShipped", source = "quantityShipped")
    @Mapping(target = "unitPrice", source = "price")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatusToEntity")
    OrderItem toEntity(OrderItemDto orderItemDto);

    List<OrderItemDto> toDtoList(List<OrderItem> orderItems);

    @Named("mapStatusToDto")
    default OrderItemDto.OrderItemStatus mapStatusToDto(OrderItem.OrderItemStatus status) {
        return OrderItemDto.OrderItemStatus.valueOf(status.name());
    }

    @Named("mapStatusToEntity")
    default OrderItem.OrderItemStatus mapStatusToEntity(OrderItemDto.OrderItemStatus status) {
        if (status == OrderItemDto.OrderItemStatus.PARTIALLY_CONFIRMED) {
            return OrderItem.OrderItemStatus.RESERVED;
        }
        return OrderItem.OrderItemStatus.valueOf(status.name());
    }
}