package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.InventoryRepository;
import com.example.server_spring.repository.OrderRepository;
import com.example.server_spring.repository.PlayerRepository;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.packets.request.InventoryRequest;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Xá»­ lĂ½ gĂ³i tin tĂºi Ä‘á»“ (Inventory).
 * - Tráº£ vá» danh sĂ¡ch item trong tĂºi, sá»‘ coin, vĂ  Ä‘Æ¡n hĂ ng Ä‘ang giao.
 */
@Component
public class InventoryPacketHandler {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerRepository playerRepo;
    @Autowired
    private InventoryRepository inventoryRepo;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private com.example.server_spring.services.WorldService worldService;

    /**
     * Xá»­ lĂ½ yĂªu cáº§u xem tĂºi Ä‘á»“ (báº¥m H).
     * Tráº£ vá»: coins, danh sĂ¡ch item, Ä‘Æ¡n Ä‘ang giao (náº¿u cĂ³) kĂ¨m tá»a Ä‘á»™ Ä‘Ă­ch.
     */
    public void handleInventory(Connection conn, InventoryRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null)
            return;
        int userId = player.getDbUserId();

        InventoryResponse resp = new InventoryResponse();

        // 1. Láº¥y sá»‘ coin hiá»‡n táº¡i
        PlayerRepository.PlayerData pd = playerRepo.getPlayerData(userId);
        if (pd != null) {
            resp.playerCoins = pd.coins;
        }

        // 2. Láº¥y danh sĂ¡ch item trong tĂºi Ä‘á»“
        List<InventoryRepository.PlayerItem> items = inventoryRepo.getPlayerInventory(userId);
        resp.itemIds = new int[items.size()];
        resp.itemNames = new String[items.size()];
        resp.quantities = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            resp.itemIds[i] = items.get(i).itemId;
            resp.itemNames[i] = items.get(i).itemName;
            resp.quantities[i] = items.get(i).quantity;
        }

        // 3. Láº¥y Ä‘Æ¡n hĂ ng Ä‘ang giao (Shipper)
        List<OrderRepository.OrderData> deliveries = orderRepo.getActiveDeliveries(userId);
        for (OrderRepository.OrderData od : deliveries) {
            InventoryResponse.OrderInfo info = new InventoryResponse.OrderInfo();
            info.orderId = od.orderId;
            info.shopId = od.shopId;
            info.itemName = od.itemName;
            info.reward = od.reward;
            info.status = od.status;
            info.acceptedAtMs = od.acceptedAt != null ? od.acceptedAt.getTime() : 0;
            info.timeoutMinutes = od.timeoutMinutes;
            info.buyerType = od.buyerType;
            info.buyerRefId = od.buyerRefId;
            info.shopX = od.shopX;
            info.shopY = od.shopY;
            info.shopZ = od.shopZ;

            info.destX = od.destX;
            info.destY = od.destY;
            info.destZ = od.destZ;
            
            info.shopX = od.shopX;
            info.shopY = od.shopY;
            info.shopZ = od.shopZ;
            info.shopName = od.shopName != null ? od.shopName : od.shopId;
            info.destinationName = od.destinationName;

            // DEFENSIVE: If shop coordinates are zero, try fetching from WorldService
            if (info.shopX == 0 && info.shopZ == 0) {
                com.example.server_spring.entity.ServerInteractable shopObj = worldService.getObjectById(od.shopId);
                if (shopObj != null) {
                    info.shopX = shopObj.getPosition().x;
                    info.shopY = shopObj.getPosition().y;
                    info.shopZ = shopObj.getPosition().z;
                    System.out.println("[GPS-DEBUG] Order #" + od.orderId + " Shop coords recovered from WorldService: (" + info.shopX + "," + info.shopZ + ")");
                }
            }

            resp.activeDeliveries.add(info);
        }

        // 4. Láº¥y Ä‘Æ¡n hĂ ng Ä‘Ă£ Ä‘áº·t (Buyer)
        List<OrderRepository.OrderData> myOrders = orderRepo.getBuyerOrders(userId);
        for (OrderRepository.OrderData od : myOrders) {
            InventoryResponse.OrderInfo info = new InventoryResponse.OrderInfo();
            info.orderId = od.orderId;
            info.shopId = od.shopId;
            info.itemName = od.itemName;
            info.status = od.status;
            info.buyerType = od.buyerType;
            info.buyerRefId = od.buyerRefId;
            resp.myOrders.add(info);
        }

        conn.sendTCP(resp);
    }
}
