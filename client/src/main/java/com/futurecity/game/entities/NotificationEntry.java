package com.futurecity.game.entities;

public class NotificationEntry {
    public enum NotificationType {
        INFO, SUCCESS, WARNING, ALERT
    }

    private String message;
    private String tag; // Để định danh và xóa (ví dụ: "order_123")
    private long timestamp;
    private boolean read;
    private NotificationType type;

    public NotificationEntry(String message, NotificationType type) {
        this(message, type, null);
    }

    public NotificationEntry(String message, NotificationType type, String tag) {
        this.message = message;
        this.type = type;
        this.tag = tag;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getMessage() { return message; }
    public String getTag() { return tag; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public NotificationType getType() { return type; }
}
