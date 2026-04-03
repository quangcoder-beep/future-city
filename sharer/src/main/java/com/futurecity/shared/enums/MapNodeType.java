package com.futurecity.shared.enums;

/**
 * Phân loại các loại Node (Vật thể) trên bản đồ dựa trên tiền tố tên.
 * Hỗ trợ cả định dạng cũ (skyscraper) và mới (building_id_name).
 */
public enum MapNodeType {
    SHOP("shop", true, true, false, "Enter Shop"),
    SKYSCRAPER("skyscraper", true, true, false, "Delivery Target"),
    BUILDING("building", true, true, false, "Delivery Target"),
    HOUSE("house", true, true, false, "Delivery Target"),
    PROP("prop", true, false, false, ""),
    TREE("tree", true, false, false, ""),
    LAND_ISLAND("land_island", false, false, true, ""),
    LAND("land", false, false, true, ""),
    ROAD("road", false, false, true, ""),
    BRIDGE("bridge", false, false, true, ""),
    NPC("npc", false, true, false, "Talk"),
    UNKNOWN("", false, false, false, "");

    private final String prefix;
    private final boolean isObstacle;
    private final boolean isInteractable;
    private final boolean isTerrain;
    private final String defaultPrompt;

    MapNodeType(String prefix, boolean isObstacle, boolean isInteractable, boolean isTerrain, String defaultPrompt) {
        this.prefix = prefix;
        this.isObstacle = isObstacle;
        this.isInteractable = isInteractable;
        this.isTerrain = isTerrain;
        this.defaultPrompt = defaultPrompt;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isObstacle() {
        return isObstacle;
    }

    public boolean isInteractable() {
        return isInteractable;
    }

    public boolean isTerrain() {
        return isTerrain;
    }

    public String getDefaultPrompt() {
        return defaultPrompt;
    }

    /**
     * Tìm loại Node dựa trên tên ID. Chấp nhận cả prefix_ và prefix (direct match).
     */
    public static MapNodeType fromId(String id) {
        if (id == null || id.isEmpty())
            return UNKNOWN;
        String lowerId = id.toLowerCase();

        // 1. Ưu tiên khớp chế độ prefix (chuẩn Blender)
        for (MapNodeType type : values()) {
            if (type == UNKNOWN || type.prefix.isEmpty()) continue;
            if (lowerId.startsWith(type.prefix)) {
                if (lowerId.length() == type.prefix.length()) return type;
                char nextChar = lowerId.charAt(type.prefix.length());
                if (!Character.isLetter(nextChar)) return type;
            }
        }

        // 2. Chế độ "Tìm kiếm linh hoạt" cho các loại tương tác
        if (lowerId.contains("shop")) return SHOP;
        if (lowerId.contains("npc")) return NPC;
        if (lowerId.contains("house") || lowerId.contains("nha")) return HOUSE;
        if (lowerId.contains("building") || lowerId.contains("skyscraper") || lowerId.contains("tower")) return BUILDING;
        if (lowerId.contains("road") || lowerId.contains("duong")) return ROAD;
        if (lowerId.contains("bridge") || lowerId.contains("cau")) return BRIDGE;

        return UNKNOWN;
    }
}
