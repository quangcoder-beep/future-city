package com.futurecity.game.managers;

import com.badlogic.gdx.graphics.Color;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class LightManager {
    private DirectionalLightEx sun;
    private SceneManager sceneManager; // Keep reference to change Ambient Light

    public LightManager(DirectionalLightEx sun, SceneManager sceneManager) {
        this.sun = sun;
        this.sceneManager = sceneManager;

        // Default for light to be bright
        this.sun.intensity = 1.0f;
    }

    public void changeDir(float x, float y, float z) {
        sun.direction.set(x, y, z).nor();
    }

    public void changColor(Color color) {
        sun.color.set(color);
    }

    // Function to manage overall brightness
    public void setBrightness(float ambient, float sunIntensity) {
        sceneManager.setAmbientLight(ambient);
        sun.intensity = sunIntensity;
    }

    public DirectionalLightEx getSun() {
        return sun;
    }
}
