package com.example.server_spring.services;

import com.example.server_spring.handler.KryoNetworkHandler;
import com.example.server_spring.entity.ServerPlayer;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.packets.resonse.BatchMovementResponse;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

/**
 * Vòng lặp game chính — thay thế hoàn toàn ServerLoop.render().
 * Chạy @Scheduled 60Hz (16ms/tick).
 */
@Service
public class GameLoopService {

    @Autowired
    private KryoNetworkHandler networkHandler;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PhysicsService physicsService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private MapService mapService;
    @Autowired
    private WorldService worldService;
    @Autowired
    private AoIGrid aoiGrid; // AoI: chỉ broadcast nearby players

    private long lastTickTime;
    private boolean initialized = false;
    private int tickCount = 0; // Dùng để tách tickrate broadcast

    /**
     * Khởi tạo Game Loop sau khi Spring Beans đã sẵn sàng.
     * Nạp bản đồ và đặt thời gian bắt đầu.
     */
    @PostConstruct
    public void init() {
        loadMap();
        lastTickTime = System.nanoTime();
        initialized = true;
        System.out.println("[LOOP] Game loop ready!");
    }

    /**
     * Nạp dữ liệu bản đồ từ file .glb.
     * Tách các vật cản (Obstacles) và mạng lưới tìm đường (NavGrid).
     */
    private void loadMap() {
        try {
            String path = "models/currentcity.glb";
            com.badlogic.gdx.files.FileHandle file = com.badlogic.gdx.Gdx.files.internal(path);

            if (!file.exists()) {
                path = "assets/models/currentcity.glb";
                file = com.badlogic.gdx.Gdx.files.internal(path);
            }

            if (!file.exists()) {
                throw new java.io.FileNotFoundException("Không tìm thấy file: " + path);
            }

            System.out.println("[MAP] Found map file at: " + file.path());
            SceneAsset mapAsset = new net.mgsx.gltf.loaders.glb.GLBLoader().load(file);

            float scale = GameConstants.MAP_SCALE;
            worldService.loadFromMap(mapAsset);
            mapService.load(mapAsset, scale);

            // Cấu hình OrderService
            String[] shopIds = worldService.getAllShopIds();
            float[][] destinations = worldService.getAllDeliveryDestinations();

            if (destinations.length == 0) {
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.err.println("[MAP-CRITICAL] NO DELIVERY DESTINATIONS FOUND IN GLB FILE!");
                System.err.println("[MAP-CRITICAL] Check your Blender node names (building, house, npc).");
                System.err.println("[MAP-CRITICAL] Using hardcoded fallback destinations. These may be OUT OF BOUNDS!");
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                destinations = new float[][] {
                        { 500, 50, 100 }, { 600, 50, -50 }, { 400, 50, 200 },
                        { 800, 50, -300 }, { 1200, 50, 400 }, { -200, 50, 600 },
                        { 1500, 50, -800 }, { 100, 50, 1200 }, { -600, 50, -200 },
                        { 2000, 50, 100 }, { 300, 50, -1500 }, { -1000, 50, 400 }
                };
            }

            orderService.configure(shopIds, destinations);

            System.out.println("[LOOP] Map loaded successfully!");
        } catch (Exception e) {
            System.err.println("[ERROR] MAP LOAD ERROR: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            System.err.println("[LOOP] WARNING: Could not load map! Using fallback.");
            mapService.loadFallback();
        }
    }

    /**
     * VÒNG LẶP CHÍNH — chạy mỗi 16ms (≈60 FPS).
     * Physics + Packet: 60Hz
     * Broadcast: 20Hz (mỗi 3 tick) — client có lerp nên đủ mượt
     */
    @Scheduled(fixedRate = 16)
    public void tick() {
        if (!initialized)
            return;

        // Tính delta (giây)
        long now = System.nanoTime();
        float delta = (now - lastTickTime) / 1_000_000_000f;
        lastTickTime = now;
        delta = Math.min(delta, 0.05f); // cap 50ms
        tickCount++;

        // 1. Xử lý packet từ client — 60Hz (responsive)
        networkHandler.processQueue(delta);

        // 2. Vật lý (input → velocity → collision → gravity) — 60Hz (chính xác)
        Collection<ServerPlayer> players = playerService.getAll();
        physicsService.update(delta, players);

        // 3. Đơn hàng NPC — timer nội bộ tự điều tiết
        orderService.update(delta);

        // 4. Broadcast 20Hz — giảm 3x bandwidth, client lerp bù lag
        if (tickCount % 3 == 0) {
            broadcastWorldState(players);
        }
    }

    /**
     * Gửi batch state đến mỗi client.
     * AoI: mỗi client chỉ nhận player gần mình (~400m).
     * Batch: tất cả ghom vào 1 gói UDP duy nhất.
     */
    private void broadcastWorldState(Collection<ServerPlayer> players) {
        if (players.isEmpty())
            return;

        // Cập nhật vị trí tất cả player vào AoI grid
        for (ServerPlayer p : players) {
            aoiGrid.updatePlayer(p);
        }

        // Mỗi receiver chỉ nhận state của nearby players
        for (ServerPlayer receiver : players) {
            if (receiver.getConnection() == null)
                continue;

            List<ServerPlayer> nearby = aoiGrid.getNearbyPlayers(receiver);
            if (nearby.isEmpty())
                continue;

            // Gom tất cả nearby states vào 1 batch
            BatchMovementResponse batch = new BatchMovementResponse();
            batch.count = nearby.size();
            batch.states = new PlayerState[batch.count];
            for (int i = 0; i < batch.count; i++) {
                batch.states[i] = nearby.get(i).getState();
            }

            try {
                receiver.getConnection().sendUDP(batch);
            } catch (Exception e) {
                // Client đã disconnect, bỏ qua
            }
        }
    }
}
