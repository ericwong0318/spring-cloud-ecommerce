package com.example.category;

import com.example.common.dto.CategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    CategoryDto toDto(Category category);

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    Category toEntity(CategoryDto categoryDto);

    List<CategoryDto> toDtoList(List<Category> categories);
}
