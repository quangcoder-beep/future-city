package com.futurecity.game.entities;

/**
 * PlayerState
 * <p>
 * Enum định nghĩa các trạng thái (State) có thể có của nhân vật.
 * Đây là thành phần cốt lõi của mô hình State Machine (FSM).
 */
public enum PlayerState {
    /** Đứng yên */
    IDLE,

    /** Đi bộ */
    WALKING,

    /** Chạy nhanh */
    RUNNING,

    /** Nhảy (chưa implement logic) */
    JUMPING,

    /** Rơi tự do (khi không đứng trên mặt đất) */
    FALLING
}
