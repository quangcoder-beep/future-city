package com.futurecity.shared.utils;

import com.futurecity.shared.enums.MapNodeType;

/**
 * Chá»©a thĂ´ng tin Ä‘Ă£ phĂ¢n tĂ¡ch cá»§a má»™t Node tá»« file 3D.
 * NĂ³ lÆ°u trá»¯ thĂ´ng tin cá»§a má»™t Ä‘iá»ƒm (Node) trĂªn báº£n Ä‘á»“,
 * vĂ­ dá»¥ nhÆ°: Tá»a Ä‘á»™ (x, y, z), loáº¡i váº­t thá»ƒ á»Ÿ Ä‘Ă³ (CĂ¢y, NhĂ , ÄÆ°á»ng Ä‘i),
 * hoáº·c cĂ¡c thuá»™c tĂ­nh áº©n nhÆ° "cĂ³ thá»ƒ Ä‘i qua hay khĂ´ng".
 */
public class MapNodeData {
    public MapNodeType type;
    public String originalId; // TĂªn gá»‘c tá»« Blender (vd: npc_jane_Clothes)
    public String id; // TĂªn Ä‘á»‹nh danh (vd: jane)
    public String displayName; // TĂªn hiá»ƒn thá»‹ (vd: Clothes)
    public String prompt; // CĂ¢u lá»‡nh (vd: NĂ³i chuyá»‡n)

    public MapNodeData(MapNodeType type, String originalId, String id, String displayName, String prompt) {
        this.type = type;
        this.originalId = originalId;
        this.id = id;
        this.displayName = displayName;
        this.prompt = prompt;
    }
}
