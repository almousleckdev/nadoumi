package com.ruoyi.web.controller.system;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.framework.web.service.AdminSessionCookie;
import com.ruoyi.framework.web.service.SysLoginService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cookie-based staff sign-in for the admin console: same credential checks as
 * {@code POST /login}, but the token goes into an httpOnly cookie and never into
 * the response body. Sign-out is the existing {@code /logout}, which also clears
 * the cookie. {@code /login} keeps returning a bearer token for API clients.
 */
@RestController
@RequestMapping("/staff/session")
public class StaffSessionController
{
    private final SysLoginService loginService;

    private final AdminSessionCookie sessionCookie;

    public StaffSessionController(SysLoginService loginService, AdminSessionCookie sessionCookie)
    {
        this.loginService = loginService;
        this.sessionCookie = sessionCookie;
    }

    @Anonymous
    @PostMapping
    public AjaxResult signIn(@RequestBody LoginBody loginBody, HttpServletResponse response)
    {
        String token = loginService.staffLogin(loginBody.getUsername(), loginBody.getPassword(),
                loginBody.getCode(), loginBody.getUuid());
        sessionCookie.write(response, token);
        return AjaxResult.success();
    }
}
