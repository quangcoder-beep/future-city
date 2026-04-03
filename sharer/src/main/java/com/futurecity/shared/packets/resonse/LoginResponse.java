package com.futurecity.shared.packets.resonse;

/**
 * Gói tin Server trả về cho Client sau khi xử lý đăng nhập.
 * Trả về trạng thái và ID mới được cấp.
 */
public class LoginResponse {
    public String status; // "SUCCESS" hoặc "FAIL"
    public String message; // Chi tiết lỗi hoặc lời chào
    public int newId;
    public String token; // Access Token JWT (ngắn hạn, 15 phút)
    public String refreshToken; // Refresh Token (dài hạn, 30 ngày)
    public String nickname;
    public String avatarUrl;
    public boolean needsNickname;
    public int credits;
    public int deliveries;
    public float reputation;

    // Cần constructor mặc định cho KryoNet
    public LoginResponse() {
    }

    public LoginResponse(int newId, String token) {
        this.status = "SUCCESS";
        this.newId = newId;
        this.message = "Đăng nhập thành công";
        this.token = token;
    }
}
