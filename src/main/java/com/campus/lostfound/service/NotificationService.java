package com.campus.lostfound.service;

import com.campus.lostfound.model.Notification;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.NotificationRepository;
import org.springframework.stereotype.Service;

/** Small helper so every module sends notifications the same way. */
@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public void notify(User user, String message) {
        if (user == null) return;
        notifications.save(new Notification(user, message));
    }
}
