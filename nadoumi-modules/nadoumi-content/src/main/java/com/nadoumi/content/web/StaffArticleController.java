package com.nadoumi.content.web;

import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.content.service.ArticleCommentService;
import com.nadoumi.content.service.ArticleService;
import com.nadoumi.content.web.request.ArticleRequest;
import com.nadoumi.content.web.response.StaffArticleResponse;
import com.nadoumi.content.web.response.StaffCommentResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.AuditActor;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Staff news authoring and comment moderation. */
@RestController
@RequestMapping("/api/staff/news")
public class StaffArticleController {

    private final ArticleService articles;
    private final ArticleCommentService comments;

    public StaffArticleController(ArticleService articles, ArticleCommentService comments) {
        this.articles = articles;
        this.comments = comments;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:article:list')")
    public PageResponse<StaffArticleResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return articles.staffList(q, language, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:article:view')")
    public StaffArticleResponse get(@PathVariable UUID id) {
        return articles.staffGet(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:article:create')")
    @Log(title = "News", businessType = BusinessType.INSERT)
    public StaffArticleResponse create(@Valid @RequestBody ArticleRequest req) {
        return articles.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:article:edit')")
    @Log(title = "News", businessType = BusinessType.UPDATE)
    public StaffArticleResponse update(@PathVariable UUID id, @Valid @RequestBody ArticleRequest req) {
        return articles.update(id, req);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermi('nad:article:publish')")
    @Log(title = "News", businessType = BusinessType.UPDATE)
    public StaffArticleResponse publish(@PathVariable UUID id) {
        return articles.publish(id);
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("@ss.hasPermi('nad:article:publish')")
    @Log(title = "News", businessType = BusinessType.UPDATE)
    public StaffArticleResponse unpublish(@PathVariable UUID id) {
        return articles.unpublish(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:article:remove')")
    @Log(title = "News", businessType = BusinessType.DELETE)
    public void delete(@PathVariable UUID id) {
        articles.delete(id);
    }

    @PostMapping("/{id}/cover")
    @PreAuthorize("@ss.hasPermi('nad:article:edit')")
    @Log(title = "News media", businessType = BusinessType.UPDATE)
    public MediaUploadResult uploadCover(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return articles.uploadCover(id, file);
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("@ss.hasPermi('nad:article:edit')")
    @Log(title = "News media", businessType = BusinessType.UPDATE)
    public MediaUploadResult uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return articles.uploadBodyImage(id, file);
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("@ss.hasPermi('nad:article:view')")
    public List<StaffCommentResponse> comments(@PathVariable UUID id) {
        return comments.staffList(id);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:article:comment:remove')")
    @Log(title = "News comment", businessType = BusinessType.DELETE)
    public void deleteComment(@PathVariable Long commentId) {
        comments.staffDelete(commentId, AuditActor.userId());
    }
}
