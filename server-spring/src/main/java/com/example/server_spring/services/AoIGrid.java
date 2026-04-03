package com.example.server_spring.services;

import com.example.server_spring.entity.ServerPlayer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Area of Interest Grid cho Player.
 * Chia bản đồ thành các ô 200m. Mỗi player chỉ nhận state của
 * những player nằm trong ô hiện tại và 8 ô lân cận (~600m bán kính).
 *
 * Giảm dữ liệu broadcast từ N² xuống N×K (K ≈ 10-20 player gần đó).
 */
@Service
public class AoIGrid {

    private static final float CELL_SIZE = 200f; // Mỗi ô 200m
    private static final int NEIGHBOR_RANGE = 1; // 1 ô xung quanh = bán kính ~400m

    // Map: cell_key → set players trong ô đó
    private final Map<Long, Set<ServerPlayer>> cells = new ConcurrentHashMap<>();
    // Map: player → cell key hiện tại (để xóa khỏi ô cũ khi di chuyển)
    private final Map<Integer, Long> playerCell = new ConcurrentHashMap<>();

    /**
     * Cập nhật vị trí của player trong grid.
     * Gọi mỗi lần chuẩn bị broadcast.
     */
    public void updatePlayer(ServerPlayer player) {
        float x = player.getState().position.x;
        float z = player.getState().position.z;
        long newKey = cellKey(worldToCell(x), worldToCell(z));

        Long oldKey = playerCell.get(player.getId());

        // Nếu chưa di chuyển sang ô mới → bỏ qua
        if (newKey == (oldKey != null ? oldKey : Long.MIN_VALUE)) return;

        // Xóa khỏi ô cũ
        if (oldKey != null) {
            Set<ServerPlayer> oldCell = cells.get(oldKey);
            if (oldCell != null) oldCell.remove(player);
        }

        // Thêm vào ô mới
        cells.computeIfAbsent(newKey, k -> ConcurrentHashMap.newKeySet()).add(player);
        playerCell.put(player.getId(), newKey);
    }

    /**
     * Trả về danh sách player gần với player truyền vào.
     * Bao gồm ô hiện tại + 8 ô lân cận.
     */
    public List<ServerPlayer> getNearbyPlayers(ServerPlayer player) {
        float x = player.getState().position.x;
        float z = player.getState().position.z;
        int cx = worldToCell(x);
        int cz = worldToCell(z);

        List<ServerPlayer> result = new ArrayList<>();
        for (int dx = -NEIGHBOR_RANGE; dx <= NEIGHBOR_RANGE; dx++) {
            for (int dz = -NEIGHBOR_RANGE; dz <= NEIGHBOR_RANGE; dz++) {
                Set<ServerPlayer> cell = cells.get(cellKey(cx + dx, cz + dz));
                if (cell != null) result.addAll(cell);
            }
        }
        return result;
    }

    /** Xóa player khỏi grid khi disconnect */
    public void removePlayer(ServerPlayer player) {
        Long key = playerCell.remove(player.getId());
        if (key != null) {
            Set<ServerPlayer> cell = cells.get(key);
            if (cell != null) cell.remove(player);
        }
    }

    private int worldToCell(float world) {
        return (int) Math.floor(world / CELL_SIZE);
    }

    private long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
