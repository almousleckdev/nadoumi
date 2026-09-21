package com.nadoumi.document.mapper;

import com.nadoumi.document.domain.DocumentVersion;
import com.nadoumi.document.domain.enums.VerificationStatus;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DocumentVersionMapper {

    DocumentVersion findById(Long id);

    List<DocumentVersion> findByDocument(@Param("documentId") long documentId);

    /** Highest version_no for a document, or 0 if none exist yet. */
    int maxVersionNo(@Param("documentId") long documentId);

    int insert(DocumentVersion version);

    int updateVerification(@Param("id") long id, @Param("status") VerificationStatus verificationStatus,
            @Param("verifiedBy") long verifiedBy);
}
