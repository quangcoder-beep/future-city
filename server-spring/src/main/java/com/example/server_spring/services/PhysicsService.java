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
            try {
                PlayerState state = player.getState();
                float groundY = player.getGroundHeight();

                // eSports CSP: Process all queued inputs with their exact client deltaTime
                com.futurecity.shared.packets.request.MovementRequest req;
                String newAnim = state.currentAnimation;
                while ((req = player.pendingInputs.poll()) != null) {
                    if (req == null) continue;

                    // 1. Input → Velocity + Animation
                    newAnim = PlayerPhysics.processInput(
                            state, req.horizontal, req.vertical,
                            req.isRunning, req.cameraYaw, false, req.deltaTime);

                    // 2. Collision
                    mapService.applyCollision(state, req.deltaTime);

                    // 3. Gravity
                    PlayerPhysics.applyGravity(state, groundY, req.deltaTime);
                    
                    // 4. Update Sequence Acknowledgment
                    state.lastProcessedSequence = req.sequence;
                }
                
                state.currentAnimation = newAnim;
            } catch (Exception e) {
                System.err.println("[PHYSICS-ERROR] Error processing player " + player.getId() + ": " + e.getMessage());
                player.pendingInputs.clear(); // Clear bad inputs to prevent infinite crash loop
            }
        }
    }
}
