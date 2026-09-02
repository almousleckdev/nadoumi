package com.nadoumi.common.web;

import java.util.List;

/**
 * Spring {@code Page}-shaped list body for {@code /api/**} endpoints
 * (docs/API_DESIGN.md §5). Zero-based {@code page}.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
