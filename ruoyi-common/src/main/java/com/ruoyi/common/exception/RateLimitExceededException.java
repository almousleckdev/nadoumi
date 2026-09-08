package com.ruoyi.common.exception;

/**
 * Raised by {@code RateLimiterAspect} when a caller exceeds a {@code @RateLimiter}
 * quota. The exception handlers map this to HTTP 429 with a {@code Retry-After}
 * header, rather than the HTTP 200 {@code AjaxResult} envelope a plain
 * {@link ServiceException} would produce.
 */
public class RateLimitExceededException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /** Seconds after which the caller may retry — the rate-limit window. */
    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds)
    {
        super("Too many requests, please try again shortly");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds()
    {
        return retryAfterSeconds;
    }
}
