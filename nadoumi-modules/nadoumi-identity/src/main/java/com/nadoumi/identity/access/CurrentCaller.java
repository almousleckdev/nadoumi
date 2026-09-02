package com.nadoumi.identity.access;

import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * The authenticated caller for the current request, and whether they are Nadoumi
 * staff ({@code user_type='00'}) or an external user ({@code 10/20/30}). The
 * {@code user_type} is read live from {@code sys_user} — it is never taken from the
 * Redis-cached {@code LoginUser}.
 */
@Component
public class CurrentCaller {

    public static final String STAFF = "00";

    private final NadIdentityMapper identityMapper;

    public CurrentCaller(NadIdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    public Long userIdOrNull() {
        LoginUser user = loginUserOrNull();
        return user == null ? null : user.getUserId();
    }

    public Long requireUserId() {
        Long id = userIdOrNull();
        if (id == null) {
            throw new IllegalStateException("no authenticated caller");
        }
        return id;
    }

    public String userType() {
        String type = identityMapper.selectUserType(requireUserId());
        return type == null ? STAFF : type;
    }

    public boolean isStaff() {
        return STAFF.equals(userType());
    }

    public boolean isExternal() {
        return !isStaff();
    }

    private LoginUser loginUserOrNull() {
        try {
            return SecurityUtils.getLoginUser();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
