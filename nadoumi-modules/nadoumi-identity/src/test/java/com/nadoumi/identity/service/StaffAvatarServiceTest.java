package com.nadoumi.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaUploadResult;
import com.ruoyi.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class StaffAvatarServiceTest {

    private static final long USER_ID = 3L;
    private static final String URL = "https://res.cloudinary.com/demo/staff/avatar/a.jpg";

    private final MediaGateway media = mock(MediaGateway.class);
    private final ISysUserService users = mock(ISysUserService.class);
    private final StaffAvatarService service = new StaffAvatarService(media, users);

    private static MockMultipartFile image() {
        return new MockMultipartFile("avatarfile", "me.png", "image/png", new byte[] { 1, 2, 3 });
    }

    private void givenStorageReturns(MediaUploadResult result) {
        when(media.upload(any(MediaGateway.UploadSource.class), any(), any(), anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(result);
    }

    @Test
    void shouldStoreThePhotoAsAPublicStaffAvatarAndSaveItsAbsoluteUrl() {
        givenStorageReturns(new MediaUploadResult(9L, URL));
        when(users.updateUserAvatar(USER_ID, URL)).thenReturn(true);

        String saved = service.replace(USER_ID, image());

        assertThat(saved).isEqualTo(URL);
        ArgumentCaptor<com.nadoumi.common.media.MediaOwnerRef> owner =
                ArgumentCaptor.forClass(com.nadoumi.common.media.MediaOwnerRef.class);
        verify(media).upload(any(MediaGateway.UploadSource.class), eq("me.png"), eq("image/png"), eq(3L),
                eq(MediaCategory.STAFF_AVATAR), eq(null), owner.capture(), eq(USER_ID));
        assertThat(owner.getValue().kind()).isEqualTo(MediaOwnerKind.USER);
        assertThat(owner.getValue().id()).isEqualTo(USER_ID);
        verify(users).updateUserAvatar(USER_ID, URL);
    }

    @Test
    void shouldRejectAnEmptyUpload_withoutTouchingStorage() {
        assertThatThrownBy(() -> service.replace(USER_ID, new MockMultipartFile("avatarfile", new byte[0])))
                .isInstanceOf(NadBadRequestException.class);
        verify(media, never()).upload(any(MediaGateway.UploadSource.class), any(), any(), anyLong(), any(), any(), any(), anyLong());
    }

    @Test
    void shouldFail_whenTheUserRowCouldNotBeUpdated() {
        givenStorageReturns(new MediaUploadResult(9L, URL));
        when(users.updateUserAvatar(USER_ID, URL)).thenReturn(false);

        assertThatThrownBy(() -> service.replace(USER_ID, image())).isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldNotAcceptAnAssetThatHasNoPublicUrl() {
        givenStorageReturns(new MediaUploadResult(9L, null));

        assertThatThrownBy(() -> service.replace(USER_ID, image())).isInstanceOf(IllegalStateException.class);
        verify(users, never()).updateUserAvatar(anyLong(), any());
    }
}
