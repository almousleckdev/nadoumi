package com.nadoumi.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of the money ledger: revenue coming in, or an approved or paid expense going out. */
public class LedgerEntry {
    private String kind;       // REVENUE | EXPENSE
    private long id;
    private LocalDate occurredOn;
    private String title;
    private String category;   // revenue source or expense category
    private String reference;
    private BigDecimal amount; // always positive; kind says the direction
    private String currency;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
