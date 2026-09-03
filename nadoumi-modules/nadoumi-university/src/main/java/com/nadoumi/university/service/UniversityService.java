package com.nadoumi.university.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.text.Slugs;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.UniversityGalleryImage;
import com.nadoumi.university.domain.UniversityHighlight;
import com.nadoumi.university.domain.UniversityRanking;
import com.nadoumi.university.domain.enums.PublishStatus;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.domain.enums.UniversityType;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.mapper.UniversitySearch;
import com.nadoumi.university.web.request.UniversityRequest;
import com.nadoumi.university.web.response.PublicUniversityResponse;
import com.nadoumi.university.web.response.UniversityResponse;
import com.ruoyi.common.utils.SecurityUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * University catalog. {@code (name, country)} is unique. The rankings and
 * highlights lists are edited whole: a save replaces them for that university
 * inside the same transaction as the scalar update.
 */
@Service
public class UniversityService {

    private static final int MAX_GALLERY_IMAGES = 6;

    private final UniversityMapper mapper;
    private final MediaGateway media;

    public UniversityService(UniversityMapper mapper, MediaGateway media) {
        this.mapper = mapper;
        this.media = media;
    }

    // ---- staff ----

    public PageResponse<UniversityResponse> list(String q, String country, String province, String city,
            UniversityType type, UniversityStatus status, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<University> rows = mapper.search(UniversitySearch.staff(q, country, province, city, type, status));
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(this::toResponse).toList(), page, size, total);
    }

    public UniversityResponse get(Long id) {
        return toResponse(loadWithChildren(id));
    }

    @Transactional
    public UniversityResponse create(UniversityRequest req) {
        University u = new University();
        apply(u, req);
        requireUniqueName(u.getName(), u.getCountry(), null);
        u.setSlug(uniqueSlug(u.getName(), null));
        u.setCreateBy(currentUser());
        mapper.insert(u);
        replaceChildren(u.getId(), req);
        return get(u.getId());
    }

    @Transactional
    public UniversityResponse update(Long id, UniversityRequest req) {
        University u = load(id);
        apply(u, req);
        requireUniqueName(u.getName(), u.getCountry(), id);
        u.setSlug(uniqueSlug(u.getName(), id));
        u.setUpdateBy(currentUser());
        mapper.update(u);
        replaceChildren(id, req);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.delete(id) == 0) {
            throw new NadNotFoundException("university not found");
        }
        // children go with the row (ON DELETE CASCADE)
    }

    // ---- media uploads (staff) ----

    @Transactional
    public MediaUploadResult uploadLogo(long id, MultipartFile file) {
        load(id);
        MediaUploadResult result = uploadFor(id, file, MediaCategory.UNIVERSITY_LOGO);
        mapper.updateLogoMediaId(id, result.mediaId());
        return result;
    }

    @Transactional
    public MediaUploadResult uploadBanner(long id, MultipartFile file) {
        load(id);
        MediaUploadResult result = uploadFor(id, file, MediaCategory.UNIVERSITY_BANNER);
        mapper.updateBannerMediaId(id, result.mediaId());
        return result;
    }

    @Transactional
    public MediaUploadResult uploadGalleryImage(long id, MultipartFile file) {
        load(id);
        List<UniversityGalleryImage> existing = mapper.findGallery(id);
        if (existing.size() >= MAX_GALLERY_IMAGES) {
            throw new NadBadRequestException("at most " + MAX_GALLERY_IMAGES + " gallery images");
        }
        MediaUploadResult result = uploadFor(id, file, MediaCategory.UNIVERSITY_GALLERY);
        mapper.insertGalleryImage(id,
                new UniversityGalleryImage(null, result.url(), result.mediaId(), null), existing.size());
        return result;
    }

    private MediaUploadResult uploadFor(long universityId, MultipartFile file, MediaCategory category) {
        try {
            return media.upload(file.getInputStream(), file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), category, null,
                    new MediaOwnerRef(MediaOwnerKind.UNIVERSITY, universityId), currentUserId());
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read upload", e);
        }
    }

    // ---- public (published + active only) ----

    public PageResponse<PublicUniversityResponse> publicList(String q, String country, String province,
            String city, UniversityType type, Boolean featured, Boolean recommended, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<University> rows = mapper.search(
                UniversitySearch.publicCatalog(q, country, province, city, type, featured, recommended));
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(
                rows.stream().map(u -> PublicUniversityResponse.of(toResponse(u))).toList(), page, size, total);
    }

    public PublicUniversityResponse publicGet(String idOrSlug) {
        University u = loadWithChildren(resolveId(idOrSlug));
        if (u.getStatus() != UniversityStatus.ACTIVE || u.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new NadNotFoundException("university not found");
        }
        return PublicUniversityResponse.of(toResponse(u));
    }

    /** Resolve the {@code {idOrSlug}} path segment (public routes are slug-first). */
    public Long resolveId(String idOrSlug) {
        if (idOrSlug != null && idOrSlug.chars().allMatch(Character::isDigit)) {
            return Long.valueOf(idOrSlug);
        }
        Long id = mapper.findIdBySlug(idOrSlug);
        if (id == null) {
            throw new NadNotFoundException("university not found");
        }
        return id;
    }

    // ---- internals ----

    private University load(Long id) {
        University u = mapper.findById(id);
        if (u == null) {
            throw new NadNotFoundException("university not found");
        }
        return u;
    }

    private University loadWithChildren(Long id) {
        University u = load(id);
        u.setRankings(mapper.findRankings(id));
        u.setHighlights(mapper.findHighlights(id));
        u.setGallery(mapper.findGallery(id));
        return u;
    }

    private void replaceChildren(Long universityId, UniversityRequest req) {
        mapper.deleteRankings(universityId);
        if (req.rankings() != null) {
            for (UniversityRequest.RankingInput in : req.rankings()) {
                UniversityRanking r = new UniversityRanking();
                r.setUniversityId(universityId);
                r.setSource(in.source().trim());
                r.setRankPosition(in.rankPosition());
                r.setRankYear(in.rankYear() == null ? null : in.rankYear().intValue());
                r.setNote(blankToNull(in.note()));
                mapper.insertRanking(r);
            }
        }
        mapper.deleteHighlights(universityId);
        if (req.highlights() != null) {
            int order = 0;
            for (UniversityRequest.HighlightInput in : req.highlights()) {
                UniversityHighlight h = new UniversityHighlight();
                h.setUniversityId(universityId);
                h.setKind(in.kind());
                h.setSortOrder(order++);
                h.setText(in.text().trim());
                mapper.insertHighlight(h);
            }
        }
        mapper.deleteGallery(universityId);
        if (req.gallery() != null) {
            int order = 0;
            for (UniversityRequest.GalleryInput in : req.gallery()) {
                if (in.imageUrl() == null || in.imageUrl().isBlank()) {
                    continue;
                }
                mapper.insertGalleryImage(universityId,
                        new UniversityGalleryImage(null, in.imageUrl().trim(), in.mediaId(),
                                blankToNull(in.caption())),
                        order++);
            }
        }
    }

    private String uniqueSlug(String name, Long selfId) {
        return Slugs.unique(Slugs.slugify(name), selfId, mapper::findIdBySlug);
    }

    private void requireUniqueName(String name, String country, Long selfId) {
        Long existing = mapper.findIdByNameAndCountry(name, country);
        if (existing != null && !existing.equals(selfId)) {
            throw new NadBadRequestException("a university with this name already exists in " + country);
        }
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException e) {
            return "system";
        }
    }

    private static long currentUserId() {
        try {
            Long id = SecurityUtils.getUserId();
            return id == null ? 0L : id;
        }
        catch (RuntimeException e) {
            return 0L;
        }
    }

    /** Media id wins; the legacy {@code *_image_url} string is the deprecation-window fallback. */
    private UniversityResponse toResponse(University u) {
        return UniversityResponse.of(u,
                resolveUrl(u.getLogoMediaId(), u.getLogoImageUrl()),
                resolveUrl(u.getBannerMediaId(), u.getCoverImageUrl()),
                g -> resolveUrl(g.mediaId(), g.imageUrl()));
    }

    private String resolveUrl(Long mediaId, String legacy) {
        if (mediaId == null) {
            return legacy;
        }
        try {
            return media.publicUrl(mediaId);
        }
        catch (RuntimeException e) {
            return legacy;
        }
    }

    private static void apply(University u, UniversityRequest req) {
        u.setName(req.name().trim());
        u.setNameCn(blankToNull(req.nameCn()));
        u.setCountry(req.country().toUpperCase());
        u.setType(req.type());
        u.setCity(blankToNull(req.city()));
        u.setProvince(blankToNull(req.province()));
        u.setFoundedYear(req.foundedYear());
        u.setTotalStudents(req.totalStudents());
        u.setInternationalStudents(req.internationalStudents());
        u.setFacultyCount(req.facultyCount());
        u.setWebsite(blankToNull(req.website()));
        u.setRankingTier(blankToNull(req.rankingTier()));
        u.setIntroduction(blankToNull(req.introduction()));
        u.setHistory(blankToNull(req.history()));
        u.setCampusInfo(blankToNull(req.campusInfo()));
        u.setAccommodationInfo(blankToNull(req.accommodationInfo()));
        u.setNearbyInfo(blankToNull(req.nearbyInfo()));
        u.setAdmissionsEmail(blankToNull(req.admissionsEmail()));
        u.setOfficePhone(blankToNull(req.officePhone()));
        u.setLogoImageUrl(blankToNull(req.logoImageUrl()));
        u.setCoverImageUrl(blankToNull(req.coverImageUrl()));
        // Media ids are primarily set through the dedicated upload endpoints; only
        // overwrite from the request when the client actually sent a value.
        if (req.logoMediaId() != null) {
            u.setLogoMediaId(req.logoMediaId());
        }
        if (req.bannerMediaId() != null) {
            u.setBannerMediaId(req.bannerMediaId());
        }
        u.setRecommended(Boolean.TRUE.equals(req.recommended()));
        u.setFeatured(Boolean.TRUE.equals(req.featured()));
        u.setStatus(req.status());
        u.setPublishStatus(req.publishStatus());
        u.setRemark(blankToNull(req.remark()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
