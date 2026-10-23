package com.nadoumi.program.domain;

import java.math.BigDecimal;

/**
 * Row of {@code nad_program_offering} -- one duration of one level of a programme,
 * with its tuition and accommodation. The USD figures are display values filled in
 * by the service from the editable FX rate; they are not stored.
 */
public class ProgramOffering {

    private Long id;
    private Long programId;
    private String level;
    private int durationMonths;
    private BigDecimal tuitionAmount;
    private String tuitionCurrency;
    private String tuitionPeriod;
    private BigDecimal accommodationAmount;
    private String accommodationCurrency;
    private String accommodationPeriod;
    private String note;
    private int sortOrder;

    private BigDecimal tuitionAmountUsd;
    private BigDecimal accommodationAmountUsd;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }

    public BigDecimal getTuitionAmount() { return tuitionAmount; }
    public void setTuitionAmount(BigDecimal tuitionAmount) { this.tuitionAmount = tuitionAmount; }

    public String getTuitionCurrency() { return tuitionCurrency; }
    public void setTuitionCurrency(String tuitionCurrency) { this.tuitionCurrency = tuitionCurrency; }

    public String getTuitionPeriod() { return tuitionPeriod; }
    public void setTuitionPeriod(String tuitionPeriod) { this.tuitionPeriod = tuitionPeriod; }

    public BigDecimal getAccommodationAmount() { return accommodationAmount; }
    public void setAccommodationAmount(BigDecimal accommodationAmount) { this.accommodationAmount = accommodationAmount; }

    public String getAccommodationCurrency() { return accommodationCurrency; }
    public void setAccommodationCurrency(String accommodationCurrency) { this.accommodationCurrency = accommodationCurrency; }

    public String getAccommodationPeriod() { return accommodationPeriod; }
    public void setAccommodationPeriod(String accommodationPeriod) { this.accommodationPeriod = accommodationPeriod; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public BigDecimal getTuitionAmountUsd() { return tuitionAmountUsd; }
    public void setTuitionAmountUsd(BigDecimal tuitionAmountUsd) { this.tuitionAmountUsd = tuitionAmountUsd; }

    public BigDecimal getAccommodationAmountUsd() { return accommodationAmountUsd; }
    public void setAccommodationAmountUsd(BigDecimal accommodationAmountUsd) { this.accommodationAmountUsd = accommodationAmountUsd; }
}
