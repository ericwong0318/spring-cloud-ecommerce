package com.example.product;

import com.example.common.dto.ProductVariantDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    ProductVariantMapper INSTANCE = Mappers.getMapper(ProductVariantMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    ProductVariantDto toDto(ProductVariant variant);

    @Mapping(target = "skuCode", source = "skuCode")
    @Mapping(target = "attributes", source = "attributes")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "inventoryId", source = "inventoryId")
    ProductVariant toEntity(ProductVariantDto dto);

    List<ProductVariantDto> toDtoList(List<ProductVariant> variants);
}
