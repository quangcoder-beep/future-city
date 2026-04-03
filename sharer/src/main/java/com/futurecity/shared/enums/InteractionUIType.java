package com.futurecity.shared.enums;

/**
 * Loáº¡i UI hiá»ƒn thá»‹ khi tÆ°Æ¡ng tĂ¡c vá»›i váº­t thá»ƒ.
 * Má»—i váº­t thá»ƒ (Shop, NPC...) cĂ³ má»™t InteractionUIType riĂªng.
 */
public enum InteractionUIType {
    NONE, // KhĂ´ng hiá»ƒn thá»‹ UI (chá»‰ trigger logic)
    SHOP_CLOTHES, // Menu cá»­a hĂ ng quáº§n Ă¡o
    SHOP_FOOD, // Menu cá»­a hĂ ng Ä‘á»“ Äƒn
    SHOP_GENERAL, // Menu cá»­a hĂ ng tá»•ng há»£p
    NPC_DIALOGUE, // Há»™p thoáº¡i NPC
    HOUSE_INFO, // Xem thĂ´ng tin nhĂ 
    PLAYER_INTERACT, // TÆ°Æ¡ng tĂ¡c ngÆ°á»i chÆ¡i
    PICKUP_ACTION, // ThĂ´ng bĂ¡o Ä‘Ă£ láº¥y hĂ ng
    HANDOVER_ACTION // Giao diá»‡n bĂ n giao hĂ ng chuyĂªn nghiá»‡p
}
