package com.futurecity.game.systems;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.futurecity.game.entities.GameObject;
import com.futurecity.game.entities.Interactable;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.managers.MapManager;
import java.util.ArrayList;
import java.util.List;

/**
 * MinimapLogic
 * Chá»‹u trĂ¡ch nhiá»‡m tĂ­nh toĂ¡n dá»¯ liá»‡u cho Minimap.
 * - Chuyá»ƒn Ä‘á»•i tá»a Ä‘á»™ World (3D) sang Minimap (2D).
 * - Lá»c danh sĂ¡ch cĂ¡c váº­t thá»ƒ náº±m trong pháº¡m vi hiá»ƒn thá»‹.
 */
public class MinimapLogic {
    private MainCharactor player;
    private MapManager mapManager;

    // CĂ¡c biáº¿n táº¡m Ä‘á»ƒ tĂ­nh toĂ¡n (trĂ¡nh táº¡o object má»›i liĂªn tá»¥c)
    private final Vector2 tmpPos = new Vector2();
    private final List<GameObject> visibleObstacles = new ArrayList<>();
    private final List<Interactable> visibleInteractables = new ArrayList<>();
    private final List<GameObject> visibleRoads = new ArrayList<>();
    private final List<GameObject> visibleLands = new ArrayList<>();
    private final List<GameObject> visibleTrees = new ArrayList<>();

    // ÄÆ°á»ng Ä‘i hiá»‡n táº¡i (nháº­n tá»« server)
    private List<Vector3> currentPath = new ArrayList<>();

    public MinimapLogic(MainCharactor player, MapManager mapManager) {
        this.player = player;
        this.mapManager = mapManager;
    }

    public void setCurrentPath(List<Vector3> path) {
        this.currentPath = path != null ? path : new ArrayList<>();
    }

    public List<Vector3> getCurrentPath() {
        return currentPath;
    }

    public Vector3 getPlayerPosition() {
        return player.getPosition();
    }

    /**
     * Cáº­p nháº­t logic: Lá»c cĂ¡c váº­t thá»ƒ xung quanh ngÆ°á»i chÆ¡i
     * 
     * @param viewRadius BĂ¡n kĂ­nh hiá»ƒn thá»‹ cá»§a Minimap (theo Ä‘Æ¡n vá»‹ World)
     */
    public void update(float viewRadius) {
        visibleObstacles.clear();
        visibleInteractables.clear();
        visibleRoads.clear();
        visibleLands.clear();
        visibleTrees.clear();

        Vector3 playerPos = player.getPosition();
        float playerX = playerPos.x;
        float playerZ = playerPos.z;
        float radiusSq = viewRadius * viewRadius;
        // Táº§m nhĂ¬n xa hÆ¡n cho Ä‘á»‹a hĂ¬nh
        float terrainRadiusSq = (viewRadius + 100) * (viewRadius + 100);

        // 1. Lá»c tĂ²a nhĂ  (Obstacles)
        for (GameObject obj : mapManager.getObstacles()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= radiusSq) {
                visibleObstacles.add(obj);
            }
        }

        // 2. Lá»c NPC/Shop (Interactables)
        for (Interactable obj : mapManager.getInteractables()) {
            if (obj instanceof GameObject) {
                GameObject go = (GameObject) obj;
                float distSq = Vector2.dst2(playerX, playerZ, go.getPosition().x, go.getPosition().z);
                if (distSq <= radiusSq) {
                    visibleInteractables.add(obj);
                }
            }
        }

        // 3. Lá»c Roads
        for (GameObject obj : mapManager.getRoads()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= terrainRadiusSq) {
                visibleRoads.add(obj);
            }
        }

        // 4. Lá»c Lands
        for (GameObject obj : mapManager.getLands()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= terrainRadiusSq) {
                visibleLands.add(obj);
            }
        }

        // 5. Lá»c Trees
        for (GameObject obj : mapManager.getTrees()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= radiusSq) {
                visibleTrees.add(obj);
            }
        }

        // 6. Cáº¯t tá»‰a Ä‘Æ°á»ng dáº«n GPS khi nhĂ¢n váº­t Ä‘i qua (Client-Side Path Culling)
        // Náº¿u khĂ´ng lĂ m bÆ°á»›c nĂ y, Ä‘Æ°á»ng GPS sáº½ ná»‘i tá»« sau Ä‘uĂ´i nhĂ¢n váº­t táº¡o ra má»™t vá»‡t
        // zig-zag dá»‹ há»£m.
        if (currentPath != null && currentPath.size() > 1) {
            boolean cull = true;
            while (cull && currentPath.size() > 2) {
                Vector3 n0 = currentPath.get(0);
                Vector3 n1 = currentPath.get(1);

                float d0 = Vector2.dst2(playerX, playerZ, n0.x, n0.z);
                float d1 = Vector2.dst2(playerX, playerZ, n1.x, n1.z);

                // XĂ³a Ä‘iá»ƒm Ä‘iá»u hÆ°á»›ng cÅ© náº¿u:
                // - ÄĂ£ Ä‘i qua/giáº«m lĂªn Ä‘iá»ƒm Ä‘Ă³ (CĂ¡ch < 4m = d0 < 16)
                // - HOáº¶C Ä‘iá»ƒm tiáº¿p theo náº±m gáº§n ngÆ°á»i chÆ¡i hÆ¡n Ä‘iá»ƒm cÅ© (nghÄ©a lĂ  Ä‘iá»ƒm cÅ© Ä‘Ă£ bá»‹
                // vÆ°á»£t máº·t vĂ  bá» láº¡i sau lÆ°ng)
                if (d0 < 16f || d1 < d0) {
                    currentPath.remove(0);
                } else {
                    cull = false;
                }
            }
        }
    }

    /**
     * Chuyá»ƒn Ä‘á»•i tá»a Ä‘á»™ tháº­t (World) sang tá»a Ä‘á»™ tÆ°Æ¡ng Ä‘á»‘i trĂªn Minimap
     * 
     * @param worldX     Tá»a Ä‘á»™ X trong game
     * @param worldZ     Tá»a Ä‘á»™ Z trong game (tÆ°Æ¡ng á»©ng Y trĂªn báº£n Ä‘á»“ 2D)
     * @param mapCenterX TĂ¢m X cá»§a Minimap widget
     * @param mapCenterY TĂ¢m Y cá»§a Minimap widget
     * @param scale      Tá»· lá»‡ thu nhá» (World -> Map)
     * @return Vector2 chá»©a tá»a Ä‘á»™ váº½ trĂªn mĂ n hĂ¬nh
     */
    public Vector2 worldToMapCoords(float worldX, float worldZ, float mapCenterX, float mapCenterY, float scale) {
        return worldToMapCoords(worldX, worldZ, mapCenterX, mapCenterY, scale, 0);
    }

    public Vector2 worldToMapCoords(float worldX, float worldZ, float mapCenterX, float mapCenterY, float scale,
            float rotationDeg) {
        // Tá»a Ä‘á»™ tÆ°Æ¡ng Ä‘á»‘i so vá»›i ngÆ°á»i chÆ¡i
        // FBO Camera dĂ¹ng up=(0,0,-1): North (-Z) lĂ  hÆ°á»›ng lĂªn trĂªn trong áº£nh.
        // VĂ¬ váº­y cáº§n Ä‘áº£o Z: khi worldZ nhá» hÆ¡n playerZ (=Báº¯c) thĂ¬ relY pháº£i dÆ°Æ¡ng
        // (=LĂªn).
        float relX = worldX - player.getPosition().x;
        float relY = -(worldZ - player.getPosition().z); // Äáº£o Z Ä‘á»ƒ khá»›p FBO

        // Xoay tá»a Ä‘á»™ náº¿u cáº§n (Heading-Up mode)
        if (rotationDeg != 0) {
            float rad = (float) Math.toRadians(rotationDeg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            float newX = relX * cos - relY * sin;
            float newY = relX * sin + relY * cos;

            relX = newX;
            relY = newY;
        }

        // Chuyá»ƒn sang tá»a Ä‘á»™ pixel trĂªn widget
        float mapX = mapCenterX + (relX * scale);
        float mapY = mapCenterY + (relY * scale);

        return tmpPos.set(mapX, mapY);
    }

    public List<GameObject> getVisibleObstacles() {
        return visibleObstacles;
    }

    public List<Interactable> getVisibleInteractables() {
        return visibleInteractables;
    }

    public List<GameObject> getVisibleRoads() {
        return visibleRoads;
    }

    public List<GameObject> getVisibleLands() {
        return visibleLands;
    }

    public List<GameObject> getVisibleTrees() {
        return visibleTrees;
    }

    public float getPlayerRotation() {
        // Tráº£ vá» gĂ³c quay cá»§a nhĂ¢n váº­t (Ä‘á»ƒ xoay mÅ©i tĂªn)
        // Cáº§n Ä‘áº£o dáº¥u hoáº·c cá»™ng trá»« tĂ¹y thuá»™c vĂ o há»‡ tá»a Ä‘á»™ cá»§a LibGDX vs Model
        return player.getRotation();
    }

    /**
     * Chuyá»ƒn ngÆ°á»£c tá»« tá»a Ä‘á»™ Minimap (UI) sang tá»a Ä‘á»™ World (3D)
     */
    public Vector3 mapToWorldCoords(float mapX, float mapY, float mapCenterX, float mapCenterY, float scale,
            float rotationDeg) {
        // Tá»a Ä‘á»™ tÆ°Æ¡ng Ä‘á»‘i so vá»›i tĂ¢m widget
        float relX = (mapX - mapCenterX) / scale;
        float relY = (mapY - mapCenterY) / scale;

        // Xoay tá»a Ä‘á»™ ngÆ°á»£c láº¡i náº¿u cĂ³ rotation
        if (rotationDeg != 0) {
            float rad = (float) Math.toRadians(-rotationDeg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            float newX = relX * cos - relY * sin;
            float newY = relX * sin + relY * cos;

            relX = newX;
            relY = newY;
        }

        // Chuyá»ƒn Ä‘á»•i ngÆ°á»£c láº¡i (Äáº£o ngÆ°á»£c relY vá» worldZ vĂ¬ Ä‘Ă£ Ä‘áº£o khi Ä‘i tá»«
        // World->Map)
        Vector3 playerPos = player.getPosition();
        return new Vector3(playerPos.x + relX, 0, playerPos.z - relY);
    }
}
