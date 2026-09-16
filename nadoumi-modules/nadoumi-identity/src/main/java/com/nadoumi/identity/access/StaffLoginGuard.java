package com.nadoumi.identity.access;

import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.service.LoginUsernameGuard;
import org.springframework.stereotype.Component;

/**
 * Keeps external users ({@code user_type} 10/20/30) off the staff {@code /login}
 * endpoint (API_DESIGN §4.1). An unknown username is left to the normal auth flow.
 * Registered as the {@link LoginUsernameGuard} bean, replacing RuoYi's no-op
 * default — {@code SysLoginController} carries no reference to this class.
 */
@Component
public class StaffLoginGuard implements LoginUsernameGuard {

    private final NadIdentityMapper identityMapper;

    public StaffLoginGuard(NadIdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    @Override
    public void assertLoginAllowed(String username) {
        Long userId = identityMapper.selectUserIdByUserName(username);
        if (userId == null) {
            return;
        }
        String userType = identityMapper.selectUserType(userId);
        if (userType != null && !CurrentCaller.STAFF.equals(userType)) {
            throw new ServiceException("External users must sign in at /api/student/login");
        }
    }
}
