package com.loanportal.service;

import com.loanportal.model.Notification;
import com.loanportal.model.OutboxEmail;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class NotificationService {

    @Transactional
    public void notify(User user, String message, String link) {
        Notification n = new Notification();
        n.user = user;
        n.message = message;
        n.link = link;
        n.persist();
    }

    /** Mock email channel: writes to the outbox table instead of calling a real ESP. */
    @Transactional
    public void sendEmail(String toAddress, String subject, String body) {
        OutboxEmail email = new OutboxEmail();
        email.toAddress = toAddress;
        email.subject = subject;
        email.body = body;
        email.status = "SENT";
        email.persist();
    }

    public List<Notification> forUser(Long userId, int limit) {
        return Notification.find("user.id = ?1 order by createdAt desc", userId).page(0, limit).list();
    }

    public long unreadCount(Long userId) {
        return Notification.count("user.id = ?1 and read = false", userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = Notification.findById(notificationId);
        if (n != null && n.user.id.equals(userId)) {
            n.read = true;
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        Notification.update("read = true where user.id = ?1 and read = false", userId);
    }
}
