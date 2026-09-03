package com.nadoumi.scholarship.mapper;

import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipCategory;
import com.nadoumi.scholarship.domain.ScholarshipDocumentRequirement;
import com.nadoumi.scholarship.domain.ScholarshipEligibility;
import com.nadoumi.scholarship.domain.ScholarshipFee;
import com.nadoumi.scholarship.domain.ScholarshipIntake;
import com.nadoumi.scholarship.domain.ScholarshipAccommodation;
import com.nadoumi.scholarship.domain.ScholarshipCoverage;
import com.nadoumi.scholarship.domain.ScholarshipInternal;
import com.nadoumi.scholarship.domain.ScholarshipLevelStipend;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * All public / student reads go through {@code v_scholarship_student} and the
 * student-safe child tables — never {@code nad_scholarship_internal}. The
 * {@code *Internal} methods are the only ones that touch the confidential table
 * and are called from staff, permission-checked code paths only.
 */
public interface ScholarshipMapper {

    // ---- public (v_scholarship_student) ----

    List<Scholarship> searchPublic(ScholarshipSearch filter);

    Scholarship findPublicBySlug(@Param("slug") String slug);

    Scholarship findPublicById(@Param("id") Long id);

    List<ScholarshipFacetRow> facetLevels(ScholarshipSearch filter);

    List<ScholarshipFacetRow> facetCategories(ScholarshipSearch filter);

    List<ScholarshipFacetRow> facetFundingModels(ScholarshipSearch filter);

    List<ScholarshipFacetRow> facetTeachingLanguages(ScholarshipSearch filter);

    /** The full extensible category reference, for filter UIs and the admin form. */
    List<ScholarshipCategory> allCategories();

    // ---- staff (nad_scholarship base table) ----

    List<Scholarship> searchStaff(ScholarshipSearch filter);

    Scholarship findById(@Param("id") Long id);

    Long findIdBySlug(@Param("slug") String slug);

    /** Highest numeric suffix used by a {@code <prefix>NNNN} reference code, or 0. */
    Integer maxReferenceSeq(@Param("prefix") String prefix);

    int insert(Scholarship scholarship);

    int update(Scholarship scholarship);

    int delete(@Param("id") Long id);

    int markPublished(@Param("id") Long id);

    // ---- children (shared by public + staff assembly) ----

    List<String> findLevels(@Param("scholarshipId") Long scholarshipId);

    List<ScholarshipCategory> findCategories(@Param("scholarshipId") Long scholarshipId);

    List<ScholarshipIntake> findIntakes(@Param("scholarshipId") Long scholarshipId);

    ScholarshipEligibility findEligibility(@Param("scholarshipId") Long scholarshipId);

    List<ScholarshipFee> findFees(@Param("scholarshipId") Long scholarshipId);

    List<ScholarshipLevelStipend> findLevelStipends(@Param("scholarshipId") Long scholarshipId);

    List<ScholarshipAccommodation> findAccommodations(@Param("scholarshipId") Long scholarshipId);

    List<ScholarshipCoverage> findCoverage(@Param("scholarshipId") Long scholarshipId);

    List<ScholarshipDocumentRequirement> findDocumentRequirements(@Param("scholarshipId") Long scholarshipId);

    // ---- child writes (staff) ----

    int deleteLevels(@Param("scholarshipId") Long scholarshipId);

    int insertLevel(@Param("scholarshipId") Long scholarshipId, @Param("level") String level);

    int deleteCategoryLinks(@Param("scholarshipId") Long scholarshipId);

    int insertCategoryLink(@Param("scholarshipId") Long scholarshipId, @Param("code") String code);

    int deleteIntakes(@Param("scholarshipId") Long scholarshipId);

    int insertIntake(@Param("scholarshipId") Long scholarshipId, @Param("in") ScholarshipIntake intake,
            @Param("sortOrder") int sortOrder);

    int deleteEligibility(@Param("scholarshipId") Long scholarshipId);

    int insertEligibility(@Param("scholarshipId") Long scholarshipId, @Param("e") ScholarshipEligibility eligibility);

    int deleteFees(@Param("scholarshipId") Long scholarshipId);

    int insertFee(@Param("scholarshipId") Long scholarshipId, @Param("f") ScholarshipFee fee,
            @Param("sortOrder") int sortOrder);

    int deleteLevelStipends(@Param("scholarshipId") Long scholarshipId);

    int insertLevelStipend(@Param("scholarshipId") Long scholarshipId, @Param("s") ScholarshipLevelStipend stipend);

    int deleteAccommodations(@Param("scholarshipId") Long scholarshipId);

    int insertAccommodation(@Param("scholarshipId") Long scholarshipId,
            @Param("a") ScholarshipAccommodation accommodation, @Param("sortOrder") int sortOrder);

    int deleteCoverage(@Param("scholarshipId") Long scholarshipId);

    int insertCoverage(@Param("scholarshipId") Long scholarshipId,
            @Param("c") ScholarshipCoverage coverage, @Param("sortOrder") int sortOrder);

    int deleteDocumentRequirements(@Param("scholarshipId") Long scholarshipId);

    int insertDocumentRequirement(@Param("scholarshipId") Long scholarshipId,
            @Param("d") ScholarshipDocumentRequirement requirement, @Param("sortOrder") int sortOrder);

    // ---- confidential (nad_scholarship_internal) — staff, permission-checked ----

    ScholarshipInternal findInternal(@Param("scholarshipId") Long scholarshipId);

    int upsertInternal(@Param("scholarshipId") Long scholarshipId, @Param("i") ScholarshipInternal internal,
            @Param("actor") String actor);
}
