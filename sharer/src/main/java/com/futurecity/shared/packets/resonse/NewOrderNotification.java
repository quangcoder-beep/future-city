package com.futurecity.shared.packets.resonse;

/**
 * Server → Client: Thông báo đơn mới từ shop đã đăng ký.
 */
public class NewOrderNotification {
    public int orderId;
    public String shopId;
    public String shopName;
    public String itemName;
    public int reward;
    public String destination;
}
