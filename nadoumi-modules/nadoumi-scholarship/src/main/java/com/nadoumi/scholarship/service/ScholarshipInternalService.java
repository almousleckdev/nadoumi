package com.nadoumi.scholarship.service;

import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.scholarship.domain.ScholarshipInternal;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.web.request.ScholarshipInternalRequest;
import com.nadoumi.scholarship.web.response.ScholarshipInternalResponse;
import com.nadoumi.university.service.UniversityService;
import com.ruoyi.common.utils.AuditActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The confidential {@code nad_scholarship_internal} linkage (partner university, programme,
 * partnership, operational and commercial terms). Staff-only: callers are permission-checked at the
 * controller ({@code nad:scholarship:internal:*}) and this data must never reach a student-facing DTO.
 */
@Service
public class ScholarshipInternalService {

    private static final String DEFAULT_INTERNAL_STATUS = "DRAFT";

    private final ScholarshipMapper mapper;
    private final UniversityService universityService;

    public ScholarshipInternalService(ScholarshipMapper mapper, UniversityService universityService) {
        this.mapper = mapper;
        this.universityService = universityService;
    }

    @Transactional(readOnly = true)
    public ScholarshipInternalResponse get(Long scholarshipId) {
        requireScholarship(scholarshipId);
        ScholarshipInternal internal = mapper.findInternal(scholarshipId);
        String universityName = internal != null && internal.universityId() != null
                ? universityService.get(internal.universityId()).name() : null;
        return ScholarshipInternalResponse.of(internal, universityName);
    }

    @Transactional(rollbackFor = Exception.class)
    public ScholarshipInternalResponse put(Long scholarshipId, ScholarshipInternalRequest req) {
        requireScholarship(scholarshipId);
        if (req.universityId() != null) {
            universityService.get(req.universityId());
        }
        mapper.upsertInternal(scholarshipId, new ScholarshipInternal(
                req.universityId(), req.programId(), req.partnershipId(),
                req.internalStatus() == null ? DEFAULT_INTERNAL_STATUS : req.internalStatus(),
                req.operationalNotes(), req.confidentialTerms(), req.commissionModelJson()), AuditActor.username());
        return get(scholarshipId);
    }

    private void requireScholarship(Long scholarshipId) {
        if (mapper.findById(scholarshipId) == null) {
            throw new NadNotFoundException("scholarship not found");
        }
    }
}
