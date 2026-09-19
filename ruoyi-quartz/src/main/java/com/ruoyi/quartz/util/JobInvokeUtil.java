package com.ruoyi.quartz.util;

import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.quartz.domain.SysJob;

/**
 * Runs a scheduled job. Resolution is delegated to {@link JobRegistry}, which
 * only ever calls {@code run()} on a registered bean.
 */
public class JobInvokeUtil
{
    public static void invokeMethod(SysJob sysJob)
    {
        SpringUtils.getBean(JobRegistry.class).run(sysJob.getInvokeTarget());
    }
}
