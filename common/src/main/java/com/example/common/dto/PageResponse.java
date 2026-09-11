package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "List of items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> content;

    @Schema(description = "Current page number (0-based)", example = "0")
    private int pageNumber;

    @Schema(description = "Page size", example = "20")
    private int pageSize;

    @Schema(description = "Total number of elements", example = "100")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;

    @Schema(description = "Whether there is a next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there is a previous page", example = "false")
    private boolean hasPrevious;

    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
