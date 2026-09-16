package com.ruoyi.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Safe resolution of the current caller for audit columns (create_by,
 * update_by, submitted_by, ...) when a write might happen outside an
 * authenticated request (a scheduled job, a test, or a genuine
 * security-context failure). {@link SecurityUtils#getUsername()} /
 * {@link SecurityUtils#getUserId()} throw in that case; this logs a warning
 * and falls back instead of failing the write, so the fallback path is
 * visible rather than silently attributing every such write to "system"/0.
 */
public class AuditActor
{
    private static final Logger log = LoggerFactory.getLogger(AuditActor.class);

    public static final String SYSTEM_USERNAME = "system";
    public static final long SYSTEM_USER_ID = 0L;

    private AuditActor()
    {
    }

    /**
     * The signed-in username, or {@link #SYSTEM_USERNAME} outside an
     * authenticated request.
     */
    public static String username()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            log.warn("no authenticated caller resolving the audit username; attributing to \"{}\": {}",
                    SYSTEM_USERNAME, e.toString());
            return SYSTEM_USERNAME;
        }
    }

    /**
     * The signed-in user id, or {@link #SYSTEM_USER_ID} outside an
     * authenticated request.
     */
    public static long userId()
    {
        try
        {
            Long id = SecurityUtils.getUserId();
            if (id == null)
            {
                throw new IllegalStateException("SecurityUtils.getUserId() returned null");
            }
            return id;
        }
        catch (Exception e)
        {
            log.warn("no authenticated caller resolving the audit user id; attributing userId={}: {}",
                    SYSTEM_USER_ID, e.toString());
            return SYSTEM_USER_ID;
        }
    }
}
