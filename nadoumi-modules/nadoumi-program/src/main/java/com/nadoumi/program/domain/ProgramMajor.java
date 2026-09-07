package com.nadoumi.program.domain;

/** Row of {@code nad_program_major} -- a major / specialisation under a programme. */
public class ProgramMajor {

    private Long id;
    private Long programId;
    private Long departmentId;
    private String departmentName;
    private String level;
    private String name;
    private String nameCn;
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
