package com.nadoumi.content.service;

import com.nadoumi.common.erasure.StudentRetirementParticipant;
import com.nadoumi.content.mapper.ArticleCommentMapper;
import com.nadoumi.content.mapper.ArticleReactionMapper;
import com.nadoumi.identity.access.CurrentCaller;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** A retired student's comments become deleted markers and their likes are removed from the counts. */
@Component
@Order(30)
class ContentRetirement implements StudentRetirementParticipant {

    private final ArticleCommentMapper comments;
    private final ArticleReactionMapper reactions;
    private final CurrentCaller caller;

    ContentRetirement(ArticleCommentMapper comments, ArticleReactionMapper reactions, CurrentCaller caller) {
        this.comments = comments;
        this.reactions = reactions;
        this.caller = caller;
    }

    @Override
    public void retire(long userId) {
        comments.markDeletedByAuthor(userId, caller.requireUserId());
        reactions.removeArticleLikesByUser(userId);
        reactions.removeCommentLikesByUser(userId);
    }
}
