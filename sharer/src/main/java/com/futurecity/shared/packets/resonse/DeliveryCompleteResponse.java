package com.futurecity.shared.packets.resonse;

/**
 * Server â†’ Client: Káº¿t quáº£ giao hĂ ng thĂ nh cĂ´ng.
 */
public class DeliveryCompleteResponse {
    public boolean success;
    public String message;
    public int reward; // Tiá»n thÆ°á»Ÿng
    public int totalCoins; // Tá»•ng coin sau khi nháº­n thÆ°á»Ÿng
}
