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

    /** Sets the sign-in email to a new, already OTP-verified address in one statement. */
    int changeEmail(@Param("userId") Long userId, @Param("email") String email);

    int touchPwdUpdateDate(@Param("userId") Long userId);

    /** The sign-in email of a live student account, or null. */
    String selectStudentEmail(@Param("userId") Long userId);

    int removeStudentRoles(@Param("userId") Long userId);

    int removeStudentPosts(@Param("userId") Long userId);

    /** Removes the account row itself. Fails on a foreign key while other rows still point at the user. */
    int removeStudentRow(@Param("userId") Long userId);

    /**
     * Soft-deletes a student account and frees its sign-in identifiers so they can be registered again.
     * Returns 0 when the user is not a live student.
     */
    int softDeleteStudent(@Param("userId") Long userId, @Param("updateBy") String updateBy);
}
