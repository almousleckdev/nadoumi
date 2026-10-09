package com.nadoumi.communication.mapper;

import com.nadoumi.communication.domain.MessageAttachment;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MessageAttachmentMapper {

    MessageAttachment findById(@Param("id") long id);

    List<MessageAttachment> listByMessage(@Param("messageId") long messageId);

    List<MessageAttachment> listByMessageIds(@Param("messageIds") java.util.Collection<Long> messageIds);

    /** Every media asset attached anywhere in the conversation (to be released when it is deleted). */
    List<Long> listMediaIdsByConversation(@Param("conversationId") long conversationId);

    int insert(MessageAttachment attachment);

    int setPromotedDocumentId(@Param("id") long id, @Param("documentId") long documentId);
}
