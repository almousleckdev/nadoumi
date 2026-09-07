package com.nadoumi.finance.domain;

import java.math.BigDecimal;

/** A per-currency aggregate row used by the finance summary. */
public class MoneyTotal {
    private String currency;
    private BigDecimal total;
    private long count;
    private String bucket; // optional: category name / source / month

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
}
