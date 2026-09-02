package com.nadoumi.university.domain;

import com.nadoumi.university.domain.enums.UniversityStatus;
import java.time.LocalDateTime;

/** Row of {@code nad_university} — public catalog entity, no commercial data. */
public class University {

    private Long id;
    private String name;
    private String country;
    private String city;
    private String website;
    private String rankingTier;
    private Long logoDocumentId;
    private UniversityStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getRankingTier() { return rankingTier; }
    public void setRankingTier(String rankingTier) { this.rankingTier = rankingTier; }

    public Long getLogoDocumentId() { return logoDocumentId; }
    public void setLogoDocumentId(Long logoDocumentId) { this.logoDocumentId = logoDocumentId; }

    public UniversityStatus getStatus() { return status; }
    public void setStatus(UniversityStatus status) { this.status = status; }

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
}
