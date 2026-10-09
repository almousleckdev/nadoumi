package com.nadoumi.document.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Rows the document module owns for one applicant, read and removed only by permanent erasure. */
public interface DocumentErasureMapper {

    /** Media assets behind every version of every document of the applicant. */
    List<Long> mediaAssetIds(@Param("applicantId") long applicantId);

    int removeEvents(@Param("applicantId") long applicantId);

    int removeVersions(@Param("applicantId") long applicantId);

    int removeDocuments(@Param("applicantId") long applicantId);
}
