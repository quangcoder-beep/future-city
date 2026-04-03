package com.futurecity.shared.packets.resonse;

/**
 * Server â†’ Client: Káº¿t quáº£ há»§y Ä‘Æ¡n.
 */
public class OrderCancelResponse {
    public boolean success;
    public String message;
    public int orderId;
    public int remainingCoins;
}
