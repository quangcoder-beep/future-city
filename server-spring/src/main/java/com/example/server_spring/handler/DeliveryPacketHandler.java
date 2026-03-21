package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.OrderRepository;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.packets.request.*;
import com.futurecity.shared.packets.resonse.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles delivery-related packets.
 * - Accept order.
 * - Complete delivery.
 * - Cancel delivery.
 */
@Component
public class DeliveryPacketHandler {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private com.example.server_spring.repository.PlayerRepository playerRepo;
    @Autowired
    private com.example.server_spring.services.WorldService worldService;

    /**
     * Accepts a delivery order.
     * Assigns courier = current player, returns delivery coordinates.
     */
    public void handleAcceptDelivery(Connection conn, DeliveryAcceptRequest req) {
        System.out.println("[SERVER] Accept request received from Shipper. Order ID: " + req.orderId);
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null)
            return;

        boolean ok = orderRepo.acceptDelivery(player.getDbUserId(), req.orderId);

        DeliveryAcceptResponse resp = new DeliveryAcceptResponse();
        resp.success = ok;
        resp.message = ok ? "Order accepted!" : "Cannot accept order!";

        // If accepted successfully, return destination info
        if (ok) {
            OrderRepository.OrderData od = orderRepo.getOrderData(req.orderId);
            if (od != null) {
                resp.orderId = od.orderId;

                // Return Shop location for Pickup (PENDING_PICKUP status)
                com.example.server_spring.entity.ServerInteractable shop = worldService.getObjectById(od.shopId);
                if (shop != null) {
                    resp.destX = shop.getPosition().x;
                    resp.destY = shop.getPosition().y;
                    resp.destZ = shop.getPosition().z;
                    resp.destinationName = "Shop (Pickup)";
                } else {
                    resp.destX = od.destX;
                    resp.destY = od.destY;
                    resp.destZ = od.destZ;
                    resp.destinationName = od.destinationName;
                }
            }
        }
        conn.sendTCP(resp);
    }

    /**
     * Completes a delivery.
     * Grants reward to courier.
     */
    public void handleCompleteDelivery(Connection conn, DeliveryCompleteRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null)
            return;

        int reward = orderRepo.completeDelivery(player.getDbUserId(), req.orderId);

        DeliveryCompleteResponse resp = new DeliveryCompleteResponse();
        resp.success = reward >= 0;
        resp.reward = Math.max(reward, 0);
        resp.totalCoins = playerRepo.getPlayerCoins(player.getDbUserId());
        resp.message = reward >= 0 ? "Delivery successful! +" + reward : "Error!";
        
        conn.sendTCP(resp);
    }

    /**
     * Cancels a delivery.
     * Penalty of 10% reward applies, order returns to PENDING.
     */
    public void handleCancelDelivery(Connection conn, DeliveryCancelRequest req) {
        System.out.println("[SERVER] Cancel request received from Shipper. Order ID: " + req.orderId);
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null)
            return;

        int penalty = orderRepo.cancelDelivery(player.getDbUserId(), req.orderId);

        DeliveryCancelResponse resp = new DeliveryCancelResponse();
        resp.success = penalty >= 0;
        resp.penalty = Math.max(penalty, 0);
        resp.remainingCoins = playerRepo.getPlayerCoins(player.getDbUserId());
        resp.message = penalty >= 0 ? "Canceled! Penalty: " + penalty : "Error!";

        conn.sendTCP(resp);
    }

    /**
     * Shipper confirms item pickup at the Shop (From PickupPanel).
     */
    public void handleDeliveryPickup(Connection conn, DeliveryPickupRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null)
            return;

        OrderRepository.OrderData od = orderRepo.getOrderData(req.orderId);
        if (od == null)
            return;

        boolean ok = orderRepo.pickupDelivery(req.orderId);
        System.out.println("[SERVER] Shipper confirmed pickup for order #" + req.orderId + ". Result: " + ok);
        
        DeliveryCompleteResponse resp = new DeliveryCompleteResponse();
        resp.success = ok;
        resp.totalCoins = playerRepo.getPlayerCoins(player.getDbUserId());
        resp.message = ok ? "Pickup confirmed! Starting delivery." : "Pickup error!";
        conn.sendTCP(resp);

        if (ok) {
            // Notify the Buyer
            if ("PLAYER".equals(od.buyerType)) {
                int buyerId = Integer.parseInt(od.buyerRefId);
                ServerPlayer buyer = playerService.getAllPlayers().stream()
                        .filter(p -> p.getDbUserId() == buyerId)
                        .findFirst().orElse(null);
                if (buyer != null && buyer.getConnection() != null) {
                    DeliveryCompleteResponse buyerNotify = new DeliveryCompleteResponse();
                    buyerNotify.success = true;
                    buyerNotify.totalCoins = playerRepo.getPlayerCoins(buyer.getDbUserId());
                    buyerNotify.message = "Shipper has picked up your order and is on the way!";
                    buyer.getConnection().sendTCP(buyerNotify);
                }
            }

            // Send new GPS (delivery point)
            DeliveryGPSUpdate gpsUpdate = new DeliveryGPSUpdate();
            gpsUpdate.orderId = od.orderId;
            gpsUpdate.destX = od.destX;
            gpsUpdate.destY = od.destY;
            gpsUpdate.destZ = od.destZ;
            gpsUpdate.buyerName = od.destinationName;
            conn.sendTCP(gpsUpdate);
        }
    }

    /**
     * Shipper requests signature (From HandoverPanel).
     */
    public void handleDeliveryHandover(Connection conn, DeliveryHandoverRequest req) {
        try {
            ServerPlayer player = playerService.get(conn.getID());
            if (player == null) return;

            OrderRepository.OrderData od = orderRepo.getOrderData(req.orderId);
            if (od == null) return;
            
            if (!"DELIVERING".equals(od.status)) return;

            if ("PLAYER".equals(od.buyerType)) {
                if (od.buyerRefId == null || od.buyerRefId.isEmpty()) return;
                
                int buyerId;
                try {
                    buyerId = Integer.parseInt(od.buyerRefId);
                } catch (NumberFormatException e) {
                    return;
                }
                
                java.util.Collection<ServerPlayer> allPlayers = playerService.getAllPlayers();
                
                ServerPlayer buyer = allPlayers.stream()
                        .filter(p -> p != null && p.getDbUserId() == buyerId)
                        .findFirst().orElse(null);

                if (buyer != null && buyer.getConnection() != null) {
                    DeliveryConfirmRequest confirmReq = new DeliveryConfirmRequest();
                    confirmReq.orderId = od.orderId;
                    confirmReq.courierName = player.getUsername();
                    confirmReq.itemName = od.itemName;
                    buyer.getConnection().sendTCP(confirmReq);

                    DeliveryCompleteResponse resp = new DeliveryCompleteResponse();
                    resp.success = true;
                    resp.totalCoins = playerRepo.getPlayerCoins(player.getDbUserId());
                    resp.message = "Delivery confirmation sent to " + buyer.getUsername() + ". Waiting...";
                    conn.sendTCP(resp);
                } else {
                    DeliveryCompleteResponse resp = new DeliveryCompleteResponse();
                    resp.success = false;
                    resp.totalCoins = playerRepo.getPlayerCoins(player.getDbUserId());
                    resp.message = "Buyer is currently offline!";
                    conn.sendTCP(resp);
                }
            } else {
                // NPC fallback
                // Check distance before completion
                float dist = com.badlogic.gdx.math.Vector3.dst(
                        player.getState().position.x, player.getState().position.y, player.getState().position.z,
                        od.destX, od.destY, od.destZ);
                
                if (dist > 200.0f) {
                    DeliveryCompleteResponse resp = new DeliveryCompleteResponse();
                    resp.success = false;
                    resp.totalCoins = playerRepo.getPlayerCoins(player.getDbUserId());
                    resp.message = "Too far! Please stand near the destination.";
                    conn.sendTCP(resp);
                    return;
                }

                int reward = orderRepo.completeDelivery(player.getDbUserId(), od.orderId);
                DeliveryCompleteResponse resp = new DeliveryCompleteResponse();
                resp.success = reward >= 0;
                resp.reward = Math.max(reward, 0);
                resp.totalCoins = playerRepo.getPlayerCoins(player.getDbUserId());
                resp.message = reward >= 0 ? "Success!" : "Error!";

                conn.sendTCP(resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
