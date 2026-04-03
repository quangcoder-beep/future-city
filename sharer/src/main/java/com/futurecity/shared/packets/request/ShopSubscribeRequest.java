package com.futurecity.shared.packets.request;

/**
 * Client â†’ Server: ÄÄƒng kĂ½/há»§y Ä‘Äƒng kĂ½ nháº­n thĂ´ng bĂ¡o tá»« shop.
 */
public class ShopSubscribeRequest {
    public int playerId;
    public String shopId;
    public boolean subscribe; // true = Ä‘Äƒng kĂ½, false = há»§y Ä‘Äƒng kĂ½
}
