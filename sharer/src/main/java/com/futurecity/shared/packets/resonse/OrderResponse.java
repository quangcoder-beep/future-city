package com.futurecity.shared.packets.resonse;

/**
 * Server â†’ Client: Káº¿t quáº£ Ä‘áº·t hĂ ng.
 */
public class OrderResponse {
    public boolean success;
    public String message;
    public int orderId;
    public int remainingCoins;
}
