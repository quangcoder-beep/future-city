package com.futurecity.game.systems;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.futurecity.game.entities.GameObject;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Camera;

public class CollisionSystem {
    private ShapeRenderer debugRenderer = new ShapeRenderer();

    public boolean checkCollision(GameObject object, List<GameObject> obstacles) {
        // Láº¥y há»™p va cháº¡m cá»§a Ä‘á»‘i tÆ°á»£ng Ä‘ang di chuyá»ƒn (á»Ÿ vá»‹ trĂ­ má»›i)
        BoundingBox objBounds = object.getWorldBounds();

        // OPTIMIZATION: DĂ¹ng vĂ²ng láº·p for-index thay vĂ¬ foreach Ä‘á»ƒ trĂ¡nh táº¡o Iterator (Garbage Collection)
        for (int i = 0; i < obstacles.size(); i++) {
            GameObject obstacle = obstacles.get(i);
            
            if (obstacle == object)
                continue; // KhĂ´ng tá»± check chĂ­nh mĂ¬nh

            // Kiá»ƒm tra giao cáº¯t giá»¯a 2 há»™p va cháº¡m (AABB Check)
            if (objBounds.intersects(obstacle.getWorldBounds())) {
                return true; // CĂ³ va cháº¡m
            }
        }
        return false; // KhĂ´ng va cháº¡m
    }

    /**
     * HĂ m váº½ Debug há»™p va cháº¡m (Wireframe).
     * GiĂºp nhĂ¬n tháº¥y há»™p va cháº¡m thá»±c táº¿ trong khĂ´ng gian 3D.
     * Ráº¥t há»¯u Ă­ch Ä‘á»ƒ kiá»ƒm tra xem Box cĂ³ bá»‹ lá»‡ch so vá»›i hĂ¬nh áº£nh hay khĂ´ng.
     */
    public void renderDebug(Camera camera) {
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        // Váº½ mĂ u Ä‘á» Ä‘á»ƒ dá»… nhĂ¬n
        debugRenderer.setColor(Color.RED);

        // LÆ°u Ă½: á» Ä‘Ă¢y tĂ´i chÆ°a implement váº½ chi tiáº¿t tá»«ng Box 
        // vĂ¬ ShapeRenderer khĂ´ng há»— trá»£ váº½ Box 3D trá»±c tiáº¿p má»™t cĂ¡ch Ä‘Æ¡n giáº£n 
        // mĂ  pháº£i váº½ 12 Ä‘Æ°á»ng tháº³ng ná»‘i cĂ¡c Ä‘á»‰nh.
        // Tuy nhiĂªn, viá»‡c Ä‘á»ƒ hĂ m nĂ y á»Ÿ Ä‘Ă¢y giĂºp cáº¥u trĂºc code chuáº©n chá»‰nh.
        // Náº¿u cáº§n debug sĂ¢u hÆ¡n, ta cĂ³ thá»ƒ dĂ¹ng ModelBuilder Ä‘á»ƒ váº½ Box.
        
        debugRenderer.end();
    }
}
