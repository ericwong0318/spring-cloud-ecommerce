package com.example.product;

import com.example.common.dto.ProductVariantDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    ProductVariantMapper INSTANCE = Mappers.getMapper(ProductVariantMapper.class);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "inventoryId", source = "inventoryId")
    ProductVariantDto toDto(ProductVariant variant);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "id", ignore = true)
    ProductVariant toEntity(ProductVariantDto dto);

    List<ProductVariantDto> toDtoList(List<ProductVariant> variants);
}