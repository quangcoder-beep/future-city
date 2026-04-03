package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.OrderRepository;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.packets.request.DriverPendingOrdersRequest;
import com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý các yêu cầu từ App Tài Xế (Driver App) trên Client.
 */
@Component
public class DriverPacketHandler {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private PlayerService playerService;

    public void handlePendingOrdersRequest(Connection conn, DriverPendingOrdersRequest req) {
        System.out.println("[DEBUG-DRIVER] handlePendingOrdersRequest from ConnID=" + conn.getID());
        ServerPlayer player = playerService.get(conn.getID());
        if (player == null) {
            System.out.println("[DEBUG-DRIVER] FAILED: Player null for ConnID=" + conn.getID());
            return;
        }

        List<OrderRepository.OrderData> pendingList = orderRepo.getAllPendingOrders();
        System.out.println("[DEBUG-DRIVER] Found " + pendingList.size() + " pending orders.");

        DriverPendingOrdersResponse response = new DriverPendingOrdersResponse();
        response.success = true;
        response.orders = new ArrayList<>();

        for (OrderRepository.OrderData od : pendingList) {
            DriverPendingOrdersResponse.OrderInfo info = new DriverPendingOrdersResponse.OrderInfo();
            info.orderId = od.orderId;
            info.shopName = od.shopName != null ? od.shopName : od.shopId;
            info.itemName = od.itemName;
            info.reward = od.reward;
            info.destinationName = od.destinationName;
            info.shopX = od.shopX;
            info.shopY = od.shopY;
            info.shopZ = od.shopZ;
            info.destX = od.destX;
            info.destY = od.destY;
            info.destZ = od.destZ;
            

            response.orders.add(info);
        }

        System.out.println("[DEBUG-DRIVER] Sending DriverPendingOrdersResponse with " + response.orders.size() + " orders to " + player.getUsername());
        conn.sendTCP(response);
    }
}
