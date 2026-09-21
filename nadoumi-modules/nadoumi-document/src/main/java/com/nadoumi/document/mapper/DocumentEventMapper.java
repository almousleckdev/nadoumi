package com.nadoumi.document.mapper;

import com.nadoumi.document.domain.DocumentEvent;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DocumentEventMapper {

    List<DocumentEvent> findByDocument(@Param("documentId") long documentId);

    int insert(DocumentEvent event);
}
