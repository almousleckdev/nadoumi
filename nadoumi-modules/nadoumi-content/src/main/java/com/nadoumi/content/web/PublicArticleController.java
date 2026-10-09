package com.nadoumi.content.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.content.service.ArticleService;
import com.nadoumi.content.web.response.ArticleSummary;
import com.nadoumi.content.web.response.PublicArticleDetail;
import com.ruoyi.common.annotation.Anonymous;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public news feed — anonymous, PUBLISHED articles only, through public DTOs. */
@RestController
@RequestMapping("/api/public/news")
public class PublicArticleController {

    private final ArticleService service;

    public PublicArticleController(ArticleService service) {
        this.service = service;
    }

    @Anonymous
    @GetMapping
    public PageResponse<ArticleSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.publicList(q, language, page, size);
    }

    @Anonymous
    @GetMapping("/{slug}/related")
    public List<ArticleSummary> related(@PathVariable String slug, @RequestParam(defaultValue = "4") int limit) {
        return service.related(slug, limit);
    }

    @Anonymous
    @GetMapping("/{slug}")
    public PublicArticleDetail get(@PathVariable String slug) {
        return service.publicGet(slug);
    }
}
