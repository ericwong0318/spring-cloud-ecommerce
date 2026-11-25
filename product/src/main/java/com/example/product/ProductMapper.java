package com.example.product;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    ProductDto toDto(Product product);

    Product toEntity(ProductDto productDto);

    default ProductVariantDto toVariantDto(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return new ProductVariantDto(
                variant.getId(),
                variant.getProductId(),
                variant.getSkuCode(),
                variant.getAttributes(),
                variant.getPrice(),
                variant.getInventoryId()
        );
    }

    default ProductVariant toVariantEntity(ProductVariantDto dto) {
        if (dto == null) {
            return null;
        }
        return new ProductVariant(dto.skuCode(), dto.attributes(), dto.price(), dto.inventoryId());
    }
}
