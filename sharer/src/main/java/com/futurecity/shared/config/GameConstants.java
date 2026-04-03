package com.futurecity.shared.config;

/**
 * Hằng số dùng chung giữa Client và Server.
 * Mục tiêu: đảm bảo mọi tính toán vật lý, camera, tương tác đều đồng bộ.
 * Lưu ý: thay đổi các giá trị ở đây sẽ ảnh hưởng tới gameplay và va chạm.
 */
public class GameConstants {
    // Kích thước nhân vật (mô hình vật lý)
    public static final float PLAYER_WIDTH = 0.5f;
    public static final float PLAYER_DEPTH = 0.5f;
    public static final float PLAYER_HEIGHT = 1.8f;

    // Trọng lực (đơn vị: m/s^2, hướng âm theo trục Y)
    public static final float GRAVITY = -9.8f;

    // Tỉ lệ thế giới hiển thị so với đơn vị mô hình
    public static final float WORLD_SCALE = 20f;

    // Thông số camera (FPS/TPS)
    public static final float CAM_EYE_HEIGHT = WORLD_SCALE * 1.1f;
    public static final float CAM_PIVOT_HEIGHT = WORLD_SCALE * 0.85f;
    public static final float CAM_DISTANCE_TPS = WORLD_SCALE * 2.5f;
    public static final float CAM_SHOULDER_OFFSET = WORLD_SCALE * 0.4f;

    // Vận tốc và gia tốc di chuyển của nhân vật
    public static final float PLAYER_WALK_SPEED = 70f;
    public static final float PLAYER_RUN_SPEED = 100f;
    public static final float PLAYER_ACCELERATION = 100f;
    public static final float PLAYER_DECELERATION = 100f;

    // Độ cao mắt dùng cho ngắm mục tiêu ở TPS (Tỉ lệ theo WORLD_SCALE)
    public static final float EYE_HEIGHT = 1.5f * WORLD_SCALE;

    // Thông số bổ sung
    public static final float CAMERA_FOV = 67f;
    public static final float MAP_SCALE = 80.0f;
    public static final float INTERACTION_RANGE = 5.0f * WORLD_SCALE; // Tăng tầm tương tác tỉ lệ với thế giới
    public static final float ANGLE_TPS = 0.5f;
    public static final float ANGLE_FPS = 0.8f;
    public static final float INTERACTION_ANGLE_WEIGHT = 1.5f;
}
