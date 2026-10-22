package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantPersonal;
import com.nadoumi.applicant.mapper.FamilyMapper;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.identity.access.CurrentCaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The applicant's handwritten signature: given once (drawn or uploaded), re-encoded to a small PNG, stored as a
 * SENSITIVE media asset and printed on the application form. Callers are checked against the applicant grant; the
 * two {@code ...OnFile}/{@code png} reads without a caller check are for server code that prints or gates the form.
 */
@Service
public class SignatureService {

    /** What a student sees about their own signature; never a URL. */
    public record Status(boolean signed, String signedAt) {
    }

    private static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;

    private final FamilyMapper mapper;
    private final ApplicantAccessGuard guard;
    private final MediaGateway media;
    private final CurrentCaller caller;

    public SignatureService(FamilyMapper mapper, ApplicantAccessGuard guard, MediaGateway media, CurrentCaller caller) {
        this.mapper = mapper;
        this.guard = guard;
        this.media = media;
        this.caller = caller;
    }

    @Transactional(rollbackFor = Exception.class)
    public Status save(long applicantId, MultipartFile file) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (file == null || file.isEmpty() || file.getSize() > MAX_UPLOAD_BYTES) {
            throw new NadBadRequestException("the signature image is missing or too large");
        }
        byte[] png;
        try {
            png = SignatureNormalizer.toPng(file.getBytes())
                    .orElseThrow(() -> new NadBadRequestException("draw or upload a clear signature"));
        }
        catch (IOException e) {
            throw new NadBadRequestException("the signature image could not be read");
        }
        long userId = caller.requireUserId();
        long mediaId = media.upload(new ByteArrayInputStream(png), "signature.png", "image/png", png.length,
                MediaCategory.APPLICANT_SIGNATURE, null, new MediaOwnerRef(MediaOwnerKind.APPLICANT, applicantId), userId)
                .mediaId();
        ApplicantPersonal previous = mapper.findPersonal(applicantId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        mapper.upsertSignature(applicantId, mediaId, now);
        if (previous != null && previous.getSignatureMediaId() != null) {
            media.softDelete(previous.getSignatureMediaId(), userId);
        }
        return new Status(true, now.toString());
    }

    @Transactional(readOnly = true)
    public Status status(long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        ApplicantPersonal personal = mapper.findPersonal(applicantId);
        boolean signed = personal != null && personal.getSignatureMediaId() != null;
        return new Status(signed, signed && personal.getSignatureAt() != null ? personal.getSignatureAt().toString() : null);
    }

    /** The owner's view of their own signature (a PNG stream through the backend, access-logged). */
    @Transactional(readOnly = true)
    public ProxyStream open(long applicantId, MediaAccessLogContext context) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        ApplicantPersonal personal = mapper.findPersonal(applicantId);
        if (personal == null || personal.getSignatureMediaId() == null) {
            throw new NadNotFoundException("no signature on file");
        }
        return media.openProxyStream(personal.getSignatureMediaId(), context);
    }

    /** No caller check: for the submit gate. */
    @Transactional(readOnly = true)
    public boolean isOnFile(long applicantId) {
        ApplicantPersonal personal = mapper.findPersonal(applicantId);
        return personal != null && personal.getSignatureMediaId() != null;
    }

    /** No caller check: for the server job that prints the form. */
    @Transactional(readOnly = true)
    public Optional<SignatureOnFile> onFile(long applicantId) {
        ApplicantPersonal personal = mapper.findPersonal(applicantId);
        if (personal == null || personal.getSignatureMediaId() == null) {
            return Optional.empty();
        }
        try (var stream = media.openProxyStream(personal.getSignatureMediaId(),
                new MediaAccessLogContext(0L, applicantId, null, null, "system", "system")).body()) {
            byte[] png = stream.readNBytes(MAX_UPLOAD_BYTES);
            return Optional.of(new SignatureOnFile(png, personal.getSignatureAt() == null ? null : personal.getSignatureAt().toLocalDate()));
        }
        catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** The signature image and the day it was given. */
    public record SignatureOnFile(byte[] png, java.time.LocalDate signedOn) {
    }
}
