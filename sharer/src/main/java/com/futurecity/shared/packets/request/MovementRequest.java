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

    /**
     * Input di chuyển theo trục dọc (W/S). Giá trị từ -1 đến 1.
     */
    public float vertical;

    /**
     * Góc quay của camera quanh trục Y (yaw).
     * Server cần thông tin này để tính toán hướng di chuyển tương đối theo camera.
     */
    public float cameraYaw;

    /**
     * Cờ báo hiệu người chơi có đang giữ phím chạy (Shift) hay không.
     */
    public boolean isRunning;

    /**
     * Độ cao mặt đất tại vị trí hiện tại (do Client tính toán từ terrain).
     * Server sẽ dùng giá trị này để xử lý trọng lực chính xác mà không cần mesh
     * terrain.
     */
    public float groundHeight;
}
