package com.nadoumi.identity.web;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Extra profile detail the stock RuoYi {@code /system/user/profile} does not
 * surface: the current session — when it started, how long it has been active,
 * when it expires, and the client (IP, resolved city, browser, OS).
 */
@RestController
@RequestMapping("/api/staff/profile")
public class StaffProfileController {

    @GetMapping("/session")
    public Map<String, Object> session() {
        LoginUser lu = SecurityUtils.getLoginUser();
        long now = System.currentTimeMillis();
        long loginTime = lu.getLoginTime() == null ? now : lu.getLoginTime();
        long expireTime = lu.getExpireTime() == null ? now : lu.getExpireTime();

        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("loginTime", loginTime);
        out.put("expireTime", expireTime);
        out.put("loggedInForSeconds", Math.max(0, (now - loginTime) / 1000));
        out.put("expiresInSeconds", Math.max(0, (expireTime - now) / 1000));
        out.put("ipaddr", lu.getIpaddr());
        out.put("location", lu.getLoginLocation());
        out.put("browser", lu.getBrowser());
        out.put("os", lu.getOs());
        return out;
    }
}
