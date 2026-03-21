package com.futurecity.shared.packets.resonse;

/**
 * Server → Client: Kết quả nhận đơn giao.
 */
public class DeliveryAcceptResponse {
    public boolean success;
    public String message;
    public int orderId;
    public float destX, destY, destZ; // Tọa độ giao hàng
    public String destinationName; // Tên điểm đến
}
