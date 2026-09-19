package com.ruoyi.common.core.job;

/**
 * A bean the Quartz scheduler may invoke. Only beans implementing this interface
 * can be the target of a {@code sys_job} row, and only through {@link #run()}:
 * the job table never chooses a class or a method, just which registered bean
 * runs. Registering a new scheduled job means implementing this interface.
 */
public interface SchedulableJob {

    void run();
}
