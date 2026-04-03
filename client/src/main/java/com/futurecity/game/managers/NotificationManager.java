package com.futurecity.game.managers;

import com.futurecity.game.entities.NotificationEntry;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private final List<NotificationEntry> notifications = new ArrayList<>();
    private final List<NotificationListener> listeners = new ArrayList<>();

    public interface NotificationListener {
        void onNewNotification(NotificationEntry entry);
        void onNotificationsRead();
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void addNotification(String message, NotificationEntry.NotificationType type) {
        addNotification(message, type, null);
    }

    public void addNotification(String message, NotificationEntry.NotificationType type, String tag) {
        // Nếu tag đã tồn tại, xóa cái cũ đi trước (để cập nhật mới nhất)
        if (tag != null) {
            removeByTag(tag);
        }

        NotificationEntry entry = new NotificationEntry(message, type, tag);
        notifications.add(0, entry); // Mới nhất lên đầu

        // Giới hạn 30 thông báo
        while (notifications.size() > 30) {
            notifications.remove(notifications.size() - 1);
        }

        for (NotificationListener listener : listeners) {
            listener.onNewNotification(entry);
        }
    }

    public void removeByTag(String tag) {
        if (tag == null) return;
        boolean removed = notifications.removeIf(e -> tag.equals(e.getTag()));
        if (removed) {
            for (NotificationListener listener : listeners) {
                listener.onNotificationsRead(); // Kích hoạt refresh UI
            }
        }
    }

    public void markAllAsRead() {
        for (NotificationEntry entry : notifications) {
            entry.setRead(true);
        }
        for (NotificationListener listener : listeners) {
            listener.onNotificationsRead();
        }
    }

    public int getUnreadCount() {
        int count = 0;
        for (NotificationEntry entry : notifications) {
            if (!entry.isRead()) count++;
        }
        return count;
    }

    public List<NotificationEntry> getNotifications() {
        return notifications;
    }

    public void clearAll() {
        notifications.clear();
        for (NotificationListener listener : listeners) {
            listener.onNotificationsRead(); // Trạng thái thay đổi
        }
    }
}
