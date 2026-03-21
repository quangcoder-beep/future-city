package com.futurecity.shared.packets.resonse;

/**
 * Server â†’ Client (Shipper): Cáº­p nháº­t vá»‹ trĂ­ thá»i gian thá»±c cá»§a ngÆ°á»i mua.
 * Packets nĂ y giĂºp Shipper tĂ¬m tháº¥y ngÆ°á»i mua ngay cáº£ khi há» Ä‘ang di chuyá»ƒn.
 */
public class DeliveryGPSUpdate {
    public int orderId;
    public float destX;
    public float destY;
    public float destZ;
    public String buyerName;
}
