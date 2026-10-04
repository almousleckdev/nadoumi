package com.nadoumi.content.mapper;

import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.enums.ArticleStatus;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleMapper {

    int insert(Article article);

    int update(Article article);

    Article findById(@Param("id") long id);

    Article findBySlug(@Param("slug") String slug);

    Long findIdBySlug(@Param("slug") String slug);

    List<Article> search(ArticleSearch filter);

    /** Sets the status; {@code publishedAt} is only written when the row has none yet. */
    int updateStatus(@Param("id") long id, @Param("status") ArticleStatus status, @Param("updateBy") String updateBy);

    int updateCoverMediaId(@Param("id") long id, @Param("mediaId") long mediaId, @Param("updateBy") String updateBy);

    int insertImage(@Param("articleId") long articleId, @Param("mediaId") long mediaId,
            @Param("sortOrder") int sortOrder, @Param("createBy") String createBy);

    int countImages(@Param("articleId") long articleId);

    int delete(@Param("id") long id);
}
