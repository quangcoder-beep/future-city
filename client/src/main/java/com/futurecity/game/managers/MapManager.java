package com.futurecity.game.managers;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.enums.MapNodeType;
import com.futurecity.game.entities.GameObject;
import com.futurecity.game.entities.Housing;
import com.futurecity.game.entities.Interactable;
import com.futurecity.game.entities.ShopHousing;
import com.futurecity.game.entities.SellerNPC;
import com.futurecity.shared.systems.NavigationGrid;
import com.futurecity.shared.utils.MapNodeData;
import com.futurecity.shared.utils.MapNodeParser;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all map data on the Client side.
 * Responsibilities:
 * - Loading and displaying the city model (City Scene).
 * - Classifying objects: Obstacles, Interactable objects (Shop, NPC).
 * - Building the Navigation Grid to serve the pathfinding feature.
 * - Calculating terrain height (Terrain Height) to place the main character
 * accurately on the ground.
 */
public class MapManager {
    private Scene cityScene;
    private List<GameObject> obstacles = new ArrayList<>();
    private List<Interactable> interactables = new ArrayList<>();
    // Data for Minimap
    private List<GameObject> roads = new ArrayList<>();
    private List<GameObject> lands = new ArrayList<>();
    private List<GameObject> trees = new ArrayList<>();
    private NavigationGrid navGrid;

    private static class TerrainMeshData {
        float[] vertices;
        short[] indices;
        int vertexStride;
        int posOffset;

        public TerrainMeshData(float[] vertices, short[] indices, int vertexStride, int posOffset) {
            this.vertices = vertices;
            this.indices = indices;
            this.vertexStride = vertexStride;
            this.posOffset = posOffset;
        }
    }

    private static class TerrainChunk {
        Node node;
        List<TerrainMeshData> meshDataList;
        BoundingBox localBounds;

        public TerrainChunk(Node node, List<TerrainMeshData> meshDataList) {
            this.node = node;
            this.meshDataList = meshDataList;
            this.localBounds = new BoundingBox();
            node.calculateBoundingBox(this.localBounds);
        }
    }

    private List<TerrainChunk> terrainChunks = new ArrayList<>();

    private final Ray ray = new Ray();
    private final Vector3 intersection = new Vector3();
    private final BoundingBox tmpBounds = new BoundingBox();
    private final Matrix4 tmpMatrix = new Matrix4();
    private final Vector3 v1 = new Vector3();
    private final Vector3 v2 = new Vector3();
    private final Vector3 v3 = new Vector3();

    public MapManager(SceneAsset cityAsset) {
        this.cityScene = new Scene(cityAsset.scene);
    }

    public void setMapScale(float scale) {
        cityScene.modelInstance.transform.setToScaling(scale, scale, scale);
        for (GameObject obj : obstacles) {
            if (obj instanceof Housing) {
                ((Housing) obj).refreshBounds();
            }
        }
    }

    /**
     * Prepare map data before entering the game.
     * Includes: Filtering objects, initializing the navigation grid, and processing
     * terrain data.
     */
    public void prepareMap() {
        // Ensure Matrix is calculated for nested nodes
        cityScene.modelInstance.calculateTransforms();

        obstacles.clear();
        interactables.clear();
        roads.clear();
        lands.clear();
        trees.clear();
        terrainChunks.clear();

        // System.out.println("DEBUG: Filtering objects (Nodes) RECURSIVELY in Map...");

        // Initialize NavigationGrid (example: 1000m x 1000m, 2m cell for high accuracy)
        // Get actual bounds from map
        BoundingBox mapBounds = new BoundingBox();
        cityScene.modelInstance.calculateBoundingBox(mapBounds);
        float width = mapBounds.getWidth();
        float depth = mapBounds.getDepth();
        float gridSize = 2.0f; // 2 meters per cell is quite stable
        int gridW = (int) (width / gridSize) + 2;
        int gridH = (int) (depth / gridSize) + 2;

        navGrid = new NavigationGrid(gridW, gridH, gridSize, mapBounds.min.x, mapBounds.min.z);

        for (Node node : cityScene.modelInstance.nodes) {
            walkNodesRecursive(node);
        }

        // System.out.println("=== MAP LOADING COMPLETE ===");
        // System.out.println("Obstacles: " + obstacles.size());
        // System.out.println("Interactables: " + interactables.size());
    }

    /**
     * Recursively traverse all Nodes in the 3D file to classify them.
     */
    private void walkNodesRecursive(Node node) {
        String nodeId = node.id;
        if (nodeId != null) {
            MapNodeData data = MapNodeParser.parse(nodeId);
            MapNodeType type = data.type;

            if (type != MapNodeType.UNKNOWN) {
                // System.out.println("   - Detected Node [" + nodeId + "] -> Type: " + type);
            }

            if (type.isInteractable()) {
                if (type == MapNodeType.SHOP) {
                    ShopHousing shop = new ShopHousing(cityScene.modelInstance, data);
                    interactables.add(shop);
                    // Add to obstacles too
                    obstacles.add(shop);
                } else if (type == MapNodeType.NPC) {
                    SellerNPC npc = new SellerNPC(cityScene.modelInstance, data);
                    interactables.add(npc);
                } else if (type == MapNodeType.BUILDING || type == MapNodeType.HOUSE
                        || type == MapNodeType.SKYSCRAPER) {
                    // Delivery targets on client
                    Housing house = new Housing(cityScene.modelInstance, nodeId);
                    interactables.add(house);
                    obstacles.add(house);
                }
            } else if (type.isObstacle()) {
                Housing house = new Housing(cityScene.modelInstance, nodeId);
                obstacles.add(house);
            }

            // Process terrain types for Minimap
            if (type == MapNodeType.ROAD || type == MapNodeType.BRIDGE) {
                com.futurecity.game.entities.EnvironmentObject road = new com.futurecity.game.entities.EnvironmentObject(
                        cityScene.modelInstance, nodeId);
                roads.add(road);

                // Bake into NavigationGrid
                bakeObjectToGrid(road, NavigationGrid.ROAD);
            } else if (type == MapNodeType.LAND || type == MapNodeType.LAND_ISLAND) {
                com.futurecity.game.entities.EnvironmentObject land = new com.futurecity.game.entities.EnvironmentObject(
                        cityScene.modelInstance, nodeId);
                lands.add(land);
            } else if (type == MapNodeType.TREE) {
                // Tree is both an obstacle (if it has collision) and a decoration
                // Here we treat it as a decoration to draw green on the map
                com.futurecity.game.entities.EnvironmentObject tree = new com.futurecity.game.entities.EnvironmentObject(
                        cityScene.modelInstance, nodeId);
                trees.add(tree);
            }

            if (type.isTerrain()) {
                cacheTerrainNode(node);
            }
        }

        if (node.hasChildren()) {
            for (Node child : node.getChildren()) {
                walkNodesRecursive(child);
            }
        }
    }

    /**
     * Mark the position of an object in the NavigationGrid.
     */
    private void bakeObjectToGrid(GameObject obj, byte type) {
        if (navGrid == null)
            return;

        BoundingBox bounds = obj.getWorldBounds();
        int minX = navGrid.worldToGridX(bounds.min.x);
        int maxX = navGrid.worldToGridX(bounds.max.x);
        int minZ = navGrid.worldToGridZ(bounds.min.z);
        int maxZ = navGrid.worldToGridZ(bounds.max.z);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                navGrid.setCell(x, z, type);
            }
        }
    }

    public NavigationGrid getNavGrid() {
        return navGrid;
    }

    /**
     * Store the mesh data of the terrain in memory for faster collision
     * calculation.
     */
    private void cacheTerrainNode(Node node) {
        List<TerrainMeshData> partsData = new ArrayList<>();
        for (NodePart part : node.parts) {
            Mesh mesh = part.meshPart.mesh;
            VertexAttribute posAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Position);
            if (posAttr == null)
                continue;
            int posOffset = posAttr.offset / 4;
            int stride = mesh.getVertexSize() / 4;
            float[] vertices = new float[mesh.getNumVertices() * stride];
            mesh.getVertices(vertices);
            short[] indices = new short[mesh.getNumIndices()];
            mesh.getIndices(indices);
            partsData.add(new TerrainMeshData(vertices, indices, stride, posOffset));
        }
        if (!partsData.isEmpty()) {
            terrainChunks.add(new TerrainChunk(node, partsData));
        }
    }

    /**
     * Get the exact height of the terrain at a position (x, z).
     * Uses Ray casting technique to shoot a ray from top to bottom.
     * 
     * @return The Y height at the given position, or 0 if no collision.
     */
    public float getTerrainHeight(float x, float currentY, float z) {
        float rayStartY = currentY + (1.1f * GameConstants.WORLD_SCALE);
        ray.origin.set(x, rayStartY, z);
        ray.direction.set(0, -1f, 0);
        float maxHight = 0f;
        for (int i = 0; i < terrainChunks.size(); i++) {
            TerrainChunk chunk = terrainChunks.get(i);
            // WorldBox for terrain must include globalTransform
            tmpBounds.set(chunk.localBounds)
                    .mul(chunk.node.globalTransform)
                    .mul(cityScene.modelInstance.transform);

            if (!Intersector.intersectRayBounds(ray, tmpBounds, null))
                continue;
            float exactHeight = checkCachedMeshIntersection(chunk.node, chunk.meshDataList, ray);
            if (exactHeight > maxHight)
                maxHight = exactHeight;
        }
        return maxHight;
    }

    /**
     * Check for detailed collision between a Ray and the triangle mesh of the
     * terrain.
     * Helps determine the exact standing position on inclined or rough surfaces.
     */
    private float checkCachedMeshIntersection(Node node, List<TerrainMeshData> meshDataList, Ray ray) {
        float hitHeight = -1000f;
        boolean hasHit = false;
        tmpMatrix.set(cityScene.modelInstance.transform).mul(node.globalTransform);
        for (TerrainMeshData data : meshDataList) {
            float[] vertices = data.vertices;
            short[] indices = data.indices;
            int stride = data.vertexStride;
            int posOffset = data.posOffset;
            for (int i = 0; i < indices.length; i += 3) {
                getVertexPos(vertices, indices[i] & 0xFFFF, stride, posOffset, v1);
                getVertexPos(vertices, indices[i + 1] & 0xFFFF, stride, posOffset, v2);
                getVertexPos(vertices, indices[i + 2] & 0xFFFF, stride, posOffset, v3);
                v1.mul(tmpMatrix);
                v2.mul(tmpMatrix);
                v3.mul(tmpMatrix);
                if (Intersector.intersectRayTriangle(ray, v1, v2, v3, intersection)) {
                    if (intersection.y > hitHeight) {
                        hitHeight = intersection.y;
                        hasHit = true;
                    }
                }
            }
        }
        return hasHit ? hitHeight : -1000f;
    }

    /**
     * Get the coordinates of a vertex from the array of vertices of the Mesh.
     */
    private void getVertexPos(float[] vertices, int index, int stride, int posOffset, Vector3 out) {
        int idx = index * stride + posOffset;
        out.set(vertices[idx], vertices[idx + 1], vertices[idx + 2]);
    }

    public Scene getCityScene() {
        return cityScene;
    }

    public List<GameObject> getObstacles() {
        return obstacles;
    }

    public List<Interactable> getInteractables() {
        return interactables;
    }

    public List<GameObject> getRoads() {
        return roads;
    }

    public List<GameObject> getLands() {
        return lands;
    }

    public List<GameObject> getTrees() {
        return trees;
    }
}
