package com.nadoumi.scholarship.domain;

import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.domain.enums.PublishStatus;
import com.nadoumi.scholarship.domain.enums.ScholarshipStatus;
import com.nadoumi.scholarship.domain.enums.TeachingLanguage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Row of {@code nad_scholarship} — the STUDENT-SAFE head of a scholarship. The
 * university / partnership linkage is not here; it lives in
 * {@link ScholarshipInternal}. Child collections are assembled by the service.
 */
public class Scholarship {

    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String country;
    private String province;
    private String city;
    private String field;
    private TeachingLanguage teachingLanguage;
    private FundingModel fundingModel;
    private boolean hasStipend;
    private LocalDate deadline;
    private String benefits;
    private String requirements;
    private String policy;
    private String nonDegreeDuration;
    private BigDecimal applicationFeeAmount;
    private String applicationFeeCurrency;
    private BigDecimal serviceFeeAmount;
    private String serviceFeeCurrency;
    private Integer slots;
    private boolean featured;
    private boolean recommended;
    private boolean hot;
    private PublishStatus publishStatus;
    private LocalDateTime publishedAt;
    private ScholarshipStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;
    private String heroImageUrl;
    private String coverImageUrl;

    private List<String> levels = new ArrayList<>();
    private List<ScholarshipCategory> categories = new ArrayList<>();
    private List<ScholarshipIntake> intakes = new ArrayList<>();
    private ScholarshipEligibility eligibility;
    private List<ScholarshipFee> fees = new ArrayList<>();
    private List<ScholarshipLevelStipend> levelStipends = new ArrayList<>();
    private List<ScholarshipAccommodation> accommodations = new ArrayList<>();
    private List<ScholarshipDocumentRequirement> documentRequirements = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public TeachingLanguage getTeachingLanguage() { return teachingLanguage; }
    public void setTeachingLanguage(TeachingLanguage teachingLanguage) { this.teachingLanguage = teachingLanguage; }
    public FundingModel getFundingModel() { return fundingModel; }
    public void setFundingModel(FundingModel fundingModel) { this.fundingModel = fundingModel; }
    public boolean isHasStipend() { return hasStipend; }
    public void setHasStipend(boolean hasStipend) { this.hasStipend = hasStipend; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getPolicy() { return policy; }
    public void setPolicy(String policy) { this.policy = policy; }
    public String getNonDegreeDuration() { return nonDegreeDuration; }
    public void setNonDegreeDuration(String nonDegreeDuration) { this.nonDegreeDuration = nonDegreeDuration; }
    public BigDecimal getApplicationFeeAmount() { return applicationFeeAmount; }
    public void setApplicationFeeAmount(BigDecimal v) { this.applicationFeeAmount = v; }
    public String getApplicationFeeCurrency() { return applicationFeeCurrency; }
    public void setApplicationFeeCurrency(String v) { this.applicationFeeCurrency = v; }
    public BigDecimal getServiceFeeAmount() { return serviceFeeAmount; }
    public void setServiceFeeAmount(BigDecimal v) { this.serviceFeeAmount = v; }
    public String getServiceFeeCurrency() { return serviceFeeCurrency; }
    public void setServiceFeeCurrency(String v) { this.serviceFeeCurrency = v; }
    public Integer getSlots() { return slots; }
    public void setSlots(Integer slots) { this.slots = slots; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
    public boolean isHot() { return hot; }
    public void setHot(boolean hot) { this.hot = hot; }
    public PublishStatus getPublishStatus() { return publishStatus; }
    public void setPublishStatus(PublishStatus publishStatus) { this.publishStatus = publishStatus; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public ScholarshipStatus getStatus() { return status; }
    public void setStatus(ScholarshipStatus status) { this.status = status; }
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

    public String getHeroImageUrl() { return heroImageUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public List<String> getLevels() { return levels; }
    public void setLevels(List<String> levels) { this.levels = levels; }
    public List<ScholarshipCategory> getCategories() { return categories; }
    public void setCategories(List<ScholarshipCategory> categories) { this.categories = categories; }
    public List<ScholarshipIntake> getIntakes() { return intakes; }
    public void setIntakes(List<ScholarshipIntake> intakes) { this.intakes = intakes; }
    public ScholarshipEligibility getEligibility() { return eligibility; }
    public void setEligibility(ScholarshipEligibility eligibility) { this.eligibility = eligibility; }
    public List<ScholarshipFee> getFees() { return fees; }
    public void setFees(List<ScholarshipFee> fees) { this.fees = fees; }
    public List<ScholarshipLevelStipend> getLevelStipends() { return levelStipends; }
    public void setLevelStipends(List<ScholarshipLevelStipend> levelStipends) { this.levelStipends = levelStipends; }
    public List<ScholarshipAccommodation> getAccommodations() { return accommodations; }
    public void setAccommodations(List<ScholarshipAccommodation> accommodations) { this.accommodations = accommodations; }
    public List<ScholarshipDocumentRequirement> getDocumentRequirements() { return documentRequirements; }
    public void setDocumentRequirements(List<ScholarshipDocumentRequirement> v) { this.documentRequirements = v; }
}
