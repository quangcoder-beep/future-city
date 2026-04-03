package com.futurecity.shared.packets.resonse;

import java.util.ArrayList;
import java.util.List;

/**
 * Server â†’ Client: Tráº£ vá» danh sĂ¡ch Shop vĂ  cĂ¡c máº·t hĂ ng cá»§a Shop.
 */
public class ShopListResponse {

    public List<ShopInfo> shops = new ArrayList<>();

    public static class ShopInfo {
        public String shopId;
        public String shopName;
        public List<ShopItemInfo> items = new ArrayList<>();
    }

    public static class ShopItemInfo {
        public int itemId;
        public String itemName;
        public int price;
        // CĂ³ thá»ƒ thĂªm sá»‘ lÆ°á»£ng tá»“n kho (stock) náº¿u muá»‘n, nhÆ°ng Ä‘áº·t online thÆ°á»ng khĂ´ng
        // cáº§n biáº¿t tá»“n kho thá»±c cho Ä‘áº¿n khi áº¥n mua.
    }
}
