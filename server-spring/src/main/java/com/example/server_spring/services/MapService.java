package com.example.server_spring.services;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.systems.NavigationGrid;
import com.futurecity.shared.utils.MapLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý vật cản và NavigationGrid của bản đồ.
 */
@Service
public class MapService {

    private final List<BoundingBox> obstacles = new ArrayList<>();
    private final BoundingBox playerBox = new BoundingBox();
    private final Vector3 tmpMin = new Vector3(); // Cache — tránh new Vector3 mỗi tick
    private final Vector3 tmpMax = new Vector3();
    private final SpatialHashGrid spatialGrid = new SpatialHashGrid(); // O(1) lookup
    private NavigationGrid navGrid;

    /** Nạp vật cản + NavGrid từ file map GLB */
    /**
     * Nạp dữ liệu vật cản và mạng lưới tìm đường (Navigation Grid) từ file bản đồ
     * GLB.
     */
    public void load(SceneAsset mapAsset, float scale) {
        obstacles.clear();
        spatialGrid.clear();
        MapLoader.MapData mapData = MapLoader.extractMapData(mapAsset, scale);
        obstacles.addAll(mapData.obstacles);
        // Xây dựng spatial grid từ obstacles — chỉ làm 1 lần khi load map
        for (BoundingBox bb : obstacles)
            spatialGrid.insert(bb);

        BoundingBox mapBounds = mapData.fullBounds;
        if (!mapBounds.isValid()) {
            mapBounds = new BoundingBox();
            for (BoundingBox bb : obstacles)
                mapBounds.ext(bb);
            for (MapLoader.RoadData rd : mapData.roads)
                mapBounds.ext(rd.bounds);
        }

        // ĐẢM BẢO NavGrid bao phủ cả các điểm giao hàng vừa tìm thấy
        for (float[] dest : worldService.getAllDeliveryDestinations()) {
            mapBounds.ext(dest[0], dest[1], dest[2]);
        }

        float gridSize = 2.0f;
        int gridW = (int) (mapBounds.getWidth() / gridSize) + 40;
        int gridH = (int) (mapBounds.getDepth() / gridSize) + 40;
        navGrid = new NavigationGrid(gridW, gridH, gridSize, mapBounds.min.x - 20, mapBounds.min.z - 20);

        int roadCount = 0;
        for (MapLoader.RoadData rd : mapData.roads) {
            bakeToGrid(rd.bounds, NavigationGrid.ROAD);
            roadCount++;
        }

        System.out.println("[MAP] Loaded " + obstacles.size() + " obstacles, " + roadCount + " road nodes. NavGrid "
                + gridW + "x" + gridH);
        System.out.println("[MAP] Map Boundaries: Min(" + mapBounds.min.x + "," + mapBounds.min.z + ") Max("
                + mapBounds.max.x + "," + mapBounds.max.z + ")");
    }

    /** Fallback: dùng CollisionData cứng khi không có file map */
    public void loadFallback() {
        obstacles.clear();
        obstacles.addAll(com.futurecity.shared.collision.CollisionData.getMapObstacles());
        System.out.println("[MAP] Fallback: " + obstacles.size() + " obstacles from CollisionData.");
    }

    @Autowired
    private WorldService worldService;

    private void bakeToGrid(BoundingBox bounds, byte type) {
        int minX = navGrid.worldToGridX(bounds.min.x), maxX = navGrid.worldToGridX(bounds.max.x);
        int minZ = navGrid.worldToGridZ(bounds.min.z), maxZ = navGrid.worldToGridZ(bounds.max.z);
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                navGrid.setCell(x, z, type);
    }

    public NavigationGrid getNavGrid() {
        return navGrid;
    }

    /**
     * Tìm đường đi ngắn nhất giữa hai điểm 3D.
     * 
     * @param start Điểm bắt đầu.
     * @param end   Điểm kết thúc.
     * @return Danh sách các điểm tọa độ, hoặc null nếu không tìm thấy.
     */
    public List<Vector3> findPath(Vector3 start, Vector3 end) {
        if (navGrid == null || start == null || end == null) {
            return null;
        }
        return navGrid.findPath(start.x, start.z, end.x, end.z);
    }

    /** Xử lý va chạm cho player: nếu đâm tường → reset velocity */
    /**
     * Kiểm tra va chạm và ngăn chặn người chơi đi xuyên tường.
     * Nếu phát hiện va chạm, vị trí người chơi sẽ được đẩy lùi về trước đó.
     */
    public void applyCollision(PlayerState state, float delta) {
        float savedX = state.position.x;
        state.position.x += state.velocity.x * delta;
        if (isColliding(state.position)) {
            state.position.x = savedX;
            state.velocity.x = 0;
        }

        float savedZ = state.position.z;
        state.position.z += state.velocity.z * delta;
        if (isColliding(state.position)) {
            state.position.z = savedZ;
            state.velocity.z = 0;
        }
    }

    private boolean isColliding(Vector3 pos) {
        float halfW = (GameConstants.PLAYER_WIDTH * GameConstants.WORLD_SCALE) * 0.6f;
        float halfD = (GameConstants.PLAYER_DEPTH * GameConstants.WORLD_SCALE) * 0.6f;
        float height = GameConstants.PLAYER_HEIGHT * GameConstants.WORLD_SCALE;
        // Tái sử dụng tmpMin/tmpMax — tránh tạo 24,000 Vector3 garbage/giây
        tmpMin.set(pos.x - halfW, pos.y + 0.5f, pos.z - halfD);
        tmpMax.set(pos.x + halfW, pos.y + height, pos.z + halfD);
        playerBox.set(tmpMin, tmpMax);
        // Spatial hash: chỉ test obstacles trong ô lân cận — O(~5) thay vì O(200)
        for (BoundingBox bb : spatialGrid.query(pos.x, pos.z))
            if (playerBox.intersects(bb))
                return true;
        return false;
    }
}
