package com.example.server_spring.services;

import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.AdminRepository;
import com.futurecity.shared.packets.resonse.KickPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service xử lý các nghiệp vụ quản trị người chơi (Kick, Ban, Warp, Warn).
 * Giúp tách biệt logic nghiệp vụ khỏi Controller.
 */
@Service
public class AdminService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private AdminRepository adminRepo;

    public void kickPlayer(int connectionId, String reason, String adminName) {
        ServerPlayer player = playerService.get(connectionId);
        if (player != null && player.getConnection() != null) {
            adminRepo.logAction(adminName, "KICK", player.getDbUserId(),
                    "Kicked player: " + player.getUsername() + ". Reason: " + reason);

            KickPacket kick = new KickPacket();
            kick.reason = "Admin: " + reason;
            player.getConnection().sendTCP(kick);
            player.getConnection().close();
        }
    }

    public void banPlayer(int connectionId, int userId, String ip, String reason, Integer duration, String adminName) {
        String logUser = "UserID:" + userId + " (IP:" + ip + ")";
        try {
            // 1. Ghi vào Database trước (Dù người chơi online hay offline đều bị cấm)
            adminRepo.banPlayer(userId, ip, reason, adminName, duration);
            adminRepo.logAction(adminName, "BAN", userId,
                    "Banned " + logUser + ". Reason: " + reason + " (Duration: " + (duration == null ? "PERM" : duration + "m") + ")");

            // 2. Kiểm tra xem người chơi có đang ONLINE không để Kick ngay
            ServerPlayer player = playerService.get(connectionId);
            if (player != null && player.getConnection() != null) {
                KickPacket kick = new KickPacket();
                kick.reason = "BANNED: " + reason;
                player.getConnection().sendTCP(kick);
                player.getConnection().close();
                System.out.println("[ADMIN] Force kicked banned player: " + player.getUsername());
            } else {
                System.out.println("[ADMIN] Banned offline player: " + logUser);
            }
        } catch (Exception e) {
            adminRepo.logAction(adminName, "BAN_ERROR", userId,
                    "Failed to ban " + logUser + ": " + e.getMessage());
            System.err.println("[ADMIN-ERROR] Ban failed for " + logUser + ": " + e.getMessage());
        }
    }

    public void warnPlayer(int connectionId, String reason, String adminName) {
        ServerPlayer player = playerService.get(connectionId);
        if (player != null) {
            adminRepo.warnPlayer(player.getDbUserId(), 1, reason, adminName);
            adminRepo.logAction(adminName, "WARN", player.getDbUserId(),
                    "Warned player: " + player.getUsername() + ". Reason: " + reason);

            int totalWarns = adminRepo.getWarningCount(player.getDbUserId());
            if (totalWarns >= 3) {
                kickPlayer(connectionId, "Too many warnings (" + totalWarns + ")", "SYSTEM");
            }
        }
    }

    public void warpPlayerToSpawn(int connectionId, String adminName) {
        ServerPlayer player = playerService.get(connectionId);
        if (player != null) {
            // Tọa độ điểm Spawn mặc định (600, 1.0, 0)
            player.getState().position.set(600, 1.0f, 0);
            adminRepo.logAction(adminName, "WARP", player.getDbUserId(),
                    "Warped player to spawn: " + player.getUsername());
        }
    }

    public void unbanPlayer(int banId, String adminName) {
        adminRepo.unbanPlayer(banId);
        adminRepo.logAction(adminName, "UNBAN", null, "Unbanned record ID: " + banId);
    }
}
