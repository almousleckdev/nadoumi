package com.nadoumi.identity.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A staff member's profile photo lives in media storage as a public asset and {@code sys_user.avatar}
 * holds its absolute URL. That is what lets the photo appear on the public site next to their articles;
 * the stock RuoYi behaviour (a file on the server's disk, a path relative to the backend) cannot be
 * reached from the public site and is lost whenever the container is rebuilt.
 */
@Service
public class StaffAvatarService {

    private final MediaGateway media;
    private final ISysUserService users;

    public StaffAvatarService(MediaGateway media, ISysUserService users) {
        this.media = media;
        this.users = users;
    }

    /** Stores the image and points the user's avatar at it. Returns the new public URL. */
    @Transactional(rollbackFor = Exception.class)
    public String replace(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NadBadRequestException("choose an image to upload");
        }
        MediaUploadResult stored = media.upload(file::getInputStream, file.getOriginalFilename(),
                file.getContentType(), file.getSize(), MediaCategory.STAFF_AVATAR, null,
                new MediaOwnerRef(MediaOwnerKind.USER, userId), userId);
        if (stored.url() == null) {
            throw new IllegalStateException("staff avatar assets must be public but media " + stored.mediaId() + " has no url");
        }
        if (!users.updateUserAvatar(userId, stored.url())) {
            throw new NadBadRequestException("the photo could not be saved, please try again");
        }
        return stored.url();
    }
}
