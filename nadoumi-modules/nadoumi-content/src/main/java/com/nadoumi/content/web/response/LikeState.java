package com.nadoumi.content.web.response;

/** The result of liking or unliking: the reader's state and the item's new total. */
public record LikeState(boolean liked, int likeCount) {
}
