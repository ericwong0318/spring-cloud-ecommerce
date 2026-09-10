package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Category data transfer object")
public record CategoryDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    @Schema(description = "Category name", example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Category description", example = "Electronic devices and accessories")
    String description,

    @Schema(description = "Parent category ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long parentId,

    @Schema(description = "Child categories", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("children")
    List<CategoryDto> children
) {}