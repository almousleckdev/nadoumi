package com.nadoumi.notification.domain;

import java.time.LocalDateTime;

/**
 * Row of {@code nad_notification_template} — the {@code ${var}} subject/body for
 * one {@code (type, channel, locale)}. Unique on that triple; the renderer falls
 * back to {@code locale = 'en'}.
 */
public class NotificationTemplate {

    private Long id;
    private String type;
    private String channel;
    private String locale;
    private String subjectTpl;
    private String bodyTpl;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getSubjectTpl() { return subjectTpl; }
    public void setSubjectTpl(String subjectTpl) { this.subjectTpl = subjectTpl; }

    public String getBodyTpl() { return bodyTpl; }
    public void setBodyTpl(String bodyTpl) { this.bodyTpl = bodyTpl; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
