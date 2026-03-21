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
    private JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    public AuthenticationService() {
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }

    /**
     * ÄÄƒng nháº­p: check DB â†’ so khá»›p BCrypt â†’ táº¡o Access Token + Refresh Token.
     */
    public LoginResponse authenticate(String username, String rawPassword) {
        LoginResponse resp = new LoginResponse();

        String storedHash = userRepo.getPasswordHash(username);
        if (storedHash != null && passwordEncoder.matches(rawPassword, storedHash)) {
            int userId = userRepo.getUserIdByUsername(username);

            resp.status = "SUCCESS";
            resp.newId = userId;
            resp.message = "ÄÄƒng nháº­p thĂ nh cĂ´ng";
            resp.token = jwtUtil.generateToken(userId);
            resp.refreshToken = jwtUtil.generateRefreshToken(userId);
        } else {
            resp.status = "FAIL";
            resp.message = "Sai thĂ´ng tin Ä‘Äƒng nháº­p!";
        }
        return resp;
    }

    /**
     * DĂ¹ng Refresh Token JWT Ä‘á»ƒ cáº¥p Access Token má»›i. KhĂ´ng cáº§n DB.
     */
    public LoginResponse refreshAccessToken(String refreshToken) {
        LoginResponse resp = new LoginResponse();

        int userId = jwtUtil.verifyRefreshToken(refreshToken);
        if (userId > 0) {
            resp.status = "SUCCESS";
            resp.newId = userId;
            resp.message = "Token Ä‘Æ°á»£c lĂ m má»›i thĂ nh cĂ´ng";
            resp.token = jwtUtil.generateToken(userId);
            resp.refreshToken = refreshToken;
        } else {
            resp.status = "FAIL";
            resp.message = "Refresh Token khĂ´ng há»£p lá»‡ hoáº·c Ä‘Ă£ háº¿t háº¡n!";
        }
        return resp;
    }

    /**
     * ÄÄƒng kĂ½ ngÆ°á»i dĂ¹ng má»›i.
     */
    public boolean register(String username, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return userRepo.registerUser(username, encodedPassword);
    }

    /**
     * XĂ¡c minh JWT Access Token â†’ tráº£ vá» userId.
     */
    public int verifyToken(String token) {
        return jwtUtil.verifyToken(token);
    }
}
