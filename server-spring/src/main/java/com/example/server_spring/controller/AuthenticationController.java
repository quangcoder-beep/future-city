package com.example.server_spring.controller;

import com.example.server_spring.services.AuthenticationService;
import com.futurecity.shared.packets.request.LoginRequest;
import com.futurecity.shared.packets.resonse.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authService;

    /** ÄÄƒng nháº­p â†’ tráº£ Access Token (15 phĂºt) + Refresh Token (30 ngĂ y). */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse resp = authService.authenticate(request.username, request.password);
        if ("SUCCESS".equals(resp.status)) {
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.status(401).body(resp);
    }

    /** ÄÄƒng kĂ½ tĂ i khoáº£n má»›i. */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody LoginRequest request) {
        LoginResponse resp = new LoginResponse();
        if (authService.register(request.username, request.password)) {
            resp.status = "SUCCESS";
            resp.message = "ÄÄƒng kĂ½ thĂ nh cĂ´ng!";
            return ResponseEntity.ok(resp);
        }
        resp.status = "FAIL";
        resp.message = "Username Ä‘Ă£ tá»“n táº¡i!";
        return ResponseEntity.status(400).body(resp);
    }

    /** DĂ¹ng Refresh Token Ä‘á»ƒ láº¥y Access Token má»›i â€” khĂ´ng cáº§n nháº­p láº¡i máº­t kháº©u. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody LoginRequest request) {
        LoginResponse resp = authService.refreshAccessToken(request.token);
        if ("SUCCESS".equals(resp.status)) {
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.status(401).body(resp);
    }
}
