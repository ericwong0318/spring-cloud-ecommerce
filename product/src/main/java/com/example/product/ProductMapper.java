package com.example.product;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "variants", source = "variants")
    ProductDto toDto(Product product);

    @Mapping(target = "variants", ignore = true)
    Product toEntity(ProductDto productDto);

    default List<ProductVariantDto> mapVariants(List<ProductVariant> variants) {
        if (variants == null) {
            return null;
        }
        return variants.stream()
                .map(this::toVariantDto)
                .collect(Collectors.toList());
    }

    default ProductVariantDto toVariantDto(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        ProductVariantDto dto = new ProductVariantDto();
        dto.setSkuCode(variant.getSkuCode());
        dto.setAttributes(variant.getAttributes());
        dto.setPrice(variant.getPrice());
        dto.setInventoryId(variant.getInventoryId());
        return dto;
    }

    default ProductVariant toVariantEntity(ProductVariantDto dto) {
        if (dto == null) {
            return null;
        }
        return new ProductVariant(dto.getSkuCode(), dto.getAttributes(), dto.getPrice(), dto.getInventoryId());
    }
}
