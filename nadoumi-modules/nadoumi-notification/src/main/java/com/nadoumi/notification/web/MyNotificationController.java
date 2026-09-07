package com.nadoumi.notification.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.notification.service.NotificationService;
import com.nadoumi.notification.web.response.NotificationView;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated principal's own notification feed — used by both the student
 * portal and the staff console. Every query is scoped to
 * {@link SecurityUtils#getUserId()}; a caller can only see and mark their own.
 */
@RestController
@RequestMapping("/api/notifications")
public class MyNotificationController {

    private final NotificationService service;

    public MyNotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationView> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listForRecipient(SecurityUtils.getUserId(), unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount(SecurityUtils.getUserId()));
    }

    @PostMapping("/{id}/read")
    public Map<String, Boolean> markRead(@PathVariable long id) {
        return Map.of("updated", service.markRead(id, SecurityUtils.getUserId()));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", service.markAllRead(SecurityUtils.getUserId()));
    }
}
