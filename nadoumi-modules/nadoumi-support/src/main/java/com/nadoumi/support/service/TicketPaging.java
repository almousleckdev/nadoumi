package com.nadoumi.support.service;

/** Clamps caller-supplied paging so an oversized {@code size} can never trigger an unbounded query. */
final class TicketPaging {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;

    private TicketPaging() {
    }

    static int limit(int size) {
        return size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    static int offset(int page, int size) {
        return Math.max(page, 0) * limit(size);
    }
}
