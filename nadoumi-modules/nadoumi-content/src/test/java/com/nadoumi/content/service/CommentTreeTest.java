package com.nadoumi.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.web.response.CommentNode;
import com.nadoumi.identity.profile.PublicProfile;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CommentTreeTest {

    /** Every author resolves to "user<id>" with a photo, unless the author id is 990 (a user that no longer exists). */
    private static final Function<Long, PublicProfile> PROFILES =
            authorId -> authorId == 990L ? null : new PublicProfile("user" + authorId / 10, "https://cdn.example/a" + authorId + ".jpg");

    private static ArticleComment comment(long id, Long parentId, CommentStatus status) {
        ArticleComment c = new ArticleComment();
        c.setId(id);
        c.setParentId(parentId);
        c.setAuthorId(id * 10);
        c.setAuthorName("Full Surname" + id); // the staff-facing join: must never reach the public thread
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
                comment(4, null, CommentStatus.VISIBLE)), PROFILES);

        assertThat(tree).extracting(CommentNode::id).containsExactly(1L, 4L);
        assertThat(tree.get(0).replies()).extracting(CommentNode::id).containsExactly(2L);
        assertThat(tree.get(0).replies().get(0).replies()).extracting(CommentNode::id).containsExactly(3L);
    }

    @Test
    void shouldHideBodyAndAuthorButKeepReplies_whenCommentIsDeleted() {
        List<CommentNode> tree = CommentTree.build(List.of(
                comment(1, null, CommentStatus.DELETED),
                comment(2, 1L, CommentStatus.VISIBLE)), PROFILES);

        CommentNode deleted = tree.get(0);
        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.body()).isNull();
        assertThat(deleted.authorName()).isNull();
        assertThat(deleted.authorAvatarUrl()).isNull();
        assertThat(deleted.replies()).extracting(CommentNode::body).containsExactly("body2");
    }

    @Test
    void shouldShowTheResolvedPublicProfile_notTheStaffFacingName() {
        List<CommentNode> tree = CommentTree.build(List.of(comment(1, null, CommentStatus.VISIBLE)), PROFILES);

        assertThat(tree.get(0).authorName()).isEqualTo("user1");
        assertThat(tree.get(0).authorAvatarUrl()).isEqualTo("https://cdn.example/a10.jpg");
    }

    @Test
    void shouldLeaveTheAuthorEmpty_whenTheUserNoLongerExists() {
        ArticleComment orphan = comment(99, null, CommentStatus.VISIBLE);
        orphan.setAuthorId(990L);

        CommentNode node = CommentTree.build(List.of(orphan), PROFILES).get(0);

        assertThat(node.authorName()).isNull();
        assertThat(node.authorAvatarUrl()).isNull();
        assertThat(node.body()).isEqualTo("body99");
    }
}
