package com.nadoumi.university.domain;

import java.time.LocalDateTime;

/**
 * Row of {@code nad_department} -- an academic department / college inside a
 * university. A degree programme's majors each belong to one department.
 */
public class Department {

    private Long id;
    private Long universityId;
    private String name;
    private String nameCn;
    private int sortOrder;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    /** Assembled by the service (count of majors that reference this department). */
    private int programCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long universityId) { this.universityId = universityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public int getProgramCount() { return programCount; }
    public void setProgramCount(int programCount) { this.programCount = programCount; }
}
