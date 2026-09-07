package com.nadoumi.notification.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.notification.service.NotificationService;
import com.nadoumi.notification.web.response.NotificationDetailView;
import com.nadoumi.notification.web.response.NotificationView;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff oversight of notifications — every recipient, with per-channel delivery
 * status. Read-only; gated by {@code nad:notification:list} / {@code :view}.
 * A staff member's <i>own</i> feed is {@code /api/notifications}.
 */
@RestController
@RequestMapping("/api/staff/notifications")
public class StaffNotificationController {

    private final NotificationService service;

    public StaffNotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:notification:list')")
    public PageResponse<NotificationView> list(
            @RequestParam(required = false) Long recipientUserId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.staffSearch(recipientUserId, type, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:notification:view')")
    public ResponseEntity<NotificationDetailView> get(@PathVariable long id) {
        NotificationDetailView view = service.staffGet(id);
        return view == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(view);
    }
}
