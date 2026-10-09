package com.nadoumi.content.web.response;

import java.util.List;

/** The most recent likers of an article (newest first) and the total number of likes. */
public record LikersPage(int total, List<Liker> likers) {
}
