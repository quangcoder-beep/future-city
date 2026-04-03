package com.futurecity.game.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.futurecity.shared.enums.InteractionAction;
import com.futurecity.shared.utils.MapNodeData;

public class ShopHousing extends Housing implements Interactable {
    private String nameId;
    private String displayName;
    private String prompt;
    private InteractionAction action;

    public ShopHousing(ModelInstance instance, MapNodeData data) {
        super(instance, data.originalId);
        this.nameId = data.id;
        this.displayName = data.displayName;
        this.prompt = data.prompt;
        this.action = InteractionAction.OPEN;
    }

    @Override
    public BoundingBox getCollisionBox() {
        return this.getWorldBounds();
    }

    @Override
    public void onInteract() {
        System.out.println("Welcome to " + displayName + " (" + nameId + ")");
    }

    @Override
    public String getInteractionPrompt() {
        return prompt;
    }

    @Override
    public String getNameId() {
        return nameId;
    }

    @Override
    public InteractionAction getAction() {
        return action;
    }
}
