package com.ruoyi.framework.web.service;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The admin console's session cookie. The token lives in an httpOnly cookie so
 * script can never read it. Because the browser attaches cookies on its own, an
 * unsafe request (anything but GET/HEAD/OPTIONS/TRACE) is honoured only when it
 * also carries {@link #CLIENT_HEADER}, which a cross-site form or simple request
 * cannot set; SameSite=Strict is the first line, this header is defence in depth.
 */
@Component
public class AdminSessionCookie
{
    public static final String CLIENT_HEADER = "X-Nadoumi-Client";

    public static final String CLIENT_VALUE = "admin";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final String name;

    private final boolean secure;

    public AdminSessionCookie(@Value("${nadoumi.admin-session.cookie-name:NAD_ADMIN_SESSION}") String name,
            @Value("${nadoumi.admin-session.secure:true}") boolean secure)
    {
        this.name = name;
        this.secure = secure;
    }

    /**
     * The token carried by the session cookie, or null when there is none or the
     * request is an unsafe one without the client header.
     */
    public String tokenFrom(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
        {
            return null;
        }
        if (!SAFE_METHODS.contains(request.getMethod()) && !CLIENT_VALUE.equals(request.getHeader(CLIENT_HEADER)))
        {
            return null;
        }
        for (Cookie cookie : cookies)
        {
            if (name.equals(cookie.getName()) && !cookie.getValue().isEmpty())
            {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** A browser-session cookie: the server-side token expiry is the real authority. */
    public void write(HttpServletResponse response, String token)
    {
        response.addHeader(HttpHeaders.SET_COOKIE, base(token).build().toString());
    }

    public void clear(HttpServletResponse response)
    {
        response.addHeader(HttpHeaders.SET_COOKIE, base("").maxAge(0).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder base(String value)
    {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secure).sameSite("Strict").path("/");
    }
}
