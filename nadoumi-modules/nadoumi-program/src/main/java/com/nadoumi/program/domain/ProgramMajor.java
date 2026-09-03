package com.nadoumi.program.domain;

/** Row of {@code nad_program_major} -- a major / specialisation under a programme. */
public class ProgramMajor {

    private Long id;
    private Long programId;
    private String name;
    private String nameCn;
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
