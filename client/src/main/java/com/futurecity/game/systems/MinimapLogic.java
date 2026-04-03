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
 * Responsible for calculating Minimap data.
 * - Converts World coordinates (3D) to Minimap coordinates (2D).
 * - Filters objects within display range.
 */
public class MinimapLogic {
    private MainCharactor player;
    private MapManager mapManager;

    // Temporary variables for calculations (avoid frequent allocations)
    private final Vector2 tmpPos = new Vector2();
    private final List<GameObject> visibleObstacles = new ArrayList<>();
    private final List<Interactable> visibleInteractables = new ArrayList<>();
    private final List<GameObject> visibleRoads = new ArrayList<>();
    private final List<GameObject> visibleLands = new ArrayList<>();
    private final List<GameObject> visibleTrees = new ArrayList<>();

    // Current path (received from server)
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
     * Update logic: Filters objects around player.
     * 
     * @param viewRadius Minimap display radius (World units)
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
        // Farther view distance for terrain
        float terrainRadiusSq = (viewRadius + 100) * (viewRadius + 100);

        // 1. Filter Buildings (Obstacles)
        for (GameObject obj : mapManager.getObstacles()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= radiusSq) {
                visibleObstacles.add(obj);
            }
        }

        // 2. Filter NPC/Shop (Interactables)
        for (Interactable obj : mapManager.getInteractables()) {
            if (obj instanceof GameObject) {
                GameObject go = (GameObject) obj;
                float distSq = Vector2.dst2(playerX, playerZ, go.getPosition().x, go.getPosition().z);
                if (distSq <= radiusSq) {
                    visibleInteractables.add(obj);
                }
            }
        }

        // 3. Filter Roads
        for (GameObject obj : mapManager.getRoads()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= terrainRadiusSq) {
                visibleRoads.add(obj);
            }
        }

        // 4. Filter Lands
        for (GameObject obj : mapManager.getLands()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= terrainRadiusSq) {
                visibleLands.add(obj);
            }
        }

        // 5. Filter Trees
        for (GameObject obj : mapManager.getTrees()) {
            float distSq = Vector2.dst2(playerX, playerZ, obj.getPosition().x, obj.getPosition().z);
            if (distSq <= radiusSq) {
                visibleTrees.add(obj);
            }
        }

        // 6. Client-Side Path Culling: Removes traversed GPS path nodes.
        // Prevents GPS line from extending behind the player.
        if (currentPath != null && currentPath.size() > 1) {
            boolean cull = true;
            while (cull && currentPath.size() > 2) {
                Vector3 n0 = currentPath.get(0);
                Vector3 n1 = currentPath.get(1);

                float d0 = Vector2.dst2(playerX, playerZ, n0.x, n0.z);
                float d1 = Vector2.dst2(playerX, playerZ, n1.x, n1.z);

                // Remove old waypoint if:
                // - Already passed/stepped on it (Dist < 4m = d0 < 16)
                // - OR next point is closer to player than old point (passed it)
                if (d0 < 16f || d1 < d0) {
                    currentPath.remove(0);
                } else {
                    cull = false;
                }
            }
        }
    }

    /**
     * Converts World (3D) coordinates to relative Minimap (2D) coordinates.
     * 
     * @param worldX     World X coordinate
     * @param worldZ     World Z coordinate (maps to Y in 2D)
     * @param mapCenterX Minimap center X
     * @param mapCenterY Minimap center Y
     * @param scale      Scale factor (World -> Map)
     * @return Transformed screen coordinates
     */
    public Vector2 worldToMapCoords(float worldX, float worldZ, float mapCenterX, float mapCenterY, float scale) {
        return worldToMapCoords(worldX, worldZ, mapCenterX, mapCenterY, scale, 0);
    }

    public Vector2 worldToMapCoords(float worldX, float worldZ, float mapCenterX, float mapCenterY, float scale,
            float rotationDeg) {
        // Relative coordinates to player
        // FBO Camera uses up=(0,0,-1): North (-Z) is Up on screen.
        // Therefore, invert Z: if worldZ < playerZ (North), relY must be positive (Up).
        float relX = worldX - player.getPosition().x;
        float relY = -(worldZ - player.getPosition().z); // Invert Z to match FBO

        // Rotate coordinates if needed (Heading-Up mode)
        if (rotationDeg != 0) {
            float rad = (float) Math.toRadians(rotationDeg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            float newX = relX * cos - relY * sin;
            float newY = relX * sin + relY * cos;

            relX = newX;
            relY = newY;
        }

        // Convert to pixel coordinates on widget
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
        // Returns character rotation (for arrow rotation)
        // Sign inversion may be needed depending on coordinate system
        return player.getRotation();
    }

    /**
     * Converts Minimap (UI) coordinates back to World (3D) coordinates.
     */
    public Vector3 mapToWorldCoords(float mapX, float mapY, float mapCenterX, float mapCenterY, float scale,
            float rotationDeg) {
        // Relative coordinates to widget center
        float relX = (mapX - mapCenterX) / scale;
        float relY = (mapY - mapCenterY) / scale;

        // Invert rotation if any
        if (rotationDeg != 0) {
            float rad = (float) Math.toRadians(-rotationDeg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            float newX = relX * cos - relY * sin;
            float newY = relX * sin + relY * cos;

            relX = newX;
            relY = newY;
        }

        // Reverse conversion (Invert relY back to worldZ as it was inverted in worldToMap)
        Vector3 playerPos = player.getPosition();
        return new Vector3(playerPos.x + relX, 0, playerPos.z - relY);
    }
}
