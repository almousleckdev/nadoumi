package com.nadoumi.program.domain;

import java.time.LocalDate;

/** Row of {@code nad_program_intake} -- an intake term for a programme. */
public class ProgramIntake {

    private Long id;
    private Long programId;
    private String term;
    private LocalDate applicationOpen;
    private LocalDate applicationClose;
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public LocalDate getApplicationOpen() { return applicationOpen; }
    public void setApplicationOpen(LocalDate applicationOpen) { this.applicationOpen = applicationOpen; }

    public LocalDate getApplicationClose() { return applicationClose; }
    public void setApplicationClose(LocalDate applicationClose) { this.applicationClose = applicationClose; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
