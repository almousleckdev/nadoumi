package com.nadoumi.program.domain;

import com.nadoumi.program.domain.enums.ProgramStatus;
import com.nadoumi.program.domain.enums.ProgramTeachingLanguage;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.domain.enums.PublishStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Row of {@code nad_program} -- a course of study offered by exactly one
 * university. {@link #majors} / {@link #intakes} are assembled by the service,
 * not by the base result map. {@link #universityName} is resolved through
 * {@code UniversityService}, never a SQL join.
 */
public class Program {

    private Long id;
    private Long universityId;
    private String name;
    private String nameCn;
    private String slug;
    private ProgramType programType;
    private String field;
    private ProgramTeachingLanguage teachingLanguage;
    private Integer durationMonths;
    private BigDecimal tuitionAmount;
    private String tuitionCurrency;
    private String summary;
    private boolean featured;
    private boolean hot;
    private PublishStatus publishStatus;
    private ProgramStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;

    private String universityName;
    private String universitySlug;

    private List<ProgramMajor> majors = new ArrayList<>();
    private List<ProgramIntake> intakes = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long universityId) { this.universityId = universityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public ProgramType getProgramType() { return programType; }
    public void setProgramType(ProgramType programType) { this.programType = programType; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public ProgramTeachingLanguage getTeachingLanguage() { return teachingLanguage; }
    public void setTeachingLanguage(ProgramTeachingLanguage teachingLanguage) { this.teachingLanguage = teachingLanguage; }

    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }

    public BigDecimal getTuitionAmount() { return tuitionAmount; }
    public void setTuitionAmount(BigDecimal tuitionAmount) { this.tuitionAmount = tuitionAmount; }

    public String getTuitionCurrency() { return tuitionCurrency; }
    public void setTuitionCurrency(String tuitionCurrency) { this.tuitionCurrency = tuitionCurrency; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isHot() { return hot; }
    public void setHot(boolean hot) { this.hot = hot; }

    public PublishStatus getPublishStatus() { return publishStatus; }
    public void setPublishStatus(PublishStatus publishStatus) { this.publishStatus = publishStatus; }

    public ProgramStatus getStatus() { return status; }
    public void setStatus(ProgramStatus status) { this.status = status; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }

    public String getUniversitySlug() { return universitySlug; }
    public void setUniversitySlug(String universitySlug) { this.universitySlug = universitySlug; }

    public List<ProgramMajor> getMajors() { return majors; }
    public void setMajors(List<ProgramMajor> majors) { this.majors = majors; }

    public List<ProgramIntake> getIntakes() { return intakes; }
    public void setIntakes(List<ProgramIntake> intakes) { this.intakes = intakes; }
}
