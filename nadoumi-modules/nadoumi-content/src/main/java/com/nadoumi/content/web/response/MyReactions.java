package com.nadoumi.content.web.response;

import java.util.List;

/** What the signed-in reader has already liked on one article. Never contains other readers' data. */
public record MyReactions(boolean articleLiked, List<Long> likedCommentIds) {
}
