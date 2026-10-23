package com.campus.lostfound.controller;

import com.campus.lostfound.controller.Dtos.NotificationView;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.NotificationRepository;
import com.campus.lostfound.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Notification module: list my alerts, unread count, mark as read. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notifications;
    private final CurrentUserService currentUser;

    public NotificationController(NotificationRepository notifications, CurrentUserService currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationView> list() {
        return notifications.findByUserOrderByCreatedAtDesc(currentUser.currentUser())
                .stream().map(NotificationView::of).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notifications.countByUserAndReadFalse(currentUser.currentUser()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        User me = currentUser.currentUser();
        var found = notifications.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Notification not found"));
        }
        var n = found.get();
        if (!n.getUser().getUserId().equals(me.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your notification"));
        }
        n.setRead(true);
        notifications.save(n);
        return ResponseEntity.ok(NotificationView.of(n));
    }

    @PostMapping("/read-all")
    public Map<String, String> markAllRead() {
        notifications.findByUserOrderByCreatedAtDesc(currentUser.currentUser())
                .forEach(n -> { n.setRead(true); notifications.save(n); });
        return Map.of("status", "ok");
    }
}
