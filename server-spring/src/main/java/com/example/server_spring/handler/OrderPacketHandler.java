package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.OrderRepository;
import com.example.server_spring.repository.PlayerRepository;
import com.example.server_spring.repository.ShopRepository;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.packets.request.OrderRequest;
import com.futurecity.shared.packets.request.ShopListRequest;
import com.futurecity.shared.packets.resonse.OrderResponse;
import com.futurecity.shared.packets.resonse.ShopListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles remote order requests (Phone App).
 */
@Component
public class OrderPacketHandler {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private ShopRepository shopRepo;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private PlayerRepository playerRepo;
    @Autowired
    private com.example.server_spring.services.OrderService orderService;
    @Autowired
    private com.example.server_spring.services.WorldService worldService;

    public void handleShopList(Connection conn, ShopListRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null) return;

        ShopListResponse res = new ShopListResponse();
        List<ShopRepository.ShopItemData> allItems = shopRepo.getAllShopItems();

        Map<String, List<ShopRepository.ShopItemData>> itemsByShop = allItems.stream()
                .collect(Collectors.groupingBy((ShopRepository.ShopItemData i) -> i.shopId));

        for (Map.Entry<String, List<ShopRepository.ShopItemData>> entry : itemsByShop.entrySet()) {
            ShopListResponse.ShopInfo shopInfo = new ShopListResponse.ShopInfo();
            shopInfo.shopId = entry.getKey();
            shopInfo.shopName = entry.getKey();

            for (ShopRepository.ShopItemData itemData : entry.getValue()) {
                ShopListResponse.ShopItemInfo itemInfo = new ShopListResponse.ShopItemInfo();
                itemInfo.itemId = itemData.itemId;
                itemInfo.itemName = itemData.itemName;
                itemInfo.price = itemData.price;
                shopInfo.items.add(itemInfo);
            }
            res.shops.add(shopInfo);
        }
        conn.sendTCP(res);
    }

    public void handleOrder(Connection conn, OrderRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null) return;

        OrderResponse res = new OrderResponse();
        ShopRepository.ShopItemData targetItem = shopRepo.getAllShopItems().stream()
                .filter(i -> i.shopId.equals(req.shopId) && i.itemId == req.itemId)
                .findFirst().orElse(null);

        if (targetItem == null || targetItem.currentStock <= 0) {
            res.success = false;
            res.message = targetItem == null ? "Item does not exist!" : "Out of stock!";
            conn.sendTCP(res);
            return;
        }

        float shopX = 0, shopZ = 0;
        com.example.server_spring.entity.ServerInteractable shopObj = worldService.getObjectById(req.shopId);
        if (shopObj != null) {
            shopX = shopObj.getPosition().x;
            shopZ = shopObj.getPosition().z;
        }
        float destX = req.destX != 0 ? req.destX : player.getState().position.x;
        float destZ = req.destZ != 0 ? req.destZ : player.getState().position.z;
        float distance = com.badlogic.gdx.math.Vector2.dst(shopX, shopZ, destX, destZ);

        int deliveryFee = 20 + (int) (targetItem.price * 0.1f) + (int) (distance / 10f);
        int totalCost = targetItem.price + deliveryFee;
        int currentCoins = playerRepo.getPlayerCoins(player.getDbUserId());

        if (currentCoins < totalCost) {
            res.success = false;
            res.message = "Insufficient coins! Need: " + totalCost + " (Owned: " + currentCoins + ")";
            conn.sendTCP(res);
            return;
        }

        // Deduct money upfront (Escrow)
        playerRepo.updateCoins(player.getDbUserId(), currentCoins - totalCost);
        shopRepo.updateStock(req.shopId, req.itemId, -1);

        float destY = req.destY != 0 ? req.destY : player.getState().position.y;

        int orderId = orderRepo.createOrder(
                req.shopId,
                req.itemId,
                targetItem.price,
                "PLAYER",
                String.valueOf(player.getDbUserId()),
                deliveryFee,
                destX, destY, destZ);

        if (orderId > 0) {
            res.success = true;
            res.orderId = orderId;
            res.message = "Order placed successfully! Waiting for courier.";
            res.remainingCoins = playerRepo.getPlayerCoins(player.getDbUserId());
            orderService.notifySubscribers(orderId, req.shopId, targetItem.itemName, deliveryFee,
                    "Player (" + (int) destX + "," + (int) destY + ")");
        } else {
            res.success = false;
            res.message = "System error creating order.";
            res.remainingCoins = playerRepo.getPlayerCoins(player.getDbUserId());
            shopRepo.updateStock(req.shopId, req.itemId, 1);
        }

        conn.sendTCP(res);
    }

    public void handleOrderCancel(Connection conn, com.futurecity.shared.packets.request.OrderCancelRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null) return;

        com.futurecity.shared.packets.resonse.OrderCancelResponse res = new com.futurecity.shared.packets.resonse.OrderCancelResponse();
        res.orderId = req.orderId;

        OrderRepository.OrderData od = orderRepo.getOrderData(req.orderId);
        if (od == null) {
            res.success = false;
            res.message = "Order not found.";
            conn.sendTCP(res);
            return;
        }

        if (!String.valueOf(player.getDbUserId()).equals(od.buyerRefId)) {
            res.success = false;
            res.message = "Unauthorized to cancel this order.";
            conn.sendTCP(res);
            return;
        }

        if ("PENDING".equals(od.status)) {
            boolean ok = orderRepo.updateOrderStatus(req.orderId, "CANCELLED");
            if (ok) {
                // Refund 100% (Price + Delivery Fee)
                int refundAmount = od.itemPrice + od.reward;
                int currentBal = playerRepo.getPlayerCoins(player.getDbUserId());
                playerRepo.updateCoins(player.getDbUserId(), currentBal + refundAmount);
                
                shopRepo.updateStock(od.shopId, od.itemId, 1);
                res.success = true;
                res.message = "Order cancelled (Free). Refunded: " + refundAmount;
            } else {
                res.success = false;
                res.message = "System error during cancellation.";
            }
        } else if ("PENDING_PICKUP".equals(od.status) || "DELIVERING".equals(od.status)) {
            // Buyer cancels active order -> Penalty applies
            int penalty = od.reward / 2;
            
            // 1. Refund full amount first
            int refundAmount = od.itemPrice + od.reward;
            int currentBal = playerRepo.getPlayerCoins(player.getDbUserId());
            playerRepo.updateCoins(player.getDbUserId(), currentBal + refundAmount);
            
            // 2. Apply penalty and compensate shipper
            boolean compensated = orderRepo.compensateShipper(req.orderId, penalty);
            
            if (compensated) {
                orderRepo.updateOrderStatus(req.orderId, "CANCELLED");
                shopRepo.updateStock(od.shopId, od.itemId, 1);
                res.success = true;
                res.message = "Order cancelled. Penalty for courier: " + penalty;
                
                int courierId = orderRepo.getCourierId(req.orderId);
                ServerPlayer courier = playerService.getAllPlayers().stream()
                        .filter(p -> p.getDbUserId() == courierId)
                        .findFirst().orElse(null);
                if (courier != null && courier.getConnection() != null) {
                    com.futurecity.shared.packets.resonse.DeliveryCancelResponse deliveryCancelRes = new com.futurecity.shared.packets.resonse.DeliveryCancelResponse();
                    deliveryCancelRes.success = true;
                    deliveryCancelRes.message = "Buyer cancelled! You received " + penalty + " Coins compensation.";
                    courier.getConnection().sendTCP(deliveryCancelRes);
                }
            } else {
                res.success = false;
                res.message = "Cancellation failed (Internal logic error).";
            }
        } else {
            res.success = false;
            res.message = "Cannot cancel order in this status.";
        }
        res.remainingCoins = playerRepo.getPlayerCoins(player.getDbUserId());
        conn.sendTCP(res);
    }
}
