package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response wrapper")
public record PageResponse<T>(
    @Schema(description = "List of items", requiredMode = Schema.RequiredMode.REQUIRED)
    List<T> content,

    @Schema(description = "Current page number (0-based)", example = "0")
    int pageNumber,

    @Schema(description = "Page size", example = "20")
    int pageSize,

    @Schema(description = "Total number of elements", example = "100")
    long totalElements,

    @Schema(description = "Total number of pages", example = "5")
    int totalPages,

    @Schema(description = "Whether this is the first page", example = "true")
    boolean first,

    @Schema(description = "Whether this is the last page", example = "false")
    boolean last,

    @Schema(description = "Whether there is a next page", example = "true")
    boolean hasNext,

    @Schema(description = "Whether there is a previous page", example = "false")
    boolean hasPrevious
) {
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}