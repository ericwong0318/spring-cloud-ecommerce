package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category data transfer object")
public class CategoryDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    @Schema(description = "Category name", example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Category description", example = "Electronic devices and accessories")
    private String description;

    @Schema(description = "Parent category ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long parentId;

    @Schema(description = "Child categories", accessMode = Schema.AccessMode.READ_ONLY)
    private List<CategoryDto> children;
}