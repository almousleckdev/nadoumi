package com.ruoyi.quartz.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.job.SchedulableJob;

/**
 * The fixed set of beans the scheduler may run. A {@code sys_job.invoke_target}
 * is valid only as {@code <beanName>.run()} where the bean implements
 * {@link SchedulableJob}; classes, other methods and arguments are never
 * resolved, so an admin who can edit jobs cannot reach arbitrary code.
 */
@Component
public class JobRegistry
{
    private static final Pattern INVOKE_TARGET = Pattern.compile("([A-Za-z][A-Za-z0-9_]*)\\.run\\(\\)");

    private final Map<String, SchedulableJob> jobs;

    /** Spring injects every {@link SchedulableJob} bean keyed by its bean name. */
    public JobRegistry(Map<String, SchedulableJob> jobs)
    {
        this.jobs = Map.copyOf(jobs);
    }

    public boolean isAllowed(String invokeTarget)
    {
        return resolve(invokeTarget) != null;
    }

    public void run(String invokeTarget)
    {
        SchedulableJob job = resolve(invokeTarget);
        if (job == null)
        {
            throw new IllegalArgumentException("Not a registered job target: " + invokeTarget);
        }
        job.run();
    }

    private SchedulableJob resolve(String invokeTarget)
    {
        if (invokeTarget == null)
        {
            return null;
        }
        Matcher matcher = INVOKE_TARGET.matcher(invokeTarget);
        return matcher.matches() ? jobs.get(matcher.group(1)) : null;
    }
}
