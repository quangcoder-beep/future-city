package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.OrderRepository;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.packets.resonse.DeliveryCompleteResponse;
import com.futurecity.shared.packets.resonse.DeliveryConfirmResponse;
import com.example.server_spring.repository.ShopRepository;
import com.example.server_spring.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handle response from Buyer (Accept/Reject delivery).
 */
@Component
public class DeliveryConfirmHandler {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private ShopRepository shopRepo;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepo;

    public void handle(Connection buyerConn, DeliveryConfirmResponse res) {
        System.out.println("[SERVER] Buyer response for Order #" + res.orderId + ": " + (res.accept ? "ACCEPTED" : "REJECTED"));
        
        OrderRepository.OrderData od = orderRepo.getOrderData(res.orderId);
        if (od == null) return;

        int courierDbId = orderRepo.getCourierId(res.orderId);
        if (courierDbId == -1) return;

        ServerPlayer courier = playerService.getAllPlayers().stream()
                .filter(p -> p.getDbUserId() == courierDbId)
                .findFirst().orElse(null);

        if (res.accept) {
            int reward = orderRepo.completeDelivery(courierDbId, res.orderId);
            
            if (courier != null && courier.getConnection() != null) {
                DeliveryCompleteResponse deliveryResp = new DeliveryCompleteResponse();
                deliveryResp.success = reward >= 0;
                deliveryResp.reward = Math.max(reward, 0);
                deliveryResp.totalCoins = playerRepo.getPlayerCoins(courierDbId);
                deliveryResp.message = reward >= 0 ? "Success! Buyer accepted. +" + reward + " Coins!" : "Completion Error!";
                courier.getConnection().sendTCP(deliveryResp);
            }

            int buyerDbId = Integer.parseInt(od.buyerRefId);
            DeliveryCompleteResponse buyerResp = new DeliveryCompleteResponse();
            buyerResp.success = reward >= 0;
            buyerResp.totalCoins = playerRepo.getPlayerCoins(buyerDbId);
            buyerResp.message = reward >= 0 ? "Success! Delivery received." : "Success!";
            buyerConn.sendTCP(buyerResp);
        } else {
            // Rejected
            System.out.println("[SERVER] Buyer REJECTED Order #" + res.orderId + ". Refunding...");
            shopRepo.updateStock(od.shopId, od.itemId, 1);
            orderRepo.updateOrderStatus(res.orderId, "REJECTED");
            
            // Refund Buyer (since they pay upfront now)
            if ("PLAYER".equals(od.buyerType)) {
                int bId = Integer.parseInt(od.buyerRefId);
                int refund = od.itemPrice + od.reward;
                int current = playerRepo.getPlayerCoins(bId);
                playerRepo.updateCoins(bId, current + refund);
            }
 
            if (courier != null && courier.getConnection() != null) {
                DeliveryCompleteResponse deliveryResp = new DeliveryCompleteResponse();
                deliveryResp.success = false;
                deliveryResp.totalCoins = playerRepo.getPlayerCoins(courierDbId);
                deliveryResp.message = "Customer REJECTED the delivery.";
                courier.getConnection().sendTCP(deliveryResp);
            }
 
            int bDbId = Integer.parseInt(od.buyerRefId);
            DeliveryCompleteResponse buyerResp = new DeliveryCompleteResponse();
            buyerResp.success = true;
            buyerResp.totalCoins = playerRepo.getPlayerCoins(bDbId);
            buyerResp.message = "Delivery REJECTED. Coins refunded.";
            buyerConn.sendTCP(buyerResp);
        }
    }
}
