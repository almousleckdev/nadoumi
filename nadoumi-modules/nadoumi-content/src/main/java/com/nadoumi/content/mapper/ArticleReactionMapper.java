package com.nadoumi.content.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Reader likes on articles and comments. One row per (item, user); writes are idempotent. */
@Mapper
public interface ArticleReactionMapper {

    /** No-op when the reader already liked the article. */
    int insertArticleLike(@Param("articleId") long articleId, @Param("userId") long userId);

    int deleteArticleLike(@Param("articleId") long articleId, @Param("userId") long userId);

    /** Removes every like a retired student gave, on articles and on comments. */
    int removeArticleLikesByUser(@Param("userId") long userId);

    int removeCommentLikesByUser(@Param("userId") long userId);

    int countArticleLikes(@Param("articleId") long articleId);

    /** Ids of the most recent likers, newest first. */
    List<Long> recentArticleLikerIds(@Param("articleId") long articleId, @Param("limit") int limit);

    boolean articleLikedBy(@Param("articleId") long articleId, @Param("userId") long userId);

    /** No-op when the reader already liked the comment. */
    int insertCommentLike(@Param("commentId") long commentId, @Param("userId") long userId);

    int deleteCommentLike(@Param("commentId") long commentId, @Param("userId") long userId);

    int countCommentLikes(@Param("commentId") long commentId);

    /** Ids of the article's comments the reader has liked. */
    List<Long> commentIdsLikedBy(@Param("articleId") long articleId, @Param("userId") long userId);
}
