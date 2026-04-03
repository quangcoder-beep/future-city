package com.example.server_spring.services;

import com.badlogic.gdx.math.collision.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spatial Hash Grid cho obstacles — chia bản đồ thành các ô 50m.
 * Lookup O(1) thay vì duyệt toàn bộ danh sách O(N).
 *
 * Giảm collision test từ ~200 xuống ~5 mỗi lần check.
 */
public class SpatialHashGrid {

    private static final float CELL_SIZE = 50f;
    private final Map<Long, List<BoundingBox>> cells = new HashMap<>();

    /** Xóa toàn bộ dữ liệu (gọi khi load map mới) */
    public void clear() {
        cells.clear();
    }

    /** Đưa một BoundingBox vào tất cả ô mà nó giao */
    public void insert(BoundingBox box) {
        int minX = worldToCell(box.min.x);
        int maxX = worldToCell(box.max.x);
        int minZ = worldToCell(box.min.z);
        int maxZ = worldToCell(box.max.z);

        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                long key = cellKey(cx, cz);
                cells.computeIfAbsent(key, k -> new ArrayList<>()).add(box);
            }
        }
    }

    /**
     * Lấy danh sách obstacles có thể giao với vị trí (x, z).
     * Chỉ trả ô chứa điểm đó — O(1) lookup.
     */
    public List<BoundingBox> query(float x, float z) {
        long key = cellKey(worldToCell(x), worldToCell(z));
        List<BoundingBox> result = cells.get(key);
        return result != null ? result : java.util.Collections.emptyList();
    }

    private int worldToCell(float world) {
        return (int) Math.floor(world / CELL_SIZE);
    }

    /** Encode 2 int thành 1 long key — tránh tạo object cho Map key */
    private long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
