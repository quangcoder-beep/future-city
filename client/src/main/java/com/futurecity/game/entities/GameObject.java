package com.futurecity.game.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

/**
 * Lá»›p trá»«u tÆ°á»£ng cÆ¡ báº£n cho má»i Ä‘á»‘i tÆ°á»£ng trong tháº¿ giá»›i game.
 * Quáº£n lĂ½ mĂ´ hĂ¬nh 3D (ModelInstance), vá»‹ trĂ­ (position) vĂ  há»™p va cháº¡m
 * (BoundingBox).
 */
public abstract class GameObject {
    protected final ModelInstance instance;

    // Vá»‹ trĂ­ vĂ  tráº¡ng thĂ¡i di chuyá»ƒn
    public Vector3 position = new Vector3();
    public Vector3 oldPosition = new Vector3();
    public Vector3 direction = new Vector3();

    public BoundingBox localBounds = new BoundingBox(); // Há»™p gá»‘c (KhuĂ´n)
    public BoundingBox worldBounds = new BoundingBox(); // Há»™p thá»±c táº¿ (BĂ¡nh)

    protected GameObject(ModelInstance instance) {
        this.instance = instance;
        if (this.instance != null) {
            this.instance.transform.getTranslation(this.position);

            // 2. TĂ­nh toĂ¡n há»™p gá»‘c 1 láº§n duy nháº¥t
            instance.calculateBoundingBox(localBounds);
            // 3. Khá»Ÿi táº¡o há»™p thá»±c táº¿ láº§n Ä‘áº§u
            worldBounds.set(localBounds).mul(instance.transform);
        }
    }

    public Vector3 getPosition() {
        return position;
    }

    public ModelInstance getInstance() {
        return instance;
    }

    public Vector3 getOldPosition() {
        return oldPosition;
    }

    public BoundingBox getLocalBounds() {
        return localBounds;
    }

    public void setLocalBounds(BoundingBox localBounds) {
        this.localBounds = localBounds;
    }

    public BoundingBox getWorldBounds() {
        return worldBounds;
    }

    public void setWorldBounds(BoundingBox worldBounds) {
        this.worldBounds = worldBounds;
    }

    public void updateBoundsFromTransform() {
        if (instance != null) {
            worldBounds.set(localBounds).mul(instance.transform);
        }
    }
}
