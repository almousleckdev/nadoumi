package com.nadoumi.communication.mapper;

import com.nadoumi.communication.domain.MessageAttachment;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MessageAttachmentMapper {

    MessageAttachment findById(@Param("id") long id);

    List<MessageAttachment> listByMessage(@Param("messageId") long messageId);

    int insert(MessageAttachment attachment);

    int setPromotedDocumentId(@Param("id") long id, @Param("documentId") long documentId);
}
