package com.futurecity.shared.packets.request;

/**
 * Client â†’ Server: Äáº·t hĂ ng (Player Ä‘áº·t, chá» courier giao).
 */
public class OrderRequest {
    public int playerId;
    public String shopId;
    public int itemId;
    public float destX, destY, destZ; // Vá»‹ trĂ­ giao hĂ ng
}
