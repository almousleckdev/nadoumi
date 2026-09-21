package com.nadoumi.application.mapper;

import com.nadoumi.application.domain.Application;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApplicationMapper {

    Application findById(Long id);

    List<Application> searchStaff(ApplicationSearch filter);

    /** All applications for one applicant, newest first — the student "my applications" list. */
    List<Application> findByApplicant(@Param("applicantId") Long applicantId);

    /** Just the owning applicant id — backs {@code ApplicationApplicantResolverImpl}. */
    Long findApplicantId(@Param("id") Long id);

    int insert(Application application);

    /**
     * Full row update guarded by the optimistic lock: {@code version = version + 1}
     * only when the caller's {@code expectedVersion} still matches. Returns 0 rows
     * affected on a conflict — the caller maps that to 409.
     */
    int updateWithLock(@Param("app") Application application, @Param("expectedVersion") int expectedVersion);

    /** Non-engine field edits while still {@code DRAFT} (opportunity / intake) — no lock needed, DRAFT is single-writer. */
    int updateDraftFields(Application application);

    /** One-time link, right after the paired {@code nad_wf_instance} row is created in {@code startDraft}. */
    int linkWorkflowInstance(@Param("id") Long id, @Param("workflowInstanceId") Long workflowInstanceId);
}
