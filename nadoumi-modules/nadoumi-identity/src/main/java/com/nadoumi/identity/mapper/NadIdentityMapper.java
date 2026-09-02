package com.nadoumi.identity.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Reads / writes the otherwise-dormant {@code sys_user.user_type} column. Nadoumi is
 * its first consumer, so this stays out of the RuoYi {@code SysUserMapper}.
 */
public interface NadIdentityMapper {

    String selectUserType(@Param("userId") Long userId);

    int updateUserType(@Param("userId") Long userId, @Param("userType") String userType);

    Long selectUserIdByUserName(@Param("userName") String userName);

    Long selectUserIdByEmail(@Param("email") String email);
}
