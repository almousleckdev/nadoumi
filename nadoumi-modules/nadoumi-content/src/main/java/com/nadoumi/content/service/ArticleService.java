package com.nadoumi.content.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.MediaUrls;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.common.text.Slugs;
import com.nadoumi.common.text.Texts;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.content.domain.Article;
import com.nadoumi.content.domain.enums.ArticleStatus;
import com.nadoumi.content.mapper.ArticleMapper;
import com.nadoumi.content.mapper.ArticleSearch;
import com.nadoumi.content.web.request.ArticleRequest;
import com.nadoumi.content.web.response.ArticleSummary;
import com.nadoumi.content.web.response.PublicArticleDetail;
import com.nadoumi.content.web.response.StaffArticleResponse;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.AuditActor;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Article authoring (staff) and the published read side (public). */
@Service
public class ArticleService {

    static final String DEFAULT_LANGUAGE = "en";
    static final int MAX_BODY_IMAGES = 50;
    static final int MAX_RELATED = 12;
    /** How many of the newest same-language articles related ones are ranked from. */
    static final int RELATED_POOL = 50;

    private final ArticleMapper mapper;
    private final MediaGateway media;
    private final ArticleCommentService comments;
    private final OutboxWriter outbox;

    public ArticleService(ArticleMapper mapper, MediaGateway media, ArticleCommentService comments,
            OutboxWriter outbox) {
        this.mapper = mapper;
        this.media = media;
        this.comments = comments;
        this.outbox = outbox;
    }

    // ---- staff ----

    @Transactional(readOnly = true)
    public PageResponse<StaffArticleResponse> staffList(String q, String language, ArticleStatus status,
            int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Article> rows = mapper.search(ArticleSearch.staff(Texts.blankToNull(q), Texts.blankToNull(language), status));
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(this::toStaff).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public StaffArticleResponse staffGet(UUID publicId) {
        return toStaff(loadPublic(publicId));
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffArticleResponse create(ArticleRequest req) {
        Article a = new Article();
        apply(a, req);
        a.setSlug(Slugs.unique(slugOf(req.title()), null, mapper::findIdBySlug));
        a.setStatus(ArticleStatus.DRAFT);
        a.setAuthorId(AuditActor.userId());
        a.setCreateBy(AuditActor.username());
        mapper.insert(a);
        return toStaff(load(a.getId()));
    }

    /** The slug is fixed at creation so published URLs never change when the title is edited. */
    @Transactional(rollbackFor = Exception.class)
    public StaffArticleResponse update(UUID publicId, ArticleRequest req) {
        Article a = loadPublic(publicId);
        apply(a, req);
        a.setUpdateBy(AuditActor.username());
        mapper.update(a);
        return toStaff(load(a.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffArticleResponse publish(UUID publicId) {
        Article a = loadPublic(publicId);
        long id = a.getId();
        if (a.getCoverMediaId() == null) {
            throw new NadBadRequestException("upload a cover image before publishing");
        }
        if (a.getBodyMd() == null || a.getBodyMd().isBlank()) {
            throw new NadBadRequestException("write the article body before publishing");
        }
        mapper.updateStatus(id, ArticleStatus.PUBLISHED, AuditActor.username());
        // students are told once; re-publishing after an unpublish must not announce it again
        if (a.getPublishedAt() == null) {
            emitPublished(a);
        }
        return toStaff(load(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffArticleResponse unpublish(UUID publicId) {
        Article a = loadPublic(publicId);
        long id = a.getId();
        if (a.getStatus() != ArticleStatus.PUBLISHED) {
            throw new NadBadRequestException("only a published article can be unpublished");
        }
        mapper.updateStatus(id, ArticleStatus.UNPUBLISHED, AuditActor.username());
        return toStaff(load(id));
    }

    /** Announce a newly public article to students (safe scalars only). */
    private void emitPublished(Article a) {
        JSONObject payload = new JSONObject();
        payload.put("articleId", a.getId());
        payload.put("articleTitle", a.getTitle());
        payload.put("articleSubtitle", a.getSubtitle() == null ? "" : a.getSubtitle());
        payload.put("articleSlug", a.getSlug());
        outbox.write("article", a.getId(), OutboxEventTypes.ARTICLE_PUBLISHED, payload.toJSONString());
    }

    /** A published article must be unpublished first, so a live page is never deleted by accident. */
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID publicId) {
        Article a = loadPublic(publicId);
        if (a.getStatus() == ArticleStatus.PUBLISHED) {
            throw new NadBadRequestException("unpublish the article before deleting it");
        }
        mapper.delete(a.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public MediaUploadResult uploadCover(UUID publicId, MultipartFile file) {
        long id = loadPublic(publicId).getId();
        MediaUploadResult result = uploadFor(id, file, MediaCategory.ARTICLE_COVER);
        mapper.updateCoverMediaId(id, result.mediaId(), AuditActor.username());
        return result;
    }

    /** Uploads an image for the article body; the caller embeds the returned URL in the Markdown. */
    @Transactional(rollbackFor = Exception.class)
    public MediaUploadResult uploadBodyImage(UUID publicId, MultipartFile file) {
        long id = loadPublic(publicId).getId();
        int existing = mapper.countImages(id);
        if (existing >= MAX_BODY_IMAGES) {
            throw new NadBadRequestException("at most " + MAX_BODY_IMAGES + " images per article");
        }
        MediaUploadResult result = uploadFor(id, file, MediaCategory.ARTICLE_IMAGE);
        mapper.insertImage(id, result.mediaId(), existing, AuditActor.username());
        return result;
    }

    // ---- public (published only) ----

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummary> publicList(String q, String language, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Article> rows = mapper.search(ArticleSearch.publicFeed(Texts.blankToNull(q), Texts.blankToNull(language)));
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(a -> ArticleSummary.of(a, coverUrl(a))).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public PublicArticleDetail publicGet(String slug) {
        Article a = publishedBySlug(slug);
        return new PublicArticleDetail(ArticleSummary.of(a, coverUrl(a)), a.getBodyMd(),
                comments.publicThread(a.getId()));
    }

    /** Articles worth reading next, from the same language, best word overlap first (see {@link RelatedArticles}). */
    @Transactional(readOnly = true)
    public List<ArticleSummary> related(String slug, int limit) {
        Article current = publishedBySlug(slug);
        int size = Math.min(Math.max(limit, 1), MAX_RELATED);
        List<Article> pool = mapper.findRelatedCandidates(current.getId(), current.getLanguage(), RELATED_POOL);
        return RelatedArticles.rank(current, pool, size).stream().map(a -> ArticleSummary.of(a, coverUrl(a))).toList();
    }

    // ---- helpers ----

    /** A draft, unpublished or unknown slug is indistinguishable from a missing article. */
    private Article publishedBySlug(String slug) {
        Article a = mapper.findBySlug(slug);
        if (a == null || a.getStatus() != ArticleStatus.PUBLISHED) {
            throw new NadNotFoundException("article not found");
        }
        return a;
    }

    private Article loadPublic(UUID publicId) {
        Article a = mapper.findByPublicId(publicId.toString());
        if (a == null) {
            throw new NadNotFoundException("article not found");
        }
        return a;
    }

    private Article load(long id) {
        Article a = mapper.findById(id);
        if (a == null) {
            throw new NadNotFoundException("article not found");
        }
        return a;
    }

    private static void apply(Article a, ArticleRequest req) {
        a.setTitle(req.title().trim());
        a.setSubtitle(Texts.blankToNull(req.subtitle()));
        a.setBodyMd(req.bodyMd());
        a.setLanguage(req.language() == null ? DEFAULT_LANGUAGE : req.language());
    }

    private static String slugOf(String title) {
        try {
            return Slugs.slugify(title);
        }
        catch (IllegalArgumentException e) {
            // titles with no ASCII letters (e.g. Chinese) get a neutral base; uniqueness adds the suffix
            return "article";
        }
    }

    private String coverUrl(Article a) {
        return MediaUrls.resolve(media, a.getCoverMediaId(), null);
    }

    private StaffArticleResponse toStaff(Article a) {
        return StaffArticleResponse.of(a, coverUrl(a));
    }

    private MediaUploadResult uploadFor(long articleId, MultipartFile file, MediaCategory category) {
        return media.upload(file::getInputStream, file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), category, null,
                    new MediaOwnerRef(MediaOwnerKind.ARTICLE, articleId), AuditActor.userId());
    }
}
