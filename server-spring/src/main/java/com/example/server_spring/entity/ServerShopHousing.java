package com.example.server_spring.entity;

import com.badlogic.gdx.math.Vector3;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.enums.MapNodeType;
import com.futurecity.shared.utils.MapNodeData;

/**
 * Tòa nhà hoặc Cửa hàng tương tác trên Server.
 */
public class ServerShopHousing implements ServerInteractable {
    private final String id;
    private final String displayName;
    private final Vector3 position;
    private final MapNodeData data;

    public ServerShopHousing(MapNodeData data, Vector3 worldPosition) {
        this.data = data;
        this.id = data.id;
        this.displayName = data.displayName;
        this.position = new Vector3(worldPosition);
    }

    @Override
    public boolean interact(ServerPlayer player) {
        System.out.println("[SHOP/HOUSE] " + player.getUsername() + " interacting with " + displayName);
        return true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Vector3 getPosition() {
        return position;
    }

    @Override
    public String getState() {
        return "closed";
    }

    @Override
    public boolean isVisibleToAll() {
        return true;
    }

    @Override
    public com.futurecity.shared.utils.MapNodeData getData() {
        return data;
    }

    @Override
    public InteractionUIType getUIType() {
        if (data.type == MapNodeType.SHOP) {
            String lower = displayName.toLowerCase();
            if (lower.contains("clothes") || lower.contains("quần áo"))
                return InteractionUIType.SHOP_CLOTHES;
            if (lower.contains("food") || lower.contains("đồ ăn"))
                return InteractionUIType.SHOP_FOOD;
            return InteractionUIType.SHOP_GENERAL;
        }
        return InteractionUIType.HOUSE_INFO;
    }
}
