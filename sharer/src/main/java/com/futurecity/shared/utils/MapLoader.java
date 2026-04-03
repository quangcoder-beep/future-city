package com.futurecity.shared.utils;

import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles loading map data (obstacles and roads) from SceneAsset.
 */
public class MapLoader {

    public static class MapData {
        public List<BoundingBox> obstacles = new ArrayList<>();
        public List<RoadData> roads = new ArrayList<>();
        public BoundingBox fullBounds = new BoundingBox();
    }

    public static class RoadData {
        public BoundingBox bounds;
        public byte type; 

        public RoadData(BoundingBox bounds, byte type) {
            this.bounds = bounds;
            this.type = type;
        }
    }

    /**
     * Extracts map data from GLB scene.
     */
    public static MapData extractMapData(SceneAsset sceneAsset, float worldScale) {
        MapData mapData = new MapData();
        mapData.fullBounds.inf();
        sceneAsset.scene.model.calculateTransforms();
        for (Node node : sceneAsset.scene.model.nodes) {
            processNodeRecursive(node, mapData, worldScale);
        }
        return mapData;
    }

    private static void processNodeRecursive(Node node, MapData mapData, float scale) {
        // Collect bounds of ONLY this node's meshes
        BoundingBox nodeMeshBox = new BoundingBox();
        nodeMeshBox.inf();
        boolean hasMesh = false;
        
        for (NodePart part : node.parts) {
            BoundingBox partBox = new BoundingBox();
            part.meshPart.mesh.calculateBoundingBox(partBox, part.meshPart.offset, part.meshPart.size);
            partBox.mul(node.globalTransform);
            nodeMeshBox.ext(partBox);
            hasMesh = true;
        }

        if (hasMesh && nodeMeshBox.isValid()) {
            Matrix4 cityTransform = new Matrix4().setToScaling(scale, scale, scale);
            BoundingBox worldBox = new BoundingBox().set(nodeMeshBox).mul(cityTransform);
            
            // Expand global bounds
            mapData.fullBounds.ext(worldBox);

            if (node.id != null) {
                MapNodeData data = MapNodeParser.parse(node.id);

                if (data.type.isObstacle()) {
                    mapData.obstacles.add(worldBox);
                } else if (data.type == com.futurecity.shared.enums.MapNodeType.ROAD
                        || data.type == com.futurecity.shared.enums.MapNodeType.BRIDGE) {
                    mapData.roads.add(new RoadData(worldBox, (byte) 1)); 
                }
            }
        }

        if (node.hasChildren()) {
            for (Node child : node.getChildren()) {
                processNodeRecursive(child, mapData, scale);
            }
        }
    }
}
