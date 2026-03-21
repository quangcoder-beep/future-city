package com.futurecity.game.managers;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import net.mgsx.gltf.loaders.glb.GLBAssetLoader;
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class MyAssetManager {
    public AssetManager manager = new AssetManager();

    public MyAssetManager() {
        FileHandleResolver resolver = new InternalFileHandleResolver();

        // 1. Register 3D loader (.glb / .gltf)
        manager.setLoader(SceneAsset.class, ".glb", new GLBAssetLoader(resolver));
        manager.setLoader(SceneAsset.class, ".gltf", new GLTFAssetLoader(resolver));

        // 2. Register Freetype loader for automatic .ttf to BitmapFont loading
        manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
    }

    /**
     * Load all assets (Images, Fonts, Models)
     */
    public void loadAssets() {
        // --- 1. Load 3D Models ---
        manager.load("models/currentcity.glb", SceneAsset.class);
        manager.load("models/mainCharactor.glb", SceneAsset.class);
        // Load more vehicles or NPCs here if any

        // --- 2. Load Textures ---
        // Login Screen Background
        manager.load("ui/backgrounds/1/Day/1.png", Texture.class);

        // NinePatch backgrounds for BasePanel, DialoguePanel, Delivery...
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/white assets/panels/panel-2.png", Texture.class);
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/white assets/panels/panel-dialogue-1.png", Texture.class);
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/white assets/panels/panel-dialogue-3.png", Texture.class);

        // Buttons
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/buttons/button-regular.png", Texture.class);
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/buttons/button-highlight.png", Texture.class);
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/buttons/button-green.png", Texture.class);

        // Scrollbars
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/sliders and lines/scrollbar-bg.png", Texture.class);
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/sliders and lines/scrollbar-1.png", Texture.class);

        // Icon Apps
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/icons/icon-coin-1.png", Texture.class); // Shop
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/icons/icon-list.png", Texture.class); // Orders
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/icons/icon-pointer-1.png", Texture.class); // GPS
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/icons/icon-chest.png", Texture.class); // Inventory
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/icons/icon-check.png", Texture.class); // Checkmark

        // Loading UI assets (if used later)
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/hud/timer-bar.png", Texture.class);
        manager.load("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/hud/timer-bg.png", Texture.class);

        // --- 3. Register font configuration ---
        manager.load("font14.ttf", BitmapFont.class, createFontParams("ui/fonts/Orbitron-Regular.ttf", 14));
        manager.load("font18.ttf", BitmapFont.class, createFontParams("ui/fonts/Orbitron-Medium.ttf", 18));
        manager.load("font24.ttf", BitmapFont.class, createFontParams("ui/fonts/Orbitron-Bold.ttf", 24));
        manager.load("font32.ttf", BitmapFont.class, createFontParams("ui/fonts/Orbitron-ExtraBold.ttf", 32));
    }

    /**
     * Helper method to create font parameters for FreeTypeFontLoader
     */
    private FreetypeFontLoader.FreeTypeFontLoaderParameter createFontParams(String fontFileName, int size) {
        FreetypeFontLoader.FreeTypeFontLoaderParameter params = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        params.fontFileName = fontFileName;
        params.fontParameters.size = size;
        params.fontParameters.minFilter = Texture.TextureFilter.Linear;
        params.fontParameters.magFilter = Texture.TextureFilter.Linear;

        // Sinh list kĂ½ tá»± tiáº¿ng Viá»‡t + thÆ°á»ng
        StringBuilder chars = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS);
        for (char c = 0x0080; c <= 0x00FF; c++)
            chars.append(c);
        for (char c = 0x0100; c <= 0x017F; c++)
            chars.append(c);
        for (char c = 0x0180; c <= 0x024F; c++)
            chars.append(c);
        for (char c = 0x0300; c <= 0x036F; c++)
            chars.append(c);
        for (char c = 0x1E00; c <= 0x1EFF; c++)
            chars.append(c);
        params.fontParameters.characters = chars.toString();

        return params;
    }

    // Check if assets are loaded (returns progress percentage)
    public float getProgress() {
        return manager.getProgress();
    }

    // Get assets after loading
    public <T> T get(String fileName, Class<T> type) {
        return manager.get(fileName, type);
    }

    public void dispose() {
        manager.dispose();
    }
}
