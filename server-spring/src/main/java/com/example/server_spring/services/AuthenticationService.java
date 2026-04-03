package com.example.server_spring.services;

import com.example.server_spring.repository.UserRepository;
import com.example.server_spring.security.JwtUtil;
import com.futurecity.shared.packets.resonse.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private com.example.server_spring.repository.AdminRepository adminRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthenticationService() {
    }

    /**
     * Đăng nhập: check DB → so khớp BCrypt → tạo Access Token + Refresh Token + Identity.
     */
    public LoginResponse authenticate(String username, String rawPassword) {
        LoginResponse resp = new LoginResponse();

        // 1. Kiểm tra Ban - ƯU TIÊN HÀNG ĐẦU
        int userId = userRepo.getUserIdByUsername(username);
        
        if (userId > 0) {
            String ip = "";
            try {
                ip = ((org.springframework.web.context.request.ServletRequestAttributes) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                    .getRequest().getRemoteAddr();
            } catch (Exception e) {}

            if (adminRepo.isBanned(userId, ip)) {
                resp.status = "FAIL";
                resp.message = "TÀI KHOẢN ĐANG BỊ KHÓA: " + adminRepo.getBanReason(userId, ip);
                return resp;
            }
        }

        // 2. Xác thực mật khẩu
        String storedHash = userRepo.getPasswordHash(username);
        if (storedHash != null && passwordEncoder.matches(rawPassword, storedHash)) {
            resp.status = "SUCCESS";
            resp.newId = userId;
            resp.message = "Đăng nhập thành công";
            resp.token = jwtUtil.generateToken(userId);
            resp.refreshToken = jwtUtil.generateRefreshToken(userId);
            resp.nickname = userRepo.getNickname(userId);
            resp.avatarUrl = userRepo.getAvatarUrl(userId);
            resp.needsNickname = (resp.nickname == null || resp.nickname.isEmpty());
        } else {
            resp.status = "FAIL";
            resp.message = "Sai thông tin đăng nhập!";
        }
        return resp;
    }

    /**
     * Dùng Refresh Token JWT để cấp Access Token mới.
     */
    public LoginResponse refreshAccessToken(String refreshToken) {
        LoginResponse resp = new LoginResponse();

        int userId = jwtUtil.verifyRefreshToken(refreshToken);
        if (userId > 0) {
            // --- BAN CHECK FOR REFRESH ---
            String ip = "";
            try {
                ip = ((org.springframework.web.context.request.ServletRequestAttributes) 
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                    .getRequest().getRemoteAddr();
            } catch (Exception e) {}

            if (adminRepo.isBanned(userId, ip)) {
                resp.status = "FAIL";
                resp.message = "TÀI KHOẢN ĐANG BỊ KHÓA: " + adminRepo.getBanReason(userId, ip);
                return resp;
            }

            resp.status = "SUCCESS";
            resp.newId = userId;
            resp.message = "Token được làm mới thành công";
            resp.token = jwtUtil.generateToken(userId);
            resp.refreshToken = refreshToken;
            resp.nickname = userRepo.getNickname(userId);
            resp.avatarUrl = userRepo.getAvatarUrl(userId);
            resp.needsNickname = (resp.nickname == null || resp.nickname.isEmpty());
        } else {
            resp.status = "FAIL";
            resp.message = "Refresh Token không hợp lệ hoặc đã hết hạn!";
        }
        return resp;
    }

    /**
     * Đăng ký người dùng mới kèm Nickname.
     */
    public boolean register(String username, String rawPassword, String nickname) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return userRepo.registerUser(username, encodedPassword, nickname);
    }

    /**
     * Xác minh JWT Access Token → trả về userId.
     */
    public int verifyToken(String token) {
        return jwtUtil.verifyToken(token);
    }
}
