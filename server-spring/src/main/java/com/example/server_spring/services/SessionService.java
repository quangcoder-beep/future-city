package com.example.server_spring.services;

import com.futurecity.shared.packets.resonse.LoginResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý phiên đăng nhập tạm thời từ OAuth2 (Polling mechanism).
 */
@Service
public class SessionService {

    // Key: SessionID (String), Value: LoginResponse (chứa token)
    private final Map<String, LoginResponse> pendingLogins = new ConcurrentHashMap<>();

    public void saveSession(String sessionId, LoginResponse response) {
        pendingLogins.put(sessionId, response);
    }

    public LoginResponse getSessionResult(String sessionId) {
        return pendingLogins.get(sessionId);
    }

    public void removeSession(String sessionId) {
        pendingLogins.remove(sessionId);
    }
}
