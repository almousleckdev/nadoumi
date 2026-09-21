package com.nadoumi.document.mapper;

import com.nadoumi.document.domain.Document;
import com.nadoumi.document.domain.enums.DocumentStatus;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DocumentMapper {

    Document findById(Long id);

    /** Own documents, optionally scoped to one application. */
    List<Document> findByApplicant(@Param("applicantId") long applicantId, @Param("applicationId") Long applicationId);

    /** Staff queue: by application or by applicant. */
    List<Document> search(@Param("applicantId") Long applicantId, @Param("applicationId") Long applicationId);

    int insert(Document document);

    /** Repoints current_version_id, sets the derived status, stamps updated_by/time. */
    int updateCurrentVersion(@Param("id") long id, @Param("currentVersionId") long currentVersionId,
            @Param("status") DocumentStatus status, @Param("updateBy") String updateBy);

    int updateStatus(@Param("id") long id, @Param("status") DocumentStatus status,
            @Param("reviewerUserId") Long reviewerUserId, @Param("rejectionReason") String rejectionReason,
            @Param("updateBy") String updateBy);

    /** Only while status = DRAFT — a submitted document is never hard-deleted. */
    int deleteDraft(@Param("id") long id);
}
