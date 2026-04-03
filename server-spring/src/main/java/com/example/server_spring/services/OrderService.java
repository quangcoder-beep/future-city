package com.example.server_spring.services;

import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.OrderRepository;
import com.example.server_spring.repository.ShopRepository;
import com.example.server_spring.repository.ShopRepository.ShopItemData;
import com.example.server_spring.repository.SubscriptionRepository;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Automatically creates NPC orders and checks for timeouts.
 */
@Service
public class OrderService {

    private static final float MIN_INTERVAL = 30f;
    private static final float MAX_INTERVAL = 90f;
    private static final float TIMEOUT_INTERVAL = 30f;

    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private ShopRepository shopRepo;
    @Autowired
    private SubscriptionRepository subscriptionRepo;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private WorldService worldService;

    private final Random random = new Random();
    private float orderTimer = 10f; // First order after 10s
    private float timeoutTimer = TIMEOUT_INTERVAL;
    private float gpsTimer = 2.0f; // Update GPS every 2s

    private String[] shopIds;
    private float[][] destinations;

    /**
     * Set up the list of shops and delivery destinations.
     * Called after the map is successfully loaded.
     */
    public void configure(String[] shopIds, float[][] destinations) {
        this.shopIds = shopIds;
        this.destinations = destinations;
    }

    /**
     * Updates order logic every tick: create new orders and check for expired ones.
     */
    public void update(float delta) {
        // Auto-generate orders
        orderTimer -= delta;
        if (orderTimer <= 0) {
            orderTimer = MIN_INTERVAL + random.nextFloat() * (MAX_INTERVAL - MIN_INTERVAL);
            generateRandomOrder();
        }

        // Timeout check
        timeoutTimer -= delta;
        if (timeoutTimer <= 0) {
            timeoutTimer = TIMEOUT_INTERVAL;
            orderRepo.checkTimeouts();
        }

        // --- REAL-TIME GPS UPDATES ---
        // Send Buyer position (if Player) to the assigned Shipper
        updateActiveDeliveryGPS(delta);
    }

    private void updateActiveDeliveryGPS(float delta) {
        gpsTimer -= delta;
        if (gpsTimer > 0) return;
        gpsTimer = 2.0f; // Reset

        // Tối ưu N+1: Query 1 lần duy nhất lấy toàn bộ đơn hàng đang giao
        List<OrderRepository.OrderData> allActive = orderRepo.getAllActiveDeliveries();
        if (allActive.isEmpty()) return;

        // Group orders by CourierID for efficient broad cast
        java.util.Map<Integer, List<OrderRepository.OrderData>> courierOrders = new java.util.HashMap<>();
        for (OrderRepository.OrderData od : allActive) {
            courierOrders.computeIfAbsent(od.courierId, k -> new java.util.ArrayList<>()).add(od);
        }

        // Broadcast cho từng courier (chạy nhanh O(N) thay vì O(N^2))
        for (ServerPlayer courier : playerService.getAll()) {
            List<OrderRepository.OrderData> deliveries = courierOrders.get(courier.getDbUserId());
            if (deliveries == null || courier.getConnection() == null) continue;

            for (OrderRepository.OrderData od : deliveries) {
                if ("PLAYER".equals(od.buyerType)) { 
                    try {
                        int buyerId = Integer.parseInt(od.buyerRefId);
                        ServerPlayer buyer = playerService.getAllPlayers().stream()
                                .filter(p -> p.getDbUserId() == buyerId)
                                .findFirst().orElse(null);
                        
                        if (buyer != null) {
                            com.futurecity.shared.packets.resonse.DeliveryGPSUpdate gpsUpdate = new com.futurecity.shared.packets.resonse.DeliveryGPSUpdate();
                            gpsUpdate.orderId = od.orderId;
                            gpsUpdate.destX = buyer.getState().position.x;
                            gpsUpdate.destY = buyer.getState().position.y;
                            gpsUpdate.destZ = buyer.getState().position.z;
                            gpsUpdate.buyerName = buyer.getUsername();

                            courier.getConnection().sendUDP(gpsUpdate); // Dùng UDP để giảm overhead
                        }
                    } catch (Exception e) {}
                }
            }
        }
    }

    private void generateRandomOrder() {
        if (shopIds == null || shopIds.length == 0)
            return;

        String shopId = shopIds[random.nextInt(shopIds.length)];
        List<ShopItemData> items = shopRepo.getShopItems(shopId);
        if (items.isEmpty())
            return;

        ShopItemData item = items.get(random.nextInt(items.size()));

        float destX = 0, destY = 0, destZ = 0;
        if (destinations != null && destinations.length > 0) {
            float[] d = destinations[random.nextInt(destinations.length)];
            destX = d[0] + (random.nextFloat() * 30f - 15f); // +/- 15m jitter
            destY = d.length > 1 ? d[1] : 0;
            destZ = (d.length > 2 ? d[2] : 0) + (random.nextFloat() * 30f - 15f); // +/- 15m jitter
        }

        // Calculate distance
        float shopX = 0, shopZ = 0;
        com.example.server_spring.entity.ServerInteractable shopObj = worldService.getObjectById(shopId);
        if (shopObj != null) {
            shopX = shopObj.getPosition().x;
            shopZ = shopObj.getPosition().z;
        }
        float distance = com.badlogic.gdx.math.Vector2.dst(shopX, shopZ, destX, destZ);

        // Delivery fee = 20 + 10% item price + (distance / 10) + r(10)
        int reward = 20 + (int) (item.price * 0.1f) + (int) (distance / 10f) + random.nextInt(10);

        int orderId = orderRepo.createOrder(shopId, item.itemId, item.price,
                "NPC", "npc_auto_" + System.currentTimeMillis(), reward, destX, destY, destZ);

        if (orderId > 0) {
            // NPC ORDER -> Deduct stock
            shopRepo.updateStock(shopId, item.itemId, -1);
            System.out.println("[ORDER-NPC] Created Order #" + orderId + " | Item: " + item.itemName + " | Reward: " + reward);
            System.out.println("   [+] Destination set to: (" + (int)destX + ", " + (int)destZ + ") from " + (destinations != null ? destinations.length : 0) + " available points.");
            notifySubscribers(orderId, shopId, item.itemName, reward, "NPC (" + (int) destX + "," + (int) destY + ")");
        }
    }

    public void notifySubscribers(int orderId, String shopId, String itemName, int reward, String destination) {
        List<Integer> subs = subscriptionRepo.getShopSubscribers(shopId);
        if (subs.isEmpty())
            return;

        NewOrderNotification notif = new NewOrderNotification();
        notif.orderId = orderId;
        notif.shopId = shopId;
        notif.shopName = shopId;
        notif.itemName = itemName;
        notif.reward = reward;
        notif.destination = destination;

        for (ServerPlayer p : playerService.getAll()) {
            if (subs.contains(p.getDbUserId())) {
                try {
                    p.getConnection().sendTCP(notif);
                } catch (Exception e) {
                    /* disconnected */ }
            }
        }
    }
}
