package com.futurecity.game.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.collision.BoundingBox;

import com.futurecity.shared.enums.InteractionAction;

public class Housing extends GameObject implements Interactable {

    protected Node node;
    protected ModelInstance cityInstance;

    /**
     * Khá»Ÿi táº¡o má»™t tĂ²a nhĂ .
     * 
     * @param cityInstance Instance cá»§a toĂ n bá»™ thĂ nh phá»‘.
     * @param nodeId       ID cá»§a node tĂ²a nhĂ .
     */
    public Housing(ModelInstance cityInstance, String nodeId) {
        super(cityInstance);
        this.cityInstance = cityInstance;
        this.node = cityInstance.getNode(nodeId);

        // Khá»Ÿi táº¡o vĂ  cáº­p nháº­t vá»‹ trĂ­ tháº¿ giá»›i ngay khi táº¡o
        updateWorldBounds();
    }

    public void refreshBounds() {
        updateWorldBounds();
    }

    /**
     * Cáº­p nháº­t há»™p va cháº¡m thá»±c táº¿ trong tháº¿ giá»›i (World Space).
     * Phá»¥ thuá»™c vĂ o vá»‹ trĂ­ vĂ  tá»· lá»‡ Scale cá»§a thĂ nh phá»‘.
     */
    public void updateWorldBounds() {
        if (node == null)
            return;

        // 1. Thu tháº­p há»™p bao cá»§a Node vĂ  Children trong Model Space (1:1 vá»›i
        // Blender)
        localBounds.inf();
        collectMeshBoundsModelSpace(node, localBounds);

        // Náº¿u váº«n rá»—ng, Ä‘áº·t máº·c Ä‘á»‹nh nhá»
        if (!localBounds.isValid()) {
            localBounds.set(new com.badlogic.gdx.math.Vector3(0, 0, 0), new com.badlogic.gdx.math.Vector3(1, 1, 1));
        }

        // 2. Chuyá»ƒn tá»« Model Space -> World Space (nhĂ¢n vá»›i transform cá»§a
        // ModelInstance
        // - lĂºc nĂ y cĂ³ Scale 80x)
        worldBounds.set(localBounds).mul(cityInstance.transform);

        // Cáº­p nháº­t vá»‹ trĂ­ tĂ¢m logic
        worldBounds.getCenter(this.position);
    }

    private void collectMeshBoundsModelSpace(Node node, BoundingBox out) {
        BoundingBox partBox = new BoundingBox();
        for (NodePart part : node.parts) {
            partBox.inf();
            part.meshPart.mesh.calculateBoundingBox(partBox, part.meshPart.offset, part.meshPart.size);
            // Quan trá»ng: NhĂ¢n vá»›i globalTransform Ä‘á»ƒ Ä‘Æ°a vá» tá»a Ä‘á»™ Model
            // chuáº©n
            partBox.mul(node.globalTransform);
            out.ext(partBox);
        }

        if (node.hasChildren()) {
            for (Node child : node.getChildren()) {
                collectMeshBoundsModelSpace(child, out);
            }
        }
    }

    public void update(float deltaTime) {
        // TÄ©nh
    }

    // --- INTERACTABLE IMPLEMENTATION ---

    @Override
    public BoundingBox getCollisionBox() {
        return this.getWorldBounds();
    }

    @Override
    public void onInteract() {
        System.out.println("Checking building: " + node.id);
    }

    @Override
    public String getInteractionPrompt() {
        return "Press F to inspect";
    }

    @Override
    public float getRequiredDot() {
        return 0.5f;
    }

    @Override
    public String getNameId() {
        return node.id;
    }

    @Override
    public InteractionAction getAction() {
        return InteractionAction.INSPECT;
    }
}
