package com.futurecity.shared.packets.resonse;

/**
 * Server â†’ Client: Dá»¯ liá»‡u inventory cá»§a player.
 */
public class InventoryResponse {
    public int playerCoins;
    public int[] itemIds;
    public String[] itemNames;
    public int[] quantities;

    // ÄÆ¡n Ä‘ang giao (Shipper)
    public java.util.List<OrderInfo> activeDeliveries = new java.util.ArrayList<>();

    // ÄÆ¡n Ä‘Ă£ Ä‘áº·t (Buyer)
    public java.util.List<OrderInfo> myOrders = new java.util.ArrayList<>();

    public static class OrderInfo {
        public int orderId;
        public String shopId;
        public String itemName;
        public String shopName;
        public String destinationName;
        public float destX, destY, destZ;
        public float shopX, shopY, shopZ;
        public int reward;
        public String status;
        public long acceptedAtMs;
        public int timeoutMinutes;
        public String buyerType;
        public String buyerRefId;
    }
}
