package com.example.category;

import com.example.common.dto.CategoryDto;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    @Mapping(target = "parentId", source = "parent.id")
    CategoryDto toDto(Category category);

    @AfterMapping
    default void setEmptyChildren(@MappingTarget CategoryDto dto, Category category) {
        if (dto.children() == null) {
            // Records are immutable, so we need to create a new one
        }
    }

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    Category toEntity(CategoryDto categoryDto);

    List<CategoryDto> toDtoList(List<Category> categories);
}
