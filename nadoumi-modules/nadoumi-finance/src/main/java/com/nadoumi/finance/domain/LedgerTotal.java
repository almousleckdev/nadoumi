package com.nadoumi.finance.domain;

import java.math.BigDecimal;

/** What the ledger rows matching a filter add up to in one currency: money in, money out and the difference. */
public record LedgerTotal(String currency, BigDecimal revenue, BigDecimal expenses, BigDecimal net, long entries) {
}
