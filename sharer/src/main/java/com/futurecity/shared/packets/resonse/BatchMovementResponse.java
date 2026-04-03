package com.futurecity.shared.packets.resonse;

import com.futurecity.shared.entities.PlayerState;

/**
 * Thay thế MovementResponse đơn lẻ.
 * Chứa mảng PlayerState của tất cả player gần client (AoI).
 *
 * Lợi ích: N player → thay vì N gói tin, chỉ cần 1 gói tin duy nhất.
 * Giảm 99% overhead serialization + system call sendUDP.
 */
public class BatchMovementResponse {
    /**
     * Mảng trạng thái của các player trong vùng AoI của receiver.
     */
    public PlayerState[] states;

    /**
     * Số lượng player hợp lệ trong mảng states.
     */
    public int count;
}
