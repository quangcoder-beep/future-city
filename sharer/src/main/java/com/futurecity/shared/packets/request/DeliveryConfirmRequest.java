package com.futurecity.shared.packets.request;

/**
 * Gá»­i cho Buyer (ngÆ°á»i tháº­t) khi Shipper Ä‘áº¿n giao hĂ ng
 */
public class DeliveryConfirmRequest {
    public int orderId;
    public String courierName; // TĂªn shipper Ä‘ang chá» giao
    public String itemName; // TĂªn máº·t hĂ ng Ä‘ang giao
}
