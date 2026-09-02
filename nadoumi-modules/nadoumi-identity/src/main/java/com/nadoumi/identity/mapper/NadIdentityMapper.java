package com.nadoumi.identity.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Reads / writes the otherwise-dormant {@code sys_user.user_type} column plus the
 * Revision 2 {@code email_verified} flag. Nadoumi is the first consumer, so this
 * stays out of the RuoYi {@code SysUserMapper}.
 */
public interface NadIdentityMapper {

    String selectUserType(@Param("userId") Long userId);

    int updateUserType(@Param("userId") Long userId, @Param("userType") String userType);

    Long selectUserIdByUserName(@Param("userName") String userName);

    Long selectUserIdByEmail(@Param("email") String email);

    /** Resolve a user by email restricted to one {@code user_type} (email-first login). */
    Long selectUserIdByEmailAndType(@Param("email") String email, @Param("userType") String userType);

    int markEmailVerified(@Param("userId") Long userId);

    int touchPwdUpdateDate(@Param("userId") Long userId);
}
