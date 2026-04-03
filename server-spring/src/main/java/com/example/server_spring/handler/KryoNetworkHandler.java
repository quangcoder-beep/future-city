package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.PlayerRepository;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.packets.request.*;
import com.futurecity.shared.packets.PlayerLeft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * KryoNet Packet Router.
 * Responsibilities:
 * 1. Receive packets from Client (on KryoNet thread).
 * 2. Put them into a queue for thread-safe processing.
 * 3. Classify and delegate to corresponding child Handlers.
 *
 * Does NOT contain processing logic — all logic resides in child Handlers:
 * - AuthPacketHandler: Login, movement
 * - ShopPacketHandler: Interaction, purchasing, subscription
 * - DeliveryPacketHandler: Accept order, complete, cancel delivery
 * - InventoryPacketHandler: View inventory
 * - PathfindingPacketHandler: Pathfinding (GPS)
 */
@Component
public class KryoNetworkHandler implements Listener {

    // ===== Child Handlers =====
    @Autowired
    private AuthPacketHandler authHandler;
    @Autowired
    private ShopPacketHandler shopHandler;
    @Autowired
    private DeliveryPacketHandler deliveryHandler;
    @Autowired
    private InventoryPacketHandler inventoryHandler;
    @Autowired
    private PathfindingPacketHandler pathfindingHandler;

    @Autowired
    private DeliveryConfirmHandler deliveryConfirmHandler;

    @Autowired
    private OrderPacketHandler orderPacketHandler;

    @Autowired
    private DriverPacketHandler driverHandler;

    // ===== Services for connect/disconnect =====
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerRepository playerRepo;

    @Autowired
    private com.example.server_spring.managers.ServerStatsManager serverStatsManager;

    // ===== Packet Queue =====

    /** Wrapper for Connection + Packet to put into the queue. */
    private static class PacketWrapper {
        final Connection conn;
        final Object packet;

        PacketWrapper(Connection c, Object p) {
            this.conn = c;
            this.packet = p;
        }
    }

    /**
     * Thread-safe queue for packets — ensures thread-safety between KryoNet thread
     * and Game Loop.
     */
    private final Queue<PacketWrapper> packetQueue = new ConcurrentLinkedQueue<>();

    /**
     * Task queue to run on the Main Thread (e.g., sending packets from another
     * thread).
     */
    private final Queue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();

    // ===== SPAM FILTER (Rate Limiting) =====
    private final java.util.Map<Integer, Integer> packetCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private long lastRateLimitReset = System.currentTimeMillis();
    private static final int MAX_PACKETS_PER_SEC = 150;

    // ===== KryoNet Callbacks (Runs on its own KryoNet thread) =====

    /** Callback when a new Client connects. */
    @Override
    public void connected(Connection connection) {
        System.out.println("[NET] New connection: ID=" + connection.getID());
    }

    /**
     * Callback when a Client disconnects.
     * Saves player position to DB and notifies other players.
     */
    @Override
    public void disconnected(Connection connection) {
        try {
            ServerPlayer player = playerService.get(connection.getID());
            if (player != null) {
                // Save position before removal
                PlayerState state = player.getState();
                playerRepo.savePlayerPosition(player.getDbUserId(),
                        state.position.x, state.position.y, state.position.z);

                // Notify all other players
                PlayerLeft leftResp = new PlayerLeft();
                leftResp.id = player.getId();
                playerService.remove(player.getId());
                playerService.broadcastTCP(leftResp);

                System.out.println("[NET] " + player.getUsername() + " has disconnected.");
            }
        } catch (Exception e) {
            System.err.println("[ERROR-NET] Exception during disconnected() cleanup for ConnID=" + connection.getID() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Callback khi nhận gói tin từ Client.
     * Chỉ đưa vào queue — KHÔNG xử lý ở đây (thread-safe).
     */
    @Override
    public void received(Connection connection, Object packet) {
        if (serverStatsManager != null) serverStatsManager.incrementPacketCount();
        try {
            // 1. Rate Limiting (Anti-Spam)
            long now = System.currentTimeMillis();
            if (now - lastRateLimitReset > 1000) {
                packetCounts.clear();
                lastRateLimitReset = now;
            }

            int count = packetCounts.getOrDefault(connection.getID(), 0) + 1;
            if (count > MAX_PACKETS_PER_SEC) {
                if (count == MAX_PACKETS_PER_SEC + 1) { // Chỉ log 1 lần mỗi giây để tránh trôi Console
                    String msg = "Auto-kicked ConnID=" + connection.getID() + " for Packet Spam (" + count + " pkts/sec)";
                    System.err.println("[SECURITY] " + msg);
                    if (serverStatsManager != null) serverStatsManager.logDdos(msg);
                    handleDisconnect(connection);
                    connection.close();
                }
                return; // Drop packet lập tức, không cho lọt vào Queue GameLoop
            }
            packetCounts.put(connection.getID(), count);

            // 2. Chấp nhận nạp vào Queue
            packetQueue.offer(new PacketWrapper(connection, packet));
        } catch (Exception e) {
            System.err.println("[ERROR-NET] Fatal error in received() for ConnID=" + connection.getID() + ": " + e.getMessage());
            e.printStackTrace();
            handleDisconnect(connection);
            connection.close();
        }
    }

    // ===== Xử lý hàng đợi (gọi bởi GameLoopService mỗi tick) =====

    /**
     * Xử lý toàn bộ gói tin trong hàng đợi. Được gọi mỗi frame từ GameLoopService.
     */
    public void processQueue(float delta) {
        // Chạy các task trên main thread
        Runnable task;
        while ((task = mainThreadTasks.poll()) != null) {
            task.run();
        }

        // Xử lý gói tin
        PacketWrapper pw;
        while ((pw = packetQueue.poll()) != null) {
            handlePacket(pw.conn, pw.packet);
        }
    }

    // ===== Bộ phân loại gói tin (Router) =====

    /**
     * Phân loại gói tin và chuyển tiếp sang Handler con tương ứng.
     */
    private void handlePacket(Connection conn, Object packet) {
        try {
            if (packet instanceof com.esotericsoftware.kryonet.FrameworkMessage)
                return;

            // Chỉ log packet quan trọng — bỏ MovementRequest vì 6000 lần/giây
            if (!(packet instanceof MovementRequest)) {
                System.out.println("[NET] " + packet.getClass().getSimpleName() + " (ID=" + conn.getID() + ")");
            }

            // --- Xác thực & di chuyển ---
            if (packet instanceof LoginRequest req)
                authHandler.handleLogin(conn, req);
            else if (packet instanceof MovementRequest req)
                authHandler.handleMovement(conn, req);

            // --- Cửa hàng ---
            else if (packet instanceof InteractionRequest req) {
                System.out.println("[DEBUG-NET] Received InteractionRequest for target: " + req.targetId + " from ConnID="
                        + conn.getID());
                shopHandler.handleInteraction(conn, req);
            } else if (packet instanceof BuyRequest req)
                shopHandler.handleBuy(conn, req);
            else if (packet instanceof ShopSubscribeRequest req)
                shopHandler.handleSubscribe(conn, req);

            // --- Giao hàng ---
            else if (packet instanceof DeliveryAcceptRequest req)
                deliveryHandler.handleAcceptDelivery(conn, req);
            else if (packet instanceof DeliveryCompleteRequest req)
                deliveryHandler.handleCompleteDelivery(conn, req);
            else if (packet instanceof DeliveryCancelRequest req)
                deliveryHandler.handleCancelDelivery(conn, req);
            else if (packet instanceof DeliveryPickupRequest req)
                deliveryHandler.handleDeliveryPickup(conn, req);
            else if (packet instanceof DeliveryHandoverRequest req)
                deliveryHandler.handleDeliveryHandover(conn, req);
            else if (packet instanceof com.futurecity.shared.packets.resonse.DeliveryConfirmResponse req)
                deliveryConfirmHandler.handle(conn, req);

            // --- Giao dịch / Kho hàng ---
            else if (packet instanceof InventoryRequest req)
                inventoryHandler.handleInventory(conn, req);

            // --- App Tài Xế ---
            else if (packet instanceof com.futurecity.shared.packets.request.DriverPendingOrdersRequest req)
                driverHandler.handlePendingOrdersRequest(conn, req);

            // --- Tìm đường (GPS) ---
            else if (packet instanceof PathfindingRequest req)
                pathfindingHandler.handlePathfinding(conn, req);

            // --- Điện thoại / Đặt Hàng ---
            else if (packet instanceof com.futurecity.shared.packets.request.ShopListRequest req)
                orderPacketHandler.handleShopList(conn, req);
            else if (packet instanceof com.futurecity.shared.packets.request.OrderRequest req)
                orderPacketHandler.handleOrder(conn, req);
            else if (packet instanceof com.futurecity.shared.packets.request.OrderCancelRequest req)
                orderPacketHandler.handleOrderCancel(conn, req);
            else
                System.out.println("[WARNING] Unhandled packet type: " + packet.getClass().getSimpleName());

        } catch (Exception e) {
            String msg = "Lỗi logic xử lý Packet " + packet.getClass().getSimpleName() + " - ConnID=" + conn.getID() + " : " + e.getMessage();
            System.err.println("[ERROR-LOGIC] " + msg);
            if (serverStatsManager != null) {
                ServerPlayer p = playerService.get(conn.getID());
                serverStatsManager.logError("WARN", "NetworkHandler", msg, (p != null ? p.getDbUserId() : null));
            }
            e.printStackTrace();
            // Lỗi logic packet có thể do hack hoặc lỗi ngầm, ngắt kết nối an toàn.
            handleDisconnect(conn);
            conn.close();
        }
    }

    /** Cleanup khi player ngắt kết nối */
    private void handleDisconnect(Connection conn) {
        authHandler.handleDisconnect(conn);
    }

    /** Cập nhật Packet Rate mỗi giây lên Dashboard */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1000)
    public void updateMetrics() {
        if (serverStatsManager != null) {
            serverStatsManager.updatePacketRate();
        }
    }
}
