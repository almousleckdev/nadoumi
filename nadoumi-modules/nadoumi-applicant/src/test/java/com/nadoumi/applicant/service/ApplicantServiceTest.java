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
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.access.NadoumiAccessServiceImpl;
import com.nadoumi.identity.exception.NadForbiddenException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.ruoyi.framework.web.service.PermissionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ApplicantServiceTest {

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final NadoumiAccessServiceImpl access = mock(NadoumiAccessServiceImpl.class);
    private final UserApplicantAccessService grants = mock(UserApplicantAccessService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PermissionService rbac = mock(PermissionService.class);
    private final MediaGateway media = mock(MediaGateway.class);

    private final ApplicantService service =
            new ApplicantService(mapper, access, grants, caller, rbac, media);

    private static MultipartFile photo() {
        return new MockMultipartFile("file", "me.jpg", "image/jpeg", new byte[] { 1, 2, 3, 4 });
    }

    private static MediaAccessLogContext ctx() {
        return new MediaAccessLogContext(1L, null, null, null, "10.0.0.1", "junit");
    }

    @Test
    void applicantPhotoUploadStoresMediaId() {
        when(access.canAccessApplicant(5L, "EDIT_PROFILE")).thenReturn(true);
        when(caller.requireUserId()).thenReturn(1L);
        when(media.upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(new MediaUploadResult(77L, null));

        long mediaId = service.uploadPhoto(5L, photo());

        assertThat(mediaId).isEqualTo(77L);
        verify(mapper).updatePhotoMediaId(5L, 77L);
    }

    @Test
    void photoUrlRequiresViewProfile_grantHolderGetsASignedUrl() {
        Applicant a = new Applicant();
        a.setId(5L);
        a.setPhotoMediaId(9L);
        when(mapper.findById(5L)).thenReturn(a);
        when(access.canAccessApplicant(5L, "VIEW_PROFILE")).thenReturn(true);
        SignedUrl signed = new SignedUrl("https://res.cloudinary.com/x/s.jpg?sig=abc",
                Instant.now().plusSeconds(120));
        when(media.issueSignedUrl(eq(9L), any())).thenReturn(signed);

        SignedUrl result = service.photoUrl(5L, ctx());

        assertThat(result).isSameAs(signed);
        verify(media).issueSignedUrl(eq(9L), any());
        verify(media, never()).denyAndLog(anyLong(), any(), any());
    }

    @Test
    void photoUrlRequiresViewProfile_noGrantIsLoggedAndForbidden() {
        Applicant a = new Applicant();
        a.setId(5L);
        a.setPhotoMediaId(9L);
        when(mapper.findById(5L)).thenReturn(a);
        when(access.canAccessApplicant(5L, "VIEW_PROFILE")).thenReturn(false);

        assertThatThrownBy(() -> service.photoUrl(5L, ctx()))
                .isInstanceOf(NadForbiddenException.class);

        verify(media).denyAndLog(eq(9L), any(), eq("NO_APPLICANT_GRANT"));
        verify(media, never()).issueSignedUrl(anyLong(), any());
    }

    @Test
    void photoUrlReturnsNotFoundWhenTheApplicantHasNoPhoto() {
        Applicant a = new Applicant();
        a.setId(5L);
        when(mapper.findById(5L)).thenReturn(a);
        when(access.canAccessApplicant(5L, "VIEW_PROFILE")).thenReturn(true);

        assertThatThrownBy(() -> service.photoUrl(5L, ctx()))
                .isInstanceOf(NadNotFoundException.class);
        verify(media, never()).issueSignedUrl(anyLong(), any());
    }
}
