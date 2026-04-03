package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerInteractable;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.*;
import com.example.server_spring.services.InteractionService;
import com.example.server_spring.services.PlayerService;
import com.example.server_spring.services.WorldService;
import com.futurecity.shared.packets.request.BuyRequest;
import com.futurecity.shared.packets.request.InteractionRequest;
import com.futurecity.shared.packets.request.ShopSubscribeRequest;
import com.futurecity.shared.packets.resonse.BuyResponse;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.ShopSubscribeResponse;
import com.futurecity.shared.packets.resonse.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShopPacketHandler {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private ShopRepository shopRepo;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private PlayerRepository playerRepo;
    @Autowired
    private InventoryRepository inventoryRepo;
    @Autowired
    private TransactionRepository transactionRepo;
    @Autowired
    private SubscriptionRepository subscriptionRepo;
    @Autowired
    private InteractionService interactionService;
    @Autowired
    private WorldService worldService;

    public void handleInteraction(Connection conn, InteractionRequest req) {
        if (req.targetId != null && req.targetId.length() > 64) {
             System.err.println("[SECURITY] Blocked oversized targetId from ConnID=" + conn.getID());
             conn.close();
             return;
        }

        ServerPlayer player = playerService.get(conn.getID());
        if (player == null) return;

        List<OrderRepository.OrderData> deliveries = orderRepo.getActiveDeliveries(player.getDbUserId());
        
        for (OrderRepository.OrderData activeOrder : deliveries) {
            if ("PENDING_PICKUP".equals(activeOrder.status)) {
                if (req.targetId.equals(activeOrder.shopId)) {
                    // Check distance to Shop
                    ServerInteractable shopObj = worldService.getObjectById(activeOrder.shopId);
                    if (shopObj != null) {
                        float dist = com.badlogic.gdx.math.Vector3.dst(
                                player.getState().position.x, player.getState().position.y, player.getState().position.z,
                                shopObj.getPosition().x, shopObj.getPosition().y, shopObj.getPosition().z);
                        if (dist > 200.0f) {
                            InteractionResponse resp = new InteractionResponse();
                            resp.success = false;
                            resp.message = "Too far from shop! (" + (int)dist + "m)";
                            conn.sendTCP(resp);
                            return;
                        }
                    }

                    InteractionResponse resp = new InteractionResponse();
                    resp.id = player.getId();
                    resp.targetId = req.targetId;
                    resp.success = true;
                    resp.uiType = com.futurecity.shared.enums.InteractionUIType.PICKUP_ACTION;
                    resp.displayName = activeOrder.shopName == null ? activeOrder.shopId : activeOrder.shopName;
                    resp.activeOrderId = activeOrder.orderId;
                    resp.playerCoins = playerRepo.getPlayerCoins(player.getDbUserId());
                    resp.isPlayerTarget = false;
                    resp.message = "Order #" + activeOrder.orderId + ": " + activeOrder.itemName;
                    conn.sendTCP(resp);
                    return;
                }
            } else if ("DELIVERING".equals(activeOrder.status)) {
                boolean isTargetMatch = false;
                ServerPlayer targetPlayerForOrder = null;
                float targetX = 0, targetY = 0, targetZ = 0;
                boolean hasTargetCoords = false;
                
                if ("PLAYER".equals(activeOrder.buyerType)) {
                    try {
                        int buyerDbId = Integer.parseInt(activeOrder.buyerRefId);
                        targetPlayerForOrder = playerService.getAllPlayers().stream()
                                .filter(p -> p.getDbUserId() == buyerDbId)
                                .findFirst().orElse(null);
                        
                        if (req.targetId.startsWith("player_")) {
                             int targetConnId = Integer.parseInt(req.targetId.replace("player_", ""));
                             ServerPlayer interactingWith = playerService.get(targetConnId);
                             
                             if (targetPlayerForOrder != null && interactingWith != null && 
                                 targetPlayerForOrder.getDbUserId() == interactingWith.getDbUserId()) {
                                 isTargetMatch = true;
                                 targetX = interactingWith.getState().position.x;
                                 targetY = interactingWith.getState().position.y;
                                 targetZ = interactingWith.getState().position.z;
                                 hasTargetCoords = true;
                             }
                        }
                    } catch (Exception e) {}
                } else if (req.targetId.equals(activeOrder.buyerRefId)) {
                    isTargetMatch = true;
                    targetX = activeOrder.destX;
                    targetY = activeOrder.destY;
                    targetZ = activeOrder.destZ;
                    hasTargetCoords = true;
                }
                
                if (isTargetMatch) {
                    // Check distance to Destination
                    if (hasTargetCoords) {
                        float dist = com.badlogic.gdx.math.Vector3.dst(
                                player.getState().position.x, player.getState().position.y, player.getState().position.z,
                                targetX, targetY, targetZ);
                        if (dist > 200.0f) {
                            InteractionResponse resp = new InteractionResponse();
                            resp.success = false;
                            resp.message = "Too far from customer! (" + (int)dist + "m)";
                            conn.sendTCP(resp);
                            return;
                        }
                    }

                    InteractionResponse resp = new InteractionResponse();
                    resp.id = player.getId();
                    resp.targetId = req.targetId;
                    resp.success = true;
                    resp.uiType = com.futurecity.shared.enums.InteractionUIType.HANDOVER_ACTION;
                    resp.displayName = (targetPlayerForOrder != null) ? targetPlayerForOrder.getUsername() : activeOrder.destinationName;
                    resp.activeOrderId = activeOrder.orderId;
                    resp.playerCoins = playerRepo.getPlayerCoins(player.getDbUserId());
                    resp.isPlayerTarget = (targetPlayerForOrder != null);
                    resp.message = "Order #" + activeOrder.orderId + ": " + activeOrder.itemName;
                    conn.sendTCP(resp);
                    return;
                }
            }
        }

        ServerInteractable target = worldService.getObjectById(req.targetId);
        if (target != null) {
            InteractionResponse resp = interactionService.processInteraction(player, target);
            conn.sendTCP(resp);
        } else if (req.targetId.startsWith("player_")) {
            try {
                int targetConnId = Integer.parseInt(req.targetId.replace("player_", ""));
                ServerPlayer targetPlayer = playerService.get(targetConnId);
                if (targetPlayer != null) {
                    float dist = com.badlogic.gdx.math.Vector3.dst(
                            player.getState().position.x, player.getState().position.y, player.getState().position.z,
                            targetPlayer.getState().position.x, targetPlayer.getState().position.y, targetPlayer.getState().position.z);
                    InteractionResponse resp = new InteractionResponse();
                    resp.id = player.getId();
                    resp.targetId = req.targetId;
                    if (dist > 200.0f) {
                        resp.success = false;
                        resp.message = "Too far!";
                    } else {
                        resp.success = true;
                        resp.message = "Success!";
                        resp.uiType = com.futurecity.shared.enums.InteractionUIType.PLAYER_INTERACT;
                        resp.displayName = targetPlayer.getUsername();
                        resp.playerCoins = playerRepo.getPlayerCoins(player.getDbUserId());
                        
                        OrderResponse targetNotify = new OrderResponse();
                        targetNotify.success = true;
                        targetNotify.message = player.getUsername() + " is interacting with you!";
                        targetPlayer.getConnection().sendTCP(targetNotify);
                    }
                    conn.sendTCP(resp);
                }
            } catch (Exception e) {}
        } else {
            InteractionResponse resp = new InteractionResponse();
            resp.id = player.getId();
            resp.targetId = req.targetId;
            resp.success = true;
            resp.message = "Success!";
            resp.uiType = com.futurecity.shared.enums.InteractionUIType.HOUSE_INFO;
            resp.displayName = req.targetId;
            resp.playerCoins = playerRepo.getPlayerCoins(player.getDbUserId());
            conn.sendTCP(resp);
        }
    }

    public void handleBuy(Connection conn, BuyRequest req) {
        if (req.shopId != null && req.shopId.length() > 64) {
             System.err.println("[SECURITY] Blocked oversized shopId from ConnID=" + conn.getID());
             conn.close();
             return;
        }

        ServerPlayer player = playerService.get(conn.getID());
        if (player == null) return;
        int result = shopRepo.buyItem(player.getDbUserId(), req.shopId, req.itemId, playerRepo, inventoryRepo, transactionRepo);
        BuyResponse resp = new BuyResponse();
        if (result >= 0) {
            resp.success = true;
            resp.remainingCoins = result;
            player.setCredits(result);
        } else {
            resp.success = false;
            resp.remainingCoins = playerRepo.getPlayerCoins(player.getDbUserId());
        }
        resp.message = result == -1 ? "Out of stock!" : result == -2 ? "Not enough coins!" : "Purchase successful!";
        
        conn.sendTCP(resp);
    }

    public void handleSubscribe(Connection conn, ShopSubscribeRequest req) {
        if (req.shopId != null && req.shopId.length() > 64) {
             System.err.println("[SECURITY] Blocked oversized shopId from ConnID=" + conn.getID());
             conn.close();
             return;
        }

        ServerPlayer player = playerService.get(conn.getID());
        if (player == null) return;
        boolean ok;
        if (req.subscribe) ok = subscriptionRepo.subscribeToShop(player.getDbUserId(), req.shopId);
        else ok = subscriptionRepo.unsubscribeFromShop(player.getDbUserId(), req.shopId);
        ShopSubscribeResponse resp = new ShopSubscribeResponse();
        resp.success = ok;
        resp.subscribed = req.subscribe;
        conn.sendTCP(resp);
    }
}
