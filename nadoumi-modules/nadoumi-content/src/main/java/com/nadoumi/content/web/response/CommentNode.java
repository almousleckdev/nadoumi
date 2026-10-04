package com.nadoumi.content.web.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A comment in the public thread. A deleted comment keeps its place (so replies stay attached)
 * but carries no body and no author.
 */
public record CommentNode(Long id, Long parentId, String authorName, String body, boolean deleted,
        LocalDateTime createTime, List<CommentNode> replies) {
}
