package com.example.server_spring.services;

import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.example.server_spring.entity.ServerInteractable;
import com.example.server_spring.entity.ServerSellerNPC;
import com.example.server_spring.entity.ServerShopHousing;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.enums.MapNodeType;
import com.futurecity.shared.utils.MapNodeData;
import com.futurecity.shared.utils.MapNodeParser;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages interactable objects loaded from the map.
 */
@Service
public class WorldService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.server_spring.repository.ShopRepository shopRepo;

    private final Map<String, ServerInteractable> objects = new HashMap<>();
    private final List<String> scannedNodeIds = new ArrayList<>();

    /** Scans all nodes in the GLB map for shops/NPCs */
    public void loadFromMap(SceneAsset mapAsset) {
        objects.clear();
        scannedNodeIds.clear();
        if (mapAsset == null || mapAsset.scene == null)
            return;
        
        mapAsset.scene.model.calculateTransforms();
        float scale = GameConstants.MAP_SCALE;
        
        // Cải tiến: Quét tất cả root nodes của Model
        for (Node node : mapAsset.scene.model.nodes) {
            scanRecursive(node, scale);
        }
        
        // Detailed report
        int shops = 0, npcs = 0, houses = 0, buildings = 0;
        for (ServerInteractable obj : objects.values()) {
            MapNodeType type = obj.getData().type;
            if (type == MapNodeType.SHOP) shops++;
            else if (type == MapNodeType.NPC) npcs++;
            else if (type == MapNodeType.HOUSE) houses++;
            else if (type == MapNodeType.BUILDING) buildings++;
        }
        
        System.out.println("================ WORLD LOAD REPORT ================");
        System.out.println("[WORLD] Total nodes scanned: " + scannedNodeIds.size());
        System.out.println("[WORLD] Interactable objects found: " + objects.size());
        System.out.println("   -> SHOPS: " + shops);
        System.out.println("   -> NPCs: " + npcs);
        System.out.println("   -> HOUSES: " + houses);
        System.out.println("   -> BUILDINGS: " + buildings);
        System.out.println("===================================================");
        
        // Diagnostic: Dump all node names
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("node_dump.txt"), scannedNodeIds);
        } catch (Exception e) {}
    }

    private void scanRecursive(Node node, float scale) {
        if (node.id != null) {
            scannedNodeIds.add(node.id);
            MapNodeData data = MapNodeParser.parse(node.id);
            
            // If node is interactable (Shop, NPC, House, Building)
            if (data.type.isInteractable()) {
                Vector3 pos = new Vector3();
                // Get global translation from model space
                node.globalTransform.getTranslation(pos);
                pos.scl(scale);

                ServerInteractable obj = null;
                if (data.type == MapNodeType.SHOP) {
                    obj = new ServerShopHousing(data, pos);
                } else if (data.type == MapNodeType.NPC) {
                    obj = new ServerSellerNPC(data, pos);
                } else if (data.type == MapNodeType.HOUSE || data.type == MapNodeType.BUILDING || data.type == MapNodeType.SKYSCRAPER) {
                    // All residential types use ServerShopHousing for position storage
                    obj = new ServerShopHousing(data, pos); 
                }

                if (obj != null) {
                    // Store in map (use node.id as key to avoid parser ID duplicates)
                    objects.put(node.id, obj);
                    
                    // Auto-register Shop to Database
                    if (data.type == MapNodeType.SHOP) {
                        try {
                            shopRepo.upsertShop(data.id, data.displayName, pos.x, pos.y, pos.z);
                        } catch (Exception e) {
                            System.err.println("[WORLD] Upsert shop error " + data.id + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // Recursively scan children
        if (node.hasChildren()) {
            for (Node child : node.getChildren()) {
                scanRecursive(child, scale);
            }
        }
    }

    public ServerInteractable getObjectById(String id) {
        return objects.get(id);
    }

    /** Returns all shop IDs */
    public String[] getAllShopIds() {
        List<String> ids = new ArrayList<>();
        for (ServerInteractable obj : objects.values()) {
            if (obj.getData().type == MapNodeType.SHOP) {
                ids.add(obj.getData().id);
            }
        }
        return ids.toArray(new String[0]);
    }

    /** Returns coordinates for all NPCs and Buildings as delivery targets */
    public float[][] getAllDeliveryDestinations() {
        List<float[]> dests = new ArrayList<>();
        for (ServerInteractable obj : objects.values()) {
            MapNodeType type = obj.getData().type;
            if (type == MapNodeType.NPC || type == MapNodeType.HOUSE || type == MapNodeType.BUILDING || type == MapNodeType.SKYSCRAPER) {
                Vector3 p = obj.getPosition();
                dests.add(new float[] { p.x, p.y, p.z });
            }
        }
        return dests.toArray(new float[0][0]);
    }
}
