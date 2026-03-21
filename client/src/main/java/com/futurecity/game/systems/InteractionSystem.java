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
 * Há»‡ thá»‘ng xá»­ lĂ½ tÆ°Æ¡ng tĂ¡c vá»›i váº­t thá»ƒ.
 */
public class InteractionSystem {
    private final Vector3 calcOrigin = new Vector3(); // Gá»‘c tĂ­nh toĂ¡n
    private final Vector3 calcDirection = new Vector3(); // HÆ°á»›ng tĂ­nh toĂ¡n
    private final Vector3 tmpVectorToObj = new Vector3(); // Vector ná»‘i táº¡m

    private final Quaternion tmpRotation = new Quaternion();
    private Camera camera; // Biáº¿n camera Ä‘á»ƒ dĂ¹ng cho UI project
    private Interactable currentFocusTarget = null; // Má»¥c tiĂªu Ä‘ang Ä‘Æ°á»£c ngáº¯m vĂ o

    // --- HĂ€M UPDATE (Gá»i má»—i khung hĂ¬nh) ---
    public void update(MainCharactor player, Camera camera, boolean isFPS, List<Interactable> objects) {
        this.camera = camera;
        // 1. CHUáº¨N Bá» Dá»® LIá»†U Äáº¦U VĂ€O (FPS vs TPS)
        if (isFPS) {
            calcOrigin.set(camera.position);
            calcDirection.set(camera.direction);
        } else {
            // Láº¥y tá»« Player Model + Offset
            player.getInstance().transform.getTranslation(calcOrigin);
            calcOrigin.y += GameConstants.EYE_HEIGHT;

            // 1. Láº¥y Ä‘á»™ xoay cá»§a nhĂ¢n váº­t nĂ©m vĂ o biáº¿n táº¡m tmpRotation
            player.getInstance().transform.getRotation(tmpRotation);

            // 2. Reset hÆ°á»›ng tĂ­nh toĂ¡n vá» trá»¥c Z (HÆ°á»›ng máº·c Ä‘á»‹nh cá»§a model)
            calcDirection.set(Vector3.Z);

            // 3. Xoay trá»¥c Z theo Ä‘á»™ xoay cá»§a nhĂ¢n váº­t
            // LĂºc nĂ y calcDirection sáº½ chá»‰ Ä‘Ăºng hÆ°á»›ng máº·t nhĂ¢n váº­t Ä‘ang quay
            tmpRotation.transform(calcDirection);

        }

        // 2. CHáº Y THUáº¬T TOĂN TĂŒM KIáº¾M (Logic Scoring Ä‘Ă£ bĂ n)
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

    // --- GETTER Äá»‚ UI VĂ€ INPUT DĂ™NG ---
    public Interactable getCurrentFocusTarget() {
        return currentFocusTarget;
    }

    public Camera getCamera() {
        return camera;
    }
}
