package com.nadoumi.notification.domain;

/**
 * Row of {@code nad_notification_preference} — one user's opt-in/out for a
 * {@code (type, channel)} pair. Absent row = channel-default. Transactional
 * {@link NotificationType}s ignore this table.
 */
public class NotificationPreference {

    private Long id;
    private Long userId;
    private String type;
    private String channel;
    private boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
