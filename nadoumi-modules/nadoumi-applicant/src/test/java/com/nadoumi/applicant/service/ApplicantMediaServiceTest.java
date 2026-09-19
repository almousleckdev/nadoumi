package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.identity.access.CurrentCaller;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

class ApplicantMediaServiceTest {

    private static final long ID = 5L;

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final ApplicantMediaService service = new ApplicantMediaService(mapper, new ApplicantAccessGuard(access), media, caller);

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "scan.jpg", "image/jpeg", new byte[] { 1, 2, 3, 4 });
    }

    private static MediaAccessLogContext ctx() {
        return new MediaAccessLogContext(1L, null, null, null, "10.0.0.1", "junit");
    }

    private static Applicant applicantWith(ApplicantMediaKind kind, Long mediaId) {
        Applicant a = new Applicant();
        a.setId(ID);
        if (kind == ApplicantMediaKind.PHOTO) {
            a.setPhotoMediaId(mediaId);
        }
        else {
            a.setPassportMediaId(mediaId);
        }
        return a;
    }

    @ParameterizedTest
    @EnumSource(ApplicantMediaKind.class)
    void shouldStoreTheMediaIdAndUseTheKindsCategory_whenUploading(ApplicantMediaKind kind) {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        when(caller.requireUserId()).thenReturn(1L);
        when(media.upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(new MediaUploadResult(77L, null));

        long mediaId = service.upload(ID, kind, file());

        assertThat(mediaId).isEqualTo(77L);
        verify(media).upload(any(), any(), any(), anyLong(), eq(kind.category()), any(), any(), anyLong());
        if (kind == ApplicantMediaKind.PHOTO) {
            verify(mapper).updatePhotoMediaId(ID, 77L);
        }
        else {
            verify(mapper).updatePassportMediaId(ID, 77L);
        }
    }

    @Test
    void shouldStoreThePassportInItsOwnCategory() {
        assertThat(ApplicantMediaKind.PASSPORT.category()).isEqualTo(MediaCategory.APPLICANT_PASSPORT);
        assertThat(ApplicantMediaKind.PHOTO.category()).isEqualTo(MediaCategory.APPLICANT_PHOTO);
    }

    @ParameterizedTest
    @EnumSource(ApplicantMediaKind.class)
    void shouldRefuseUpload_whenCallerCannotEditThisApplicant(ApplicantMediaKind kind) {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(false);

        assertThatThrownBy(() -> service.upload(ID, kind, file())).isInstanceOf(AccessDeniedException.class);
        verify(media, never()).upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong());
    }

    @ParameterizedTest
    @EnumSource(ApplicantMediaKind.class)
    void shouldIssueASignedUrl_whenCallerHoldsViewProfile(ApplicantMediaKind kind) {
        when(mapper.findById(ID)).thenReturn(applicantWith(kind, 9L));
        when(access.canAccessApplicant(ID, "VIEW_PROFILE")).thenReturn(true);
        SignedUrl signed = new SignedUrl("https://res.cloudinary.com/x/s.jpg?sig=abc", Instant.now().plusSeconds(120));
        when(media.issueSignedUrl(eq(9L), any())).thenReturn(signed);

        assertThat(service.signedUrl(ID, kind, ctx())).isSameAs(signed);
        verify(media, never()).denyAndLog(anyLong(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(ApplicantMediaKind.class)
    void shouldLogAndForbid_whenCallerHasNoGrant(ApplicantMediaKind kind) {
        when(mapper.findById(ID)).thenReturn(applicantWith(kind, 9L));
        when(access.canAccessApplicant(ID, "VIEW_PROFILE")).thenReturn(false);

        assertThatThrownBy(() -> service.signedUrl(ID, kind, ctx())).isInstanceOf(NadForbiddenException.class);

        verify(media).denyAndLog(eq(9L), any(), eq("NO_APPLICANT_GRANT"));
        verify(media, never()).issueSignedUrl(anyLong(), any());
    }

    @ParameterizedTest
    @EnumSource(ApplicantMediaKind.class)
    void shouldReturnNotFound_whenNothingHasBeenUploaded(ApplicantMediaKind kind) {
        when(mapper.findById(ID)).thenReturn(applicantWith(kind, null));
        when(access.canAccessApplicant(ID, "VIEW_PROFILE")).thenReturn(true);

        assertThatThrownBy(() -> service.signedUrl(ID, kind, ctx())).isInstanceOf(NadNotFoundException.class);
        verify(media, never()).issueSignedUrl(anyLong(), any());
    }
}
