package com.nadoumi.program.domain;

/** Row of {@code nad_major}: one subject in the shared catalogue, reused by programmes at any university. */
public class Major {

    private Long id;
    private String name;
    private String nameCn;
    private String discipline;
    /** Read-only: how many programme links point at this major. Filled by the list query only. */
    private int programCount;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public int getProgramCount() { return programCount; }
    public void setProgramCount(int programCount) { this.programCount = programCount; }
}
