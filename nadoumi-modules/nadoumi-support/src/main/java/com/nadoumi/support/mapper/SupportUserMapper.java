package com.nadoumi.support.mapper;

import org.apache.ibatis.annotations.Param;

/** The two {@code sys_user} reads this module needs: a display name and whether an assignee is staff. */
public interface SupportUserMapper {

    /** {@code nick_name}, falling back to {@code user_name}; null when the user doesn't exist. */
    String findDisplayName(@Param("userId") long userId);

    /** {@code user_type} ({@code '00'} = staff); null when the user doesn't exist. */
    String findUserType(@Param("userId") long userId);
}
