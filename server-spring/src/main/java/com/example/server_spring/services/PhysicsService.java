package com.example.server_spring.services;

import com.example.server_spring.entity.ServerPlayer;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.physics.PlayerPhysics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Xử lý vật lý cho tất cả người chơi mỗi tick.
 * Input → Velocity → Collision → Gravity → Animation
 * Tương đương ServerPhysicsSystem cũ.
 */
@Service
public class PhysicsService {

    @Autowired
    private MapService mapService;

    /**
     * Gọi mỗi tick (~60Hz) để cập nhật vật lý.
     */
    public void update(float delta, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            PlayerState state = player.getState();
            float groundY = player.getGroundHeight();

            // 1. Input → Velocity + Animation
            String newAnim = PlayerPhysics.processInput(
                    state, player.horizontal, player.vertical,
                    player.isRunning, player.cameraYaw, false, delta);

            // 2. Collision (nếu đâm tường → reset velocity)
            mapService.applyCollision(state, delta);

            // 3. Gravity
            PlayerPhysics.applyGravity(state, groundY, delta);

            // 4. Animation
            state.currentAnimation = newAnim;
        }
    }
}
