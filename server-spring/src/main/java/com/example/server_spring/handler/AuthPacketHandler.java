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
    @Autowired
    private com.example.server_spring.repository.AdminRepository adminRepo;

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

        // Security Validation
        if (req.token.length() > 1024) {
            System.err.println("[SECURITY] Blocked oversized auth token from ConnID=" + conn.getID());
            resp.status = "FAIL";
            resp.message = "Token string too large!";
            conn.sendTCP(resp);
            conn.close();
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

        // Check if banned
        String ip = conn.getRemoteAddressTCP().getAddress().getHostAddress();
        if (adminRepo.isBanned(userId, ip)) {
            resp.status = "FAIL";
            resp.message = "TÀI KHOẢN ĐANG BỊ KHÓA: " + adminRepo.getBanReason(userId, ip);
            conn.sendTCP(resp);
            
            // Send KickPacket as well to force client to main menu
            com.futurecity.shared.packets.resonse.KickPacket kick = new com.futurecity.shared.packets.resonse.KickPacket();
            kick.reason = "BANNED: " + adminRepo.getBanReason(userId, ip);
            conn.sendTCP(kick);
            
            // Wait 100ms then close to ensure delivery
            try { Thread.sleep(100); } catch (Exception e) {}
            conn.close();
            return;
        }

        // Create ServerPlayer and load position from DB
        String username = userRepo.getUsernameById(userId);
        ServerPlayer player = new ServerPlayer(conn.getID(), username, conn);
        player.setDbUserId(userId);
        player.setNickname(userRepo.getNickname(userId));

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

        // 2. Nếu đăng nhập bình thường (không trùng), tải dữ liệu từ DB
        PlayerRepository.PlayerData dbData = playerRepo.getPlayerData(userId);
        if (dbData != null) {
            if (!isDuplicate) {
                player.getState().position.set(dbData.posX, dbData.posY, dbData.posZ);
            }
            player.setCredits(dbData.coins);
            player.setDeliveries(dbData.deliveries);
            player.setReputation(dbData.reputation);
        }

        playerService.add(player);

        // Success response
        resp.status = "SUCCESS";
        resp.newId = conn.getID();
        resp.message = "Login successful";
        resp.nickname = userRepo.getNickname(userId);
        resp.avatarUrl = userRepo.getAvatarUrl(userId);
        resp.needsNickname = (resp.nickname == null || resp.nickname.isEmpty());
        
        // Populate stats from DB
        if (dbData != null) {
            resp.credits = dbData.coins;
            resp.deliveries = dbData.deliveries;
            resp.reputation = dbData.reputation;
        }
        
        conn.sendTCP(resp);
    }

    public void handleMovement(Connection conn, MovementRequest req) {
        ServerPlayer player = playerService.get(conn.getID());
        if (player != null) {
            // Queue input for PhysicsService to process deterministically
            player.pendingInputs.add(req);
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
