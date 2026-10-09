package com.nadoumi.content.mapper;

import com.nadoumi.content.domain.ArticleComment;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleCommentMapper {

    int insert(ArticleComment comment);

    ArticleComment findById(@Param("id") long id);

    /** Every comment of an article, oldest first, with the author's display name. */
    List<ArticleComment> findByArticle(@Param("articleId") long articleId);

    /** Comments the author posted at or after {@code since}; backs the per-user posting limit. */
    int countByAuthorSince(@Param("authorId") long authorId, @Param("since") LocalDateTime since);

    /** Soft delete. Returns 0 when the comment was already deleted. */
    /** Blanks every comment a retired student wrote; replies keep their place under the deleted marker. */
    int markDeletedByAuthor(@Param("authorId") long authorId, @Param("deletedBy") long deletedBy);

    int markDeleted(@Param("id") long id, @Param("deletedBy") long deletedBy);
}
