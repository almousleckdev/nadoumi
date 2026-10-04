package com.nadoumi.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.web.response.CommentNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommentTreeTest {

    private static ArticleComment comment(long id, Long parentId, CommentStatus status) {
        ArticleComment c = new ArticleComment();
        c.setId(id);
        c.setParentId(parentId);
        c.setAuthorName("user" + id);
        c.setBody("body" + id);
        c.setStatus(status);
        return c;
    }

    @Test
    void shouldNestRepliesToAnyDepth_whenCommentsReplyToReplies() {
        List<CommentNode> tree = CommentTree.build(List.of(
                comment(1, null, CommentStatus.VISIBLE),
                comment(2, 1L, CommentStatus.VISIBLE),
                comment(3, 2L, CommentStatus.VISIBLE),
                comment(4, null, CommentStatus.VISIBLE)));

        assertThat(tree).extracting(CommentNode::id).containsExactly(1L, 4L);
        assertThat(tree.get(0).replies()).extracting(CommentNode::id).containsExactly(2L);
        assertThat(tree.get(0).replies().get(0).replies()).extracting(CommentNode::id).containsExactly(3L);
    }

    @Test
    void shouldHideBodyAndAuthorButKeepReplies_whenCommentIsDeleted() {
        List<CommentNode> tree = CommentTree.build(List.of(
                comment(1, null, CommentStatus.DELETED),
                comment(2, 1L, CommentStatus.VISIBLE)));

        CommentNode deleted = tree.get(0);
        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.body()).isNull();
        assertThat(deleted.authorName()).isNull();
        assertThat(deleted.replies()).extracting(CommentNode::body).containsExactly("body2");
    }
}
