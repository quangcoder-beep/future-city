package com.futurecity.game.managers;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.futurecity.game.systems.CollisionSystem;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.AnimationName;
import com.futurecity.shared.packets.request.MovementRequest;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A class that simulates a "Local" Server to handle game logic in offline mode.
 * This class receives requests (e.g., MovementRequest) from the Client,
 * processes them (calculates physics, collisions), updates the state
 * (PlayerState),
 * and sends the results back to the Client.
 */
public class GameLogicManager {

    private final PlayerState playerState;
    private final MapManager mapManager;
    private final CollisionSystem collisionSystem;
    private final List<com.futurecity.game.entities.GameObject> obstacles;

    // Queue to receive requests from Client (NetworkManager)
    private final BlockingQueue<Object> incomingPackets = new LinkedBlockingQueue<>();

    // Temporary variables for calculation, avoiding continuous creation
    private final Vector3 tempMove = new Vector3();
    private final Vector3 tempForward = new Vector3();
    private final Vector3 tempRight = new Vector3();

    public GameLogicManager(PlayerState playerState, MapManager mapManager, CollisionSystem collisionSystem,
            List<com.futurecity.game.entities.GameObject> obstacles) {
        this.playerState = playerState;
        this.mapManager = mapManager;
        this.collisionSystem = collisionSystem;
        this.obstacles = obstacles;
    }

    /**
     * The update loop of the server logic.
     * 
     * @param delta The time between frames.
     */
    public void update(float delta) {
        // 1. Xá»­ lĂ½ táº¥t cáº£ cĂ¡c gĂ³i tin Ä‘áº¿n tá»« client
        processIncomingPackets(delta);

        // 2. Apply physics and collision
        applyPhysics(delta);

        // 3. Update animation state
        updateAnimationState();
    }

    /**
     * Client (NetworkManager) calls this method to send a packet to the "server".
     * 
     * @param packet The packet to process.
     */
    public void onPacketReceived(Object packet) {
        incomingPackets.add(packet);
    }

    /**
     * Process packets in the queue.
     */
    private void processIncomingPackets(float delta) {
        while (!incomingPackets.isEmpty()) {
            Object packet = incomingPackets.poll();
            if (packet instanceof MovementRequest) {
                handleMovementRequest((MovementRequest) packet, delta);
            }
        }
    }

    /**
     * Handle movement requests from the client.
     * This is where the old "processInput" logic from MainCharacter is moved to.
     */
    private void handleMovementRequest(MovementRequest request, float deltaTime) {
        float targetMaxSpeed = request.isRunning ? GameConstants.PLAYER_RUN_SPEED : GameConstants.PLAYER_WALK_SPEED;

        if (request.horizontal != 0 || request.vertical != 0) {
            // Calculate movement direction based on the camera angle sent by the client
            float sinYaw = MathUtils.sinDeg(request.cameraYaw);
            float cosYaw = MathUtils.cosDeg(request.cameraYaw);

            tempForward.set(sinYaw, 0, cosYaw).nor();
            tempRight.set(tempForward).rotate(Vector3.Y, -90);

            tempMove.setZero();
            if (request.vertical > 0)
                tempMove.add(tempForward);
            else if (request.vertical < 0)
                tempMove.sub(tempForward);
            if (request.horizontal > 0)
                tempMove.add(tempRight);
            else if (request.horizontal < 0)
                tempMove.sub(tempRight);
            tempMove.nor();

            // Calculate target velocity
            Vector3 targetVel = tempMove.scl(targetMaxSpeed);

            // Lerp velocity to create inertia
            playerState.velocity.x = MathUtils.lerp(playerState.velocity.x, targetVel.x,
                    GameConstants.PLAYER_ACCELERATION * deltaTime);
            playerState.velocity.z = MathUtils.lerp(playerState.velocity.z, targetVel.z,
                    GameConstants.PLAYER_ACCELERATION * deltaTime);

            // Rotate character in the direction of movement
            float targetAngle = MathUtils.atan2(playerState.velocity.x, playerState.velocity.z)
                    * MathUtils.radiansToDegrees;
            playerState.angle = lerpAngleDeg(playerState.angle, targetAngle, 10f * deltaTime);

        } else {
            // Reduce speed when there is no input
            playerState.velocity.x = MathUtils.lerp(playerState.velocity.x, 0,
                    GameConstants.PLAYER_DECELERATION * deltaTime);
            playerState.velocity.z = MathUtils.lerp(playerState.velocity.z, 0,
                    GameConstants.PLAYER_DECELERATION * deltaTime);
        }
    }

    /**
     * Apply physics, collision and gravity.
     * This is where the old "collision" and "applyGravity" logic is moved to.
     */
    private void applyPhysics(float deltaTime) {
        // --- COLLISION ---
        // Create a "ghost" to check for collisions before moving
        com.futurecity.game.entities.GameObject ghost = new com.futurecity.game.entities.GameObject(null) {
        };
        ghost.localBounds.set(playerState.position, playerState.position.cpy().add(0, GameConstants.PLAYER_HEIGHT, 0)); // Simplified
                                                                                                                        // bounds
                                                                                                                        // for
                                                                                                                        // server
        ghost.position.set(playerState.position);

        // Handle X-axis
        float currentX = playerState.position.x;
        playerState.position.x += playerState.velocity.x * deltaTime;
        ghost.position.set(playerState.position);
        if (collisionSystem.checkCollision(ghost, obstacles)) {
            playerState.position.x = currentX;
            playerState.velocity.x = 0;
        }

        // Handle Z-axis
        float currentZ = playerState.position.z;
        playerState.position.z += playerState.velocity.z * deltaTime;
        ghost.position.set(playerState.position);
        if (collisionSystem.checkCollision(ghost, obstacles)) {
            playerState.position.z = currentZ;
            playerState.velocity.z = 0;
        }

        // --- GRAVITY ---
        playerState.position.y += playerState.velocity.y * deltaTime;
        float groundY = mapManager.getTerrainHeight(playerState.position.x, playerState.position.y,
                playerState.position.z);
        if (playerState.position.y <= groundY) {
            playerState.position.y = groundY;
            playerState.velocity.y = 0;
        } else {
            playerState.velocity.y += GameConstants.GRAVITY * deltaTime;
        }
    }

    /**
     * Update animation state based on velocity.
     */
    private void updateAnimationState() {
        float speedSq = playerState.velocity.x * playerState.velocity.x
                + playerState.velocity.z * playerState.velocity.z;
        boolean isMoving = speedSq > 0.1f;

        if (isMoving) {
            // Assume client sends isRunning in MovementRequest
            // To simplify, we can infer from the magnitude of velocity
            if (speedSq > (GameConstants.PLAYER_WALK_SPEED * GameConstants.PLAYER_WALK_SPEED * 0.8f)) {
                playerState.currentAnimation = AnimationName.RUN.getValue();
            } else {
                playerState.currentAnimation = AnimationName.WALK.getValue();
            }
        } else {
            playerState.currentAnimation = AnimationName.IDLE.getValue();
        }
    }

    private float lerpAngleDeg(float from, float to, float progress) {
        float delta = ((to - from + 360 + 180) % 360) - 180;
        return (from + delta * progress + 360) % 360;
    }
}
