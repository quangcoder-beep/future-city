package com.futurecity.shared.packets.resonse;

/**
 * Server â†’ Client: Káº¿t quáº£ há»§y giao.
 */
public class DeliveryCancelResponse {
    public boolean success;
    public String message;
    public int penalty; // Tiá»n pháº¡t (10% reward)
    public int remainingCoins;
}
