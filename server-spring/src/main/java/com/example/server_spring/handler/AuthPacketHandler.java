package com.example.server_spring.handler;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.PlayerRepository;
import com.example.server_spring.repository.UserRepository;
import com.example.server_spring.services.AuthenticationService;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.packets.request.LoginRequest;
import com.futurecity.shared.packets.request.MovementRequest;
import com.futurecity.shared.packets.resonse.KickPacket;
import com.futurecity.shared.packets.resonse.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles authentication and movement packets.
 * - Login via JWT Token (KryoNet).
 * - Update movement input from Client.
 */
@Component
public class AuthPacketHandler {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PlayerRepository playerRepo;
    @Autowired
    private AuthenticationService authService;

    /**
     * Handles login via KryoNet.
     * Client must login via HTTP first to get a JWT Token, then send Token here.
     */
    public void handleLogin(Connection conn, LoginRequest req) {
        LoginResponse resp = new LoginResponse();

        // Only accept JWT Token — password is not accepted over KryoNet
        if (req.token == null || req.token.isEmpty()) {
            resp.status = "FAIL";
            resp.message = "Please login via HTTP first to get a Token!";
            conn.sendTCP(resp);
            return;
        }

        // Verify Token
        int userId = authService.verifyToken(req.token);
        if (userId <= 0) {
            resp.status = "FAIL";
            resp.message = "Invalid or expired Token!";
            conn.sendTCP(resp);
            return;
        }

        // Create ServerPlayer and load position from DB
        String username = userRepo.getUsernameById(userId);
        ServerPlayer player = new ServerPlayer(conn.getID(), username, conn);
        player.setDbUserId(userId);

        // 1. Kiểm tra session cũ trước khi đọc DB
        boolean isDuplicate = false;
        for (ServerPlayer existing : playerService.getAll()) {
            if (existing.getDbUserId() == userId) {
                isDuplicate = true;

                // a. Truyền vị trí mới nhất trực tiếp cho session mới (tránh rollback)
                PlayerState oldState = existing.getState();
                player.getState().position.set(oldState.position.x, oldState.position.y, oldState.position.z);

                // b. Lưu vào DB ngay lập tức
                playerRepo.savePlayerPosition(userId, oldState.position.x, oldState.position.y, oldState.position.z);

                // c. Kick Client cũ
                KickPacket kick = new KickPacket();
                kick.reason = "DUPLICATE_LOGIN";
                existing.getConnection().sendTCP(kick);

                // d. Xoá session cũ trước khi close (để disconnected listener không gọi lần 2)
                playerService.remove(existing.getId());
                existing.getConnection().close();

                // e. Gửi PlayerLeft cho mọi người để xoá model 3D (ghost) của session cũ
                com.futurecity.shared.packets.PlayerLeft leftResp = new com.futurecity.shared.packets.PlayerLeft();
                leftResp.id = existing.getId();
                playerService.broadcastTCP(leftResp);

                System.out.println("[AUTH] Kicked duplicate session for userId=" + userId);
                break;
            }
        }

        // 2. Nếu đăng nhập bình thường (không trùng), tải vị trí từ DB
        if (!isDuplicate) {
            PlayerRepository.PlayerData data = playerRepo.getPlayerData(userId);
            if (data != null) {
                player.getState().position.set(data.posX, data.posY, data.posZ);
            }
        }

        playerService.add(player);

        // Success response
        resp.status = "SUCCESS";
        resp.newId = conn.getID();
        resp.message = "Login successful";
        conn.sendTCP(resp);
    }

    /**
     * Cập nhật input di chuyển (hướng, chạy/đi, góc camera) từ Client.
     */
    public void handleMovement(Connection conn, MovementRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player != null) {
            player.horizontal = req.horizontal;
            player.vertical = req.vertical;
            player.isRunning = req.isRunning;
            player.cameraYaw = req.cameraYaw;
        }
    }

    /**
     * Cleanup when a client disconnects from KryoNet.
     * KryoNetworkHandler already persists position and broadcasts PlayerLeft,
     * so we only need to drop the in-memory player entry here.
     */
    public void handleDisconnect(Connection conn) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player != null) {
            playerService.remove(player.getId());
        }
    }
}
