package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.identity.access.CurrentCaller;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The applicant's PROTECTED files (profile photo, passport scan). One code path for
 * both: upload needs {@code EDIT_PROFILE}; reading needs {@code VIEW_PROFILE} and is
 * served only through a short-lived signed URL, with a logged denial otherwise.
 */
@Service
public class ApplicantMediaService {

    private final ApplicantMapper mapper;
    private final ApplicantAccessGuard guard;
    private final MediaGateway media;
    private final CurrentCaller caller;

    public ApplicantMediaService(ApplicantMapper mapper, ApplicantAccessGuard guard, MediaGateway media,
            CurrentCaller caller) {
        this.mapper = mapper;
        this.guard = guard;
        this.media = media;
        this.caller = caller;
    }

    /** Stores the file and points the applicant at it. Returns the media id (never a URL). */
    @Transactional(rollbackFor = Exception.class)
    public long upload(long applicantId, ApplicantMediaKind kind, MultipartFile file) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        MediaUploadResult result;
        try {
            result = media.upload(file.getInputStream(), file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), kind.category(), null,
                    new MediaOwnerRef(MediaOwnerKind.APPLICANT, applicantId), currentUserId());
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read upload", e);
        }
        switch (kind) {
            case PHOTO -> mapper.updatePhotoMediaId(applicantId, result.mediaId());
            case PASSPORT -> mapper.updatePassportMediaId(applicantId, result.mediaId());
        }
        return result.mediaId();
    }

    /**
     * A short-TTL signed URL, with a {@code nad_media_access_log} entry. A caller without
     * {@code VIEW_PROFILE} gets a logged denial and a 403.
     */
    public SignedUrl signedUrl(long applicantId, ApplicantMediaKind kind, MediaAccessLogContext ctx) {
        Applicant applicant = mapper.findById(applicantId);
        if (applicant == null) {
            throw new NadNotFoundException("applicant not found");
        }
        Long mediaId = kind.mediaIdOf(applicant);
        try {
            guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        }
        catch (AccessDeniedException e) {
            if (mediaId != null) {
                media.denyAndLog(mediaId, ctx, "NO_APPLICANT_GRANT");
            }
            throw new NadForbiddenException("missing VIEW_PROFILE on applicant " + applicantId);
        }
        if (mediaId == null) {
            throw new NadNotFoundException("applicant has no " + kind.name().toLowerCase());
        }
        return media.issueSignedUrl(mediaId, ctx);
    }

    private long currentUserId() {
        try {
            Long id = caller.requireUserId();
            return id == null ? 0L : id;
        }
        catch (RuntimeException e) {
            return 0L;
        }
    }
}
