package com.futurecity.game.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Camera;
import net.mgsx.gltf.scene3d.scene.SceneManager;

/**
 * Há»‡ thá»‘ng hiá»ƒn thá»‹ (Rendering System).
 * Chá»‹u trĂ¡ch nhiá»‡m xĂ³a mĂ n hĂ¬nh vĂ  váº½ toĂ n bá»™ Scene 3D.
 */
public class RenderSystem {

    /**
     * Thá»±c hiá»‡n viá»‡c váº½ má»™t khung hĂ¬nh (Frame)
     * 
     * @param sceneManager    Quáº£n lĂ½ cĂ¡c models, lights, shadows
     * @param collisionSystem DĂ¹ng Ä‘á»ƒ váº½ debug va cháº¡m
     * @param camera          Camera hiá»‡n táº¡i
     */
    public void render(SceneManager sceneManager, CollisionSystem collisionSystem, Camera camera) {
        // 1. XĂ³a mĂ n hĂ¬nh cÅ© (TĂ´ mĂ u ná»n xanh báº§u trá»i)
        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // 2. Váº½ toĂ n bá»™ Scene 3D
        sceneManager.render();

        // 3. Váº½ khung dĂ¢y Debug cho va cháº¡m (Náº¿u cáº§n kiá»ƒm tra collision)
        if (collisionSystem != null) {
            collisionSystem.renderDebug(camera);
        }
    }
}
