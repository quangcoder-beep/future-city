package com.example.server_spring.entity;

import com.badlogic.gdx.math.Vector3;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.utils.MapNodeData;

/**
 * NPC bán hàng Authoritative trên Server.
 */
public class ServerSellerNPC implements ServerInteractable {
    private final String id;
    private final String displayName;
    private final Vector3 position;
    private final MapNodeData data;
    private String state = "idle";

    public ServerSellerNPC(MapNodeData data, Vector3 worldPosition) {
        this.data = data;
        this.id = data.id;
        this.displayName = data.displayName;
        this.position = new Vector3(worldPosition);
    }

    @Override
    public boolean interact(ServerPlayer player) {
        System.out.println("[NPC] " + player.getUsername() + " → " + displayName);
        this.state = "talking";
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
        return state;
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
        return InteractionUIType.NPC_DIALOGUE;
    }
}
