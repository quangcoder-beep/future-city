package com.futurecity.game.managers;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.audio.Sound;
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
        manager.load("models/city_map.glb", SceneAsset.class);
        manager.load("models/player_char.glb", SceneAsset.class);
        // Load more vehicles or NPCs here if any

        // --- 2. Load Textures ---
        // Login Screen Background
        manager.load("images/backgrounds/login_bg.png", Texture.class);

        manager.load("images/ui/panels/panel_basic.png", Texture.class);
        manager.load("images/ui/panels/panel_dialogue_1.png", Texture.class);
        manager.load("images/ui/panels/panel_dialogue_3.png", Texture.class);

        // --- NEW PREMIUM ASSETS (Craftpix) ---
        manager.load("images/ui/panels/window_premium.png", Texture.class);
        manager.load("images/ui/buttons/table_01.png", Texture.class);
        manager.load("images/ui/buttons/table_02.png", Texture.class);

        // Buttons
        manager.load("images/ui/buttons/button_regular.png", Texture.class);
        manager.load("images/ui/buttons/button_highlight.png", Texture.class);
        manager.load("images/ui/buttons/button_green.png", Texture.class);

        // Scrollbars
        manager.load("images/ui/scrollbars/scrollbar_bg.png", Texture.class);
        manager.load("images/ui/scrollbars/scrollbar_knob.png", Texture.class);

        // Social Buttons
        manager.load("images/ui/social/google_up.png", Texture.class);
        manager.load("images/ui/social/google_over.png", Texture.class);
        manager.load("images/ui/social/facebook_up.png", Texture.class);
        manager.load("images/ui/social/facebook_over.png", Texture.class);

        // Icon Apps
        manager.load("images/ui/icons/coin.png", Texture.class); // Shop
        manager.load("images/ui/icons/list.png", Texture.class); // Orders
        manager.load("images/ui/icons/gps.png", Texture.class); // GPS
        manager.load("images/ui/icons/chest.png", Texture.class); // Inventory
        manager.load("images/ui/icons/check.png", Texture.class); // Checkmark

        // Loading UI assets (if used later)
        manager.load("images/ui/hud/timer_bar.png", Texture.class);
        manager.load("images/ui/hud/timer_bg.png", Texture.class);

        // --- 3. Register font configuration ---
        manager.load("font14.ttf", BitmapFont.class, createFontParams("fonts/Orbitron-Regular.ttf", 14));
        manager.load("font18.ttf", BitmapFont.class, createFontParams("fonts/Orbitron-Medium.ttf", 18));
        manager.load("font24.ttf", BitmapFont.class, createFontParams("fonts/Orbitron-Bold.ttf", 24));
        manager.load("font32.ttf", BitmapFont.class, createFontParams("fonts/Orbitron-ExtraBold.ttf", 32));

        // --- 4. Load Sounds & Music ---
        // UI Sounds
        manager.load("sounds/ui/click.ogg", Sound.class);
        manager.load("sounds/ui/hover.ogg", Sound.class);
        manager.load("sounds/ui/switch.ogg", Sound.class);
        
        // Typing Sound (Original)
        manager.load("sounds/ui/tap.ogg", Sound.class);

        // Notifications
        manager.load("sounds/ui/notification.mp3", Sound.class);

        // Gameplay Sounds
        manager.load("sounds/gameplay/footstep00.ogg", Sound.class);
        manager.load("sounds/gameplay/footstep01.ogg", Sound.class);

        // Music (Preview tracks as background)
        manager.load("music/bgm_main.ogg", com.badlogic.gdx.audio.Music.class);
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
