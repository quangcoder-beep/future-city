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

    // Cần constructor mặc định cho KryoNet
    public LoginResponse() {
    }

    public LoginResponse(int newId, String token) {
        this.status = "SUCCESS";
        this.newId = newId;
        this.message = "ÄÄƒng nháº­p thĂ nh cĂ´ng";
        this.token = token;
    }
}
