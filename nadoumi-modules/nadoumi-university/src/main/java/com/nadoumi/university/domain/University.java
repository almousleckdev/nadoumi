package com.nadoumi.university.domain;

import com.nadoumi.university.domain.enums.PublishStatus;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.domain.enums.UniversityType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Row of {@code nad_university} — public catalog entity, no commercial data.
 * {@link #rankings} / {@link #highlights} are assembled by the service, not by
 * the base result map.
 */
public class University {

    private Long id;
    private String name;
    private String nameCn;
    private String slug;
    private String referenceCode;
    private String partnerStatus;
    private String country;
    private UniversityType type;
    private String city;
    private String province;
    private Short foundedYear;
    private Integer totalStudents;
    private Integer internationalStudents;
    private Integer facultyCount;
    private String website;
    private String rankingTier;
    private String introduction;
    private String history;
    private String campusInfo;
    private String accommodationInfo;
    private String nearbyInfo;
    private String admissionsEmail;
    private String officePhone;
    private Long logoDocumentId;
    private Long bannerDocumentId;
    private String logoImageUrl;
    private String coverImageUrl;
    private Long logoMediaId;
    private Long bannerMediaId;
    private boolean recommended;
    private boolean featured;
    private boolean publicPartner;
    private UniversityStatus status;
    private PublishStatus publishStatus;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;

    private List<UniversityRanking> rankings = new ArrayList<>();
    private List<UniversityHighlight> highlights = new ArrayList<>();
    private List<UniversityGalleryImage> gallery = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getPartnerStatus() { return partnerStatus; }
    public void setPartnerStatus(String partnerStatus) { this.partnerStatus = partnerStatus; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public UniversityType getType() { return type; }
    public void setType(UniversityType type) { this.type = type; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public Short getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Short foundedYear) { this.foundedYear = foundedYear; }

    public Integer getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Integer totalStudents) { this.totalStudents = totalStudents; }

    public Integer getInternationalStudents() { return internationalStudents; }
    public void setInternationalStudents(Integer v) { this.internationalStudents = v; }

    public Integer getFacultyCount() { return facultyCount; }
    public void setFacultyCount(Integer facultyCount) { this.facultyCount = facultyCount; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getRankingTier() { return rankingTier; }
    public void setRankingTier(String rankingTier) { this.rankingTier = rankingTier; }

    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }

    public String getHistory() { return history; }
    public void setHistory(String history) { this.history = history; }

    public String getCampusInfo() { return campusInfo; }
    public void setCampusInfo(String campusInfo) { this.campusInfo = campusInfo; }

    public String getAccommodationInfo() { return accommodationInfo; }
    public void setAccommodationInfo(String v) { this.accommodationInfo = v; }

    public String getNearbyInfo() { return nearbyInfo; }
    public void setNearbyInfo(String nearbyInfo) { this.nearbyInfo = nearbyInfo; }

    public String getAdmissionsEmail() { return admissionsEmail; }
    public void setAdmissionsEmail(String admissionsEmail) { this.admissionsEmail = admissionsEmail; }

    public String getOfficePhone() { return officePhone; }
    public void setOfficePhone(String officePhone) { this.officePhone = officePhone; }

    public Long getLogoDocumentId() { return logoDocumentId; }
    public void setLogoDocumentId(Long logoDocumentId) { this.logoDocumentId = logoDocumentId; }

    public Long getBannerDocumentId() { return bannerDocumentId; }
    public void setBannerDocumentId(Long bannerDocumentId) { this.bannerDocumentId = bannerDocumentId; }

    public String getLogoImageUrl() { return logoImageUrl; }
    public void setLogoImageUrl(String logoImageUrl) { this.logoImageUrl = logoImageUrl; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public Long getLogoMediaId() { return logoMediaId; }
    public void setLogoMediaId(Long logoMediaId) { this.logoMediaId = logoMediaId; }

    public Long getBannerMediaId() { return bannerMediaId; }
    public void setBannerMediaId(Long bannerMediaId) { this.bannerMediaId = bannerMediaId; }

    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isPublicPartner() { return publicPartner; }
    public void setPublicPartner(boolean publicPartner) { this.publicPartner = publicPartner; }

    public UniversityStatus getStatus() { return status; }
    public void setStatus(UniversityStatus status) { this.status = status; }

    public PublishStatus getPublishStatus() { return publishStatus; }
    public void setPublishStatus(PublishStatus publishStatus) { this.publishStatus = publishStatus; }

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

    public List<UniversityRanking> getRankings() { return rankings; }
    public void setRankings(List<UniversityRanking> rankings) { this.rankings = rankings; }

    public List<UniversityHighlight> getHighlights() { return highlights; }
    public void setHighlights(List<UniversityHighlight> highlights) { this.highlights = highlights; }

    public List<UniversityGalleryImage> getGallery() { return gallery; }
    public void setGallery(List<UniversityGalleryImage> gallery) { this.gallery = gallery; }
}
