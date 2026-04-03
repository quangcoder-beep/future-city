package com.futurecity.game.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Camera;
import net.mgsx.gltf.scene3d.scene.SceneManager;

/**
 * Rendering System.
 * Responsible for clearing the screen and rendering the entire 3D Scene.
 */
public class RenderSystem {

    /**
     * Renders a single frame.
     * 
     * @param sceneManager    Manages models, lights, shadows
     * @param collisionSystem Used for rendering debug collision boxes
     * @param camera          Current camera
     */
    public void render(SceneManager sceneManager, CollisionSystem collisionSystem, Camera camera) {
        // 1. XĂ³a mĂ n hĂ¬nh cÅ© (TĂ´ mĂ u ná»n xanh báº§u trá»i)
        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // 2. Render entire 3D Scene
        sceneManager.render();

        // 3. Render Debug Wireframes for collisions (if debugging)
        if (collisionSystem != null) {
            collisionSystem.renderDebug(camera);
        }
    }
}
