package com.futurecity.shared.packets.request;

/**
 * Client gửi yêu cầu di chuyển đến Server.
 * Gói tin này chứa các thông tin về input của người chơi.
 */
public class MovementRequest {
    /**
     * Input di chuyển theo trục ngang (A/D). Giá trị từ -1 đến 1.
     */
    public float horizontal;
    public float vertical;
    public float cameraYaw;
    public boolean isRunning;

    // --- eSports CSP Fields ---
    public int sequence;
    public float deltaTime;
}
