package com.nadoumi.identity.access;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.service.LoginUsernameGuard;
import org.junit.jupiter.api.Test;

/**
 * StaffLoginGuard is registered as the {@link LoginUsernameGuard} bean that
 * RuoYi's {@code /login} calls before checking credentials — see
 * SysLoginService.
 */
class StaffLoginGuardTest {

    private final NadIdentityMapper mapper = mock(NadIdentityMapper.class);
    private final StaffLoginGuard guard = new StaffLoginGuard(mapper);

    @Test
    void implementsTheRuoYiExtensionPoint() {
        assertThatCode(() -> {
            LoginUsernameGuard asExtensionPoint = guard;
            asExtensionPoint.assertLoginAllowed("anyone");
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnExternalUser() {
        when(mapper.selectUserIdByUserName("student1")).thenReturn(42L);
        when(mapper.selectUserType(42L)).thenReturn("10");

        assertThatThrownBy(() -> guard.assertLoginAllowed("student1"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void allowsAStaffUser() {
        when(mapper.selectUserIdByUserName("staff1")).thenReturn(7L);
        when(mapper.selectUserType(7L)).thenReturn(CurrentCaller.STAFF);

        assertThatCode(() -> guard.assertLoginAllowed("staff1")).doesNotThrowAnyException();
    }

    @Test
    void allowsAnUnknownUsername_leavingItToTheNormalAuthFlow() {
        when(mapper.selectUserIdByUserName("nobody")).thenReturn(null);

        assertThatCode(() -> guard.assertLoginAllowed("nobody")).doesNotThrowAnyException();
    }
}
