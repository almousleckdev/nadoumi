package com.nadoumi.content.service;

import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.web.response.CommentNode;
import com.nadoumi.identity.profile.PublicProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Builds the nested public thread from a flat, oldest-first comment list. */
final class CommentTree {

    private CommentTree() {
    }

    /** {@code profileOf} gives the public face of an author id, or null when that user no longer exists. */
    static List<CommentNode> build(List<ArticleComment> flat, Function<Long, PublicProfile> profileOf) {
        Map<Long, List<ArticleComment>> childrenByParent = new LinkedHashMap<>();
        for (ArticleComment c : flat) {
            childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }
        return nodesOf(null, childrenByParent, profileOf);
    }

    private static List<CommentNode> nodesOf(Long parentId, Map<Long, List<ArticleComment>> childrenByParent,
            Function<Long, PublicProfile> profileOf) {
        List<CommentNode> nodes = new ArrayList<>();
        for (ArticleComment c : childrenByParent.getOrDefault(parentId, List.of())) {
            boolean deleted = c.getStatus() == CommentStatus.DELETED;
            PublicProfile author = deleted ? null : profileOf.apply(c.getAuthorId());
            nodes.add(new CommentNode(c.getId(), c.getParentId(),
                    author == null ? null : author.displayName(), author == null ? null : author.avatarUrl(),
                    deleted ? null : c.getBody(), deleted,
                    c.getLikeCount() == null ? 0 : c.getLikeCount(), c.getCreateTime(),
                    nodesOf(c.getId(), childrenByParent, profileOf)));
        }
        return nodes;
    }
}
