package com.futurecity.shared.packets.request;

/**
 * Gói tin Client gửi lên Server để yêu cầu đăng nhập.
 * Chỉ chứa dữ liệu thô, không có logic.
 */
public class LoginRequest {
    public String username;
    public String password;
    public String token; // JWT Token (dùng cho KryoNet sau khi đã login qua HTTP)
    public boolean isRegistering;
}
