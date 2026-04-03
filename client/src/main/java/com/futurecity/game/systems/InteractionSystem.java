package com.futurecity.game.systems;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.game.entities.Interactable;
import com.futurecity.game.entities.MainCharactor;
import java.util.List;

/**
 * InteractionSystem
 * Handles line-of-sight and scoring to find the best interaction target.
 */
public class InteractionSystem {
    private final Vector3 calcOrigin = new Vector3(); // Gá»‘c tĂ­nh toĂ¡n
    private final Vector3 calcDirection = new Vector3(); // HÆ°á»›ng tĂ­nh toĂ¡n
    private final Vector3 tmpVectorToObj = new Vector3(); // Vector ná»‘i táº¡m

    private final Quaternion tmpRotation = new Quaternion();
    private Camera camera; // Camera variable for UI projection
    private Interactable currentFocusTarget = null; // Current target being aimed at

    // --- UPDATE (Called every frame) ---
    public void update(MainCharactor player, Camera camera, boolean isFPS, List<Interactable> objects) {
        this.camera = camera;
        // 1. PREPARE INPUT (FPS vs TPS)
        if (isFPS) {
            calcOrigin.set(camera.position);
            calcDirection.set(camera.direction);
        } else {
            // Láº¥y tá»« Player Model + Offset
            player.getInstance().transform.getTranslation(calcOrigin);
            calcOrigin.y += GameConstants.EYE_HEIGHT;

            // 1. Get player rotation
            player.getInstance().transform.getRotation(tmpRotation);

            // 2. Reset hÆ°á»›ng tĂ­nh toĂ¡n vá» trá»¥c Z (HÆ°á»›ng máº·c Ä‘á»‹nh cá»§a
            // model)
            calcDirection.set(Vector3.Z);

            // 3. Rotate direction vector based on player orientation
            tmpRotation.transform(calcDirection);

        }

        // 2. SEARCH FOR TARGET
        currentFocusTarget = findBestTarget(objects, isFPS);
    }

    private Interactable findBestTarget(List<Interactable> objects, boolean isFPS) {
        Interactable bestTarget = null;
        float bestScore = -100f;

        // Config cho tá»«ng cháº¿ Ä‘á»™
        float maxDist = GameConstants.INTERACTION_RANGE;
        float minDot = isFPS ? GameConstants.ANGLE_FPS : GameConstants.ANGLE_TPS;
        float angleWeight = isFPS ? 2.0f : 1.2f;

        for (Interactable obj : objects) {
            BoundingBox box = obj.getCollisionBox();

            // Tìm điểm trên box gần với vị trí mắt người chơi nhất
            float closestX = Math.max(box.min.x, Math.min(calcOrigin.x, box.max.x));
            float closestY = Math.max(box.min.y, Math.min(calcOrigin.y, box.max.y));
            float closestZ = Math.max(box.min.z, Math.min(calcOrigin.z, box.max.z));

            float dist = calcOrigin.dst(closestX, closestY, closestZ);

            if (dist > maxDist)
                continue;

            // Check Góc dựa trên điểm gần nhất (thay vì tâm vật thể)
            // Điều này cực kỳ quan trọng đối với các tòa nhà lớn/cao
            tmpVectorToObj.set(closestX, closestY, closestZ).sub(calcOrigin).nor();
            float dot = calcDirection.dot(tmpVectorToObj);

            // Nếu vật thể ở quá gần (dist < 10), ta nới lỏng góc tương tác
            float effectiveMinDot = (dist < 15f) ? (minDot * 0.5f) : minDot;

            if (dot < effectiveMinDot)
                continue;

            // Tính điểm
            float distScore = 1.0f - (dist / maxDist);
            float finalScore = (dot * angleWeight) + distScore;

            if (finalScore > bestScore) {
                bestScore = finalScore;
                bestTarget = obj;
            }
        }
        return bestTarget;
    }

    // --- GETTERS ---
    public Interactable getCurrentFocusTarget() {
        return currentFocusTarget;
    }

    public Camera getCamera() {
        return camera;
    }
}
