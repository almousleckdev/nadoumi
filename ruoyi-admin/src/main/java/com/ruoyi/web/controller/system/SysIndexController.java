package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * Home / lock-screen endpoints.
 *
 * @author ruoyi
 */
@RestController
public class SysIndexController
{
    @Autowired
    private RuoYiConfig ruoyiConfig;

    @Autowired
    private ISysUserService userService;

    /**
     * Landing hint. The UI is served from the front-end app, not here.
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("{} API v{} is running. Access the application through the front-end URL.",
                ruoyiConfig.getName(), ruoyiConfig.getVersion());
    }

    /**
     * Unlock the screen for the current user.
     */
    @PostMapping("/unlockscreen")
    public AjaxResult unlockScreen(@RequestBody Map<String, String> body)
    {
        String password = body.get("password");
        if (StringUtils.isEmpty(password))
        {
            return AjaxResult.error("Password must not be empty");
        }
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        if (user == null)
        {
            return AjaxResult.error("Session expired, please sign in again");
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            return AjaxResult.error("Incorrect password");
        }

        return AjaxResult.success("Screen unlocked");
    }
}
