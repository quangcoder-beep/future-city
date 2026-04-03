package com.example.server_spring.entity;

import com.badlogic.gdx.math.Vector3;
import com.futurecity.shared.enums.InteractionUIType;

public interface ServerInteractable {
    String getId();

    String getDisplayName();

    Vector3 getPosition();

    String getState();

    boolean isVisibleToAll();

    com.futurecity.shared.utils.MapNodeData getData();

    InteractionUIType getUIType();

    boolean interact(ServerPlayer player);
}
