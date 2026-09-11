package com.example.order.mapper;

import com.example.common.dto.ShipmentItemDto;
import com.example.order.model.ShipmentItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShipmentItemMapper {

    ShipmentItemMapper INSTANCE = Mappers.getMapper(ShipmentItemMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderItemId", source = "orderItemId")
    @Mapping(target = "quantity", source = "quantity")
    ShipmentItemDto toDto(ShipmentItem shipmentItem);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "quantity", source = "quantity")
    ShipmentItem toEntity(ShipmentItemDto shipmentItemDto);

    List<ShipmentItemDto> toDtoList(List<ShipmentItem> shipmentItems);
}
