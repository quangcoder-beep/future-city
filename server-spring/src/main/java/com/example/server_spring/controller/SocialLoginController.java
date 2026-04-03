package com.example.server_spring.controller;

import com.example.server_spring.repository.UserRepository;
import com.example.server_spring.security.JwtUtil;
import com.example.server_spring.services.SessionService;
import com.example.server_spring.services.PlayerService;
import com.futurecity.shared.packets.resonse.LoginResponse;
import com.futurecity.shared.packets.resonse.NicknameUpdate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Điều hướng Social Login và cung cấp API Polling kết quả.
 */
@RestController
@RequestMapping("/api/auth")
public class SocialLoginController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Điểm bắt đầu từ Game: /api/auth/social-login?provider=google&sessionId=123
     */
    @GetMapping("/social-login")
    public void startSocialLogin(@RequestParam String provider, @RequestParam String sessionId,
            HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(true);
        session.setAttribute("pollingSessionId", sessionId);
        System.out.println("DEBUG: Social Login started. PollingSessionId stored in Session: " + sessionId);

        // Chuyển hướng đến Spring Security OAuth2 authorization endpoint
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    /**
     * API để Game gọi mỗi 2 giây: GET /api/auth/poll-status?sessionId=123
     */
    @GetMapping("/poll-status")
    public ResponseEntity<?> pollStatus(@RequestParam String sessionId) {
        LoginResponse response = sessionService.getSessionResult(sessionId);
        if (response != null) {
            // Sau khi lấy kết quả thì dọn dẹp session
            sessionService.removeSession(sessionId);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(202).body("PENDING"); // 202 Accepted
    }

    /**
     * Kiểm tra Nickname có bị trùng không
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam String name) {
        // Quy tắc validation cơ bản (sẽ chuyển vào ValidatorService sau)
        if (name == null || name.length() < 3 || name.length() > 15 || !name.matches("^[a-zA-Z0-9]*$")) {
            return ResponseEntity.badRequest().body("INVALID_FORMAT");
        }

        boolean taken = userRepository.isNicknameTaken(name);
        return ResponseEntity.ok(Map.of("available", !taken));
    }

    @Autowired
    private PlayerService playerService;

    /**
     * Thiết lập Nickname (Dành cho tài khoản mới chưa có tên hoặc đổi tên)
     */
    @PostMapping("/set-nickname")
    public ResponseEntity<?> setNickname(@RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        String token = authHeader.replace("Bearer ", "");
        String name = body.get("nickname");
        int userId = jwtUtil.verifyToken(token);
        if (userId == -1)
            return ResponseEntity.status(401).body("UNAUTHORIZED");

        if (name == null || name.length() < 3 || name.length() > 15 || !name.matches("^[a-zA-Z0-9]*$")) {
            return ResponseEntity.badRequest().body("INVALID_FORMAT");
        }

        if (userRepository.isNicknameTaken(name)) {
            return ResponseEntity.badRequest().body("TAKEN");
        }

        userRepository.updateNickname(userId, name);
        String updatedNickname = userRepository.getNickname(userId);

        // --- ĐỒNG BỘ SANG GAME SERVER (KryoNet) ---
        for (com.example.server_spring.entity.ServerPlayer p : playerService.getAll()) {
            if (p.getDbUserId() == userId) {
                p.getState().username = updatedNickname;
                p.setNickname(updatedNickname);

                // Gửi thông báo cập nhật cho tất cả mọi người (kể cả chính mình để Client cập
                // nhật nhãn)
                NicknameUpdate packet = new NicknameUpdate(p.getId(), updatedNickname);
                playerService.broadcastTCP(packet);
                break;
            }
        }

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "nickname", updatedNickname));
    }
}
