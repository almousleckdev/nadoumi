package com.nadoumi.hr.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.hr.mapper.EmployeeMapper;
import com.nadoumi.hr.web.request.EmployeeRequest;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    private final EmployeeMapper mapper = mock(EmployeeMapper.class);
    private final ISysUserService users = mock(ISysUserService.class);
    private final EmployeeService service = new EmployeeService(mapper, users);

    @Test
    void shouldRefuseANewHire_whenTheUsernameIsAlreadyTaken_andCreateNoAccount() {
        EmployeeRequest req = new EmployeeRequest(null, "nadoumi", "Initial-Pass-1!", "Nadoumi Admin", null, null, null,
                null, null, null, null, null, "FULL_TIME", "ACTIVE", LocalDate.of(2026, 10, 19), null, null, null, null,
                null, null, null, null, null, null, null);
        when(users.checkUserNameUnique(any(SysUser.class))).thenReturn(false);

        assertThatThrownBy(() -> service.create(req, false))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("already taken");

        verify(users, never()).insertUser(any(SysUser.class));
        verify(mapper, never()).insert(any());
    }
}
