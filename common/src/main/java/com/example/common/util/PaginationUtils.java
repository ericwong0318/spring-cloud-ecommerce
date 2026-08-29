package com.example.common.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    private PaginationUtils() {
        // Utility class
    }

    /**
     * Create a Pageable with default sorting by creation date descending
     */
    public static Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Create a Pageable with custom sort
     */
    public static Pageable createPageable(int page, int size, String sortBy, Sort.Direction direction) {
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    /**
     * Create a Pageable with multiple sort criteria
     */
    public static Pageable createPageable(int page, int size, Sort.Order... orders) {
        return PageRequest.of(page, size, Sort.by(orders));
    }

    /**
     * Check if page has content
     */
    public static <T> boolean hasContent(Page<T> page) {
        return page != null && page.hasContent();
    }

    /**
     * Get safe page number (non-negative)
     */
    public static int safePageNumber(int page) {
        return Math.max(0, page);
    }

    /**
     * Get safe page size (between 1 and 100)
     */
    public static int safePageSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }
}