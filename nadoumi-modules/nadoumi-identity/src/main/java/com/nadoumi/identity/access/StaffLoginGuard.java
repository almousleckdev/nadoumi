package com.nadoumi.identity.access;

import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Component;

/**
 * Keeps external users ({@code user_type} 10/20/30) off the staff {@code /login}
 * endpoint (API_DESIGN §4.1). An unknown username is left to the normal auth flow.
 */
@Component
public class StaffLoginGuard {

    private final NadIdentityMapper identityMapper;

    public StaffLoginGuard(NadIdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    public void assertStaffLogin(String username) {
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
