package com.futurecity.game.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;

public class EnvironmentObject extends GameObject {
    /**
     * Khá»Ÿi táº¡o Ä‘á»‘i tÆ°á»£ng mĂ´i trÆ°á»ng tá»« má»™t Node cá»¥ thá»ƒ trong ModelInstance.
     * 
     * @param instance Instance chá»©a mĂ´ hĂ¬nh.
     * @param nodeId   ID cá»§a Node Ä‘áº¡i diá»‡n cho váº­t thá»ƒ nĂ y.
     */
    public EnvironmentObject(ModelInstance instance, String nodeId) {
        super(instance);
        // TĂ¬m node trong model
        Node node = instance.getNode(nodeId);
        if (node != null) {
            // Cáº­p nháº­t transform cá»§a GameObject theo node
            // LÆ°u Ă½: GameObject.position Ä‘Æ°á»£c láº¥y tá»« transform cá»§a instance
            // NhÆ°ng á»Ÿ Ä‘Ă¢y chĂºng ta muá»‘n GameObject Ä‘áº¡i diá»‡n cho 1 bá»™ pháº­n (Node) cá»§a scene
            // to
            // NĂªn ta cáº§n set transform cá»§a GameObject (localBounds/worldBounds) theo node
            // nĂ y.

            // Logic cá»§a GameObject constructor hiá»‡n táº¡i láº¥y instance.transform
            // NhÆ°ng environment object lĂ  1 pháº§n cá»§a static scene.

            // Override láº¡i logic tĂ­nh bounds
            node.calculateBoundingBox(localBounds);
            worldBounds.set(localBounds).mul(node.globalTransform);

            // Cáº­p nháº­t vá»‹ trĂ­ trung tĂ¢m
            worldBounds.getCenter(position);
        }
    }

    @Override
    public void updateBoundsFromTransform() {
        // Static object khĂ´ng thay Ä‘á»•i vá»‹ trĂ­ thÆ°á»ng xuyĂªn
        // NhÆ°ng náº¿u map scale thay Ä‘á»•i thĂ¬ cáº§n update
        // Logic sáº½ phá»©c táº¡p hÆ¡n chĂºt vĂ¬ cáº§n node reference.
        // Äá»ƒ Ä‘Æ¡n giáº£n, ta giá»¯ nguyĂªn bounds Ä‘Ă£ tĂ­nh lĂºc init vĂ¬ map scale set 1 láº§n.
    }
}
