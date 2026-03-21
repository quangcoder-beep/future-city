package com.futurecity.shared.packets.resonse;

/**
 * Gói tin Server gửi cho Client khi tài khoản bị đăng nhập từ thiết bị khác.
 * Client nhận packet này sẽ hiển thị dialog thông báo và ngắt kết nối.
 */
public class KickPacket {
    /** Lý do bị kick. VD: "DUPLICATE_LOGIN" */
    public String reason;

    // Constructor mặc định bắt buộc cho KryoNet
    public KickPacket() {}
}
