package com.futurecity.shared.packets.resonse;

/**
 * Server â†’ Client: Káº¿t quáº£ Ä‘Äƒng kĂ½ shop.
 */
public class ShopSubscribeResponse {
    public boolean success;
    public String message;
    public String shopId;
    public boolean subscribed; // Tráº¡ng thĂ¡i sau: Ä‘Ă£ Ä‘Äƒng kĂ½ hay Ä‘Ă£ há»§y
}
