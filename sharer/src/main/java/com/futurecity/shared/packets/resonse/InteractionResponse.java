package com.futurecity.shared.packets.resonse;

import com.futurecity.shared.enums.InteractionUIType;

/**
 * Server → Client: Kết quả tương tác với vật thể.
 * Chứa cả dữ liệu UI (items, dialogue, orders) để client hiển thị.
 */
public class InteractionResponse {
    public int id; // ID người chơi
    public boolean success;
    public String message;
    public String targetId;
    public String newState;

    // --- UI Data ---
    public InteractionUIType uiType; // Loại UI cần hiện
    public String displayName; // Tên hiển thị: "Cửa Hàng Quần Áo"
    public int playerCoins; // Số coin hiện tại của player
    public boolean isSubscribed; // Đã đăng ký nhận tin hay chưa
    public int activeOrderId; // ID đơn hàng đang tương tác (Pickup/Handover)
    public boolean isPlayerTarget; // true nếu tương tác với người chơi, false nếu là NPC/Shop/Nhà

    // Shop data
    public int[] itemIds; // ID các item
    public String[] itemNames; // Tên item
    public int[] itemPrices; // Giá bán
    public int[] itemStocks; // Số lượng tồn kho

    // Delivery orders (đơn giao hàng khả dụng)
    public int[] orderIds;
    public String[] orderItemNames;
    public int[] orderRewards;
    public String[] orderDestinations; // Tên điểm đến

    // NPC dialogue
    public String[] dialogueLines;
}
