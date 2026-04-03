package com.futurecity.game.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;

public class LightManager {
    private DirectionalLightEx sun;
    private SceneManager sceneManager;
    private SceneSkybox skybox;
    private float currentWorldTime = 800f;
    private float targetWorldTime = 800f; // Giờ đích từ server
    private float timeSpeed = 3.428f; // 7 phút thực = 24h game
    private Color sunColor = new Color(1, 1, 0.9f, 1);

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

    public void update(float delta) {
        // 1. Tiến tới giờ đích (Smooth Sync)
        // Nếu lệch quá xa (>100 đơn vị ~ 1h game) thì nhảy thẳng
        float diff = targetWorldTime - currentWorldTime;
        if (Math.abs(diff) > 1200f)
            diff -= Math.signum(diff) * 2400f; // Xử lý qua đêm

        if (Math.abs(diff) > 100f) {
            currentWorldTime = targetWorldTime;
        } else {
            // Nhích dần tới targetTime (lerp nhẹ)
            currentWorldTime += diff * delta * 2.0f;
        }

        // 2. Tự trôi thời gian như bình thường
        currentWorldTime += delta * (100f / 60f) * timeSpeed;

        if (currentWorldTime >= 2400f)
            currentWorldTime -= 2400f;
        if (currentWorldTime < 0)
            currentWorldTime += 2400f;

        updateLighting(currentWorldTime);
    }

    public void setSkybox(SceneSkybox skybox) {
        this.skybox = skybox;
    }

    public DirectionalLightEx getSun() {
        return sun;
    }

    /**
     * Đồng bộ thời gian từ server.
     */
    public void updateTime(float serverTime) {
        this.targetWorldTime = serverTime;
        // Không gán trực tiếp currentWorldTime nữa để tránh giật
    }

    private void updateLighting(float time) {
        // Chuyển đổi time sang góc (radians)
        // 06:00 (600) = Sunrise, 12:00 (1200) = Noon, 18:00 (1800) = Sunset
        float angle = ((time - 600f) / 2400f) * MathUtils.PI2;

        // Tính toán hướng mặt trời (xoay quanh trục X để mọc Đông lặn Tây)
        float dy = -MathUtils.sin(angle);
        float dz = -MathUtils.cos(angle);
        sun.direction.set(0.2f, dy, dz).nor();

        // Điều chỉnh cường độ và màu sắc
        float ambientIntensity;
        float sunIntensity;
        sunColor.set(1, 1, 0.9f, 1);

        if (dy < 0) {
            // BAN NGÀY & HOÀNG HÔN/BÌNH MINH (Mặt trời trên cao)
            // alpha = 1 (trưa nắng), alpha = 0 (đường chân trời)
            float alpha = -dy;
            sunIntensity = MathUtils.lerp(0.8f, 1.5f, alpha);
            ambientIntensity = MathUtils.lerp(0.4f, 0.8f, alpha);

            // Chuyển từ màu Cam (Hoàng hôn) sang màu Vàng nhạt (Trưa)
            sunColor.set(1.0f, 0.4f, 0.1f, 1.0f).lerp(1f, 1f, 0.9f, 1f, alpha);

            // Hiện lại Skybox ban ngày
            if (skybox != null) {
                sceneManager.setSkyBox(skybox);
            }

            sceneManager.environment.remove(ColorAttribute.Fog);
        } else {
            // BAN ĐÊM (Mặt trời dưới thấp)
            // alpha = 0 (đường chân trời), alpha = 1 (đêm sâu)
            float alpha = MathUtils.clamp(dy / 0.5f, 0, 1);
            sunIntensity = MathUtils.lerp(0.8f, 0f, alpha);
            ambientIntensity = MathUtils.lerp(0.4f, 0.15f, alpha);

            // Chuyển từ màu Cam sang màu Xanh tối
            sunColor.set(1.0f, 0.4f, 0.1f, 1.0f).lerp(0.1f, 0.1f, 0.2f, 1f, alpha);

            // Ẩn Skybox khi trời quá tối (alpha > 0.8)
            if (alpha > 0.8f && skybox != null) {
                sceneManager.setSkyBox(null);
            }

            // Sương mù tăng dần khi trời tối

            ColorAttribute fog = sceneManager.environment.get(ColorAttribute.class, ColorAttribute.Fog);
            if (fog == null) {
                sceneManager.environment.set(ColorAttribute.createFog(0, 0, 0, 1f));
                fog = sceneManager.environment.get(ColorAttribute.class, ColorAttribute.Fog);
            }
            fog.color.set(0.02f * alpha, 0.02f * alpha, 0.05f * alpha, 1f);
        }

        sceneManager.setAmbientLight(ambientIntensity);

        // Cập nhật thuộc tính AmbientLight trong Environment để shader PBR nhận diện
        // đúng
        ColorAttribute attr = sceneManager.environment.get(ColorAttribute.class, ColorAttribute.AmbientLight);
        if (attr != null) {
            attr.color.set(ambientIntensity, ambientIntensity, ambientIntensity, 1f);
        } else {
            sceneManager.environment
                    .set(ColorAttribute.createAmbientLight(ambientIntensity, ambientIntensity, ambientIntensity, 1f));
        }

        sun.intensity = sunIntensity;
        sun.color.set(sunColor);
    }
}
