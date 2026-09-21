package com.nadoumi.communication.mapper;

import org.apache.ibatis.annotations.Param;

/** The one {@code sys_user} read this module needs: a display name for a sender. */
public interface CommunicationUserMapper {

    /** {@code nick_name}, falling back to {@code user_name}; null when the user doesn't exist. */
    String findDisplayName(@Param("userId") long userId);
}
