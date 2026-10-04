package com.nadoumi.content.service;

import com.nadoumi.content.domain.ArticleComment;
import com.nadoumi.content.domain.enums.CommentStatus;
import com.nadoumi.content.web.response.CommentNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the nested public thread from a flat, oldest-first comment list. */
final class CommentTree {

    private CommentTree() {
    }

    static List<CommentNode> build(List<ArticleComment> flat) {
        Map<Long, List<ArticleComment>> childrenByParent = new LinkedHashMap<>();
        for (ArticleComment c : flat) {
            childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }
        return nodesOf(null, childrenByParent);
    }

    private static List<CommentNode> nodesOf(Long parentId, Map<Long, List<ArticleComment>> childrenByParent) {
        List<CommentNode> nodes = new ArrayList<>();
        for (ArticleComment c : childrenByParent.getOrDefault(parentId, List.of())) {
            boolean deleted = c.getStatus() == CommentStatus.DELETED;
            nodes.add(new CommentNode(c.getId(), c.getParentId(),
                    deleted ? null : c.getAuthorName(), deleted ? null : c.getBody(), deleted,
                    c.getCreateTime(), nodesOf(c.getId(), childrenByParent)));
        }
        return nodes;
    }
}
