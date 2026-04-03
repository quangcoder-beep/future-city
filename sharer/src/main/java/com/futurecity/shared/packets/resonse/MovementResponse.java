package com.futurecity.shared.packets.resonse;

import com.futurecity.shared.entities.PlayerState;

/**
 * Server gửi gói tin này đến các Client để cập nhật trạng thái của một người
 * chơi.
 * Đây là gói tin chính để đồng bộ hóa thế giới game.
 */
public class MovementResponse {
    /**
     * Trạng thái đầy đủ của người chơi cần được cập nhật.
     */
    public PlayerState state;
}
