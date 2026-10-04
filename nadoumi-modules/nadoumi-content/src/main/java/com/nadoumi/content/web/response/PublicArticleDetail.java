package com.nadoumi.content.web.response;

import java.util.List;

public record PublicArticleDetail(ArticleSummary article, String bodyMd, List<CommentNode> comments) {
}
