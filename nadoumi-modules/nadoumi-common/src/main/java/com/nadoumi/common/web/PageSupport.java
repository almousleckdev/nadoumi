package com.nadoumi.common.web;

/**
 * Bounds for {@code /api/**} pagination inputs (docs/API_DESIGN.md §5). An
 * oversized {@code size} is clamped, not rejected, so a stray {@code ?size=100000}
 * cannot make PageHelper issue an unbounded {@code LIMIT} and exhaust the heap.
 *
 * <p>Usage: reassign the params at the top of a list service method, before
 * {@code PageHelper.startPage(...)} and {@code PageResponse.of(...)} —
 * {@code page = PageSupport.clampPage(page); size = PageSupport.clampSize(size);}
 */
public final class PageSupport {

    /** Largest {@code size} an endpoint will serve; a larger request is capped here. */
    public static final int MAX_SIZE = 100;

    /** Applied when {@code size} is missing or non-positive. */
    public static final int DEFAULT_SIZE = 20;

    private PageSupport() {
    }

    public static int clampPage(int page) {
        return Math.max(0, page);
    }

    public static int clampSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
