package com.nadoumi.content.web.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A comment in the public thread. A deleted comment keeps its place (so replies stay attached)
 * but carries no body and no author. The author is shown the way {@code PublicProfileRules} allows:
 * a student by first name with no photo, a staff member by name and photo.
 */
public record CommentNode(Long id, Long parentId, String authorName, String authorAvatarUrl, String body,
        boolean deleted, int likeCount, LocalDateTime createTime, List<CommentNode> replies) {
}
