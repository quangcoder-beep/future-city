package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisTextButton;

/**
 * Premium Cyberpunk Design System.
 * Centralizes all UI styles, drawables, and typography for consistent look.
 */
public class SkinLoader {

    // ═══ CYBERPUNK COLOR PALETTE ═══
    public static final Color NEON_CYAN = new Color(0f, 0.9f, 1f, 1f);
    public static final Color NEON_MAGENTA = new Color(1f, 0f, 0.6f, 1f);
    public static final Color NEON_GOLD = new Color(1f, 0.85f, 0.2f, 1f);
    public static final Color NEON_LIME = new Color(0.2f, 1f, 0.4f, 1f);
    public static final Color NEON_RED = new Color(1f, 0.2f, 0.2f, 1f);
    public static final Color BG_DARK = new Color(0.06f, 0.06f, 0.1f, 0.92f);
    public static final Color BG_CARD = new Color(0.08f, 0.08f, 0.14f, 0.88f);
    public static final Color BG_SURFACE = new Color(0.1f, 0.1f, 0.16f, 0.75f);
    public static final Color TEXT_DIM = new Color(1f, 1f, 1f, 0.45f);
    public static final Color TEXT_SECONDARY = new Color(0.7f, 0.8f, 0.9f, 0.8f);
    public static final Color SEPARATOR = new Color(0.2f, 0.3f, 0.4f, 0.6f);

    public static void initializeFromAssetManager(com.futurecity.game.managers.MyAssetManager assetManager) {
        if (!VisUI.isLoaded()) {
            VisUI.load();
        }
        Skin skin = VisUI.getSkin();

        // ═══ 1. FONTS ═══
        BitmapFont font14 = assetManager.get("font14.ttf", BitmapFont.class);
        BitmapFont font18 = assetManager.get("font18.ttf", BitmapFont.class);
        BitmapFont font24 = assetManager.get("font24.ttf", BitmapFont.class);
        BitmapFont font32 = assetManager.get("font32.ttf", BitmapFont.class);

        skin.add("small-font", font14, BitmapFont.class);
        skin.add("default-font", font18, BitmapFont.class);
        skin.add("title-font", font24, BitmapFont.class);
        skin.add("hud-font", font32, BitmapFont.class);

        // ═══ 2. RUNTIME DRAWABLES (Pixmap-generated) ═══
        createRuntimeDrawables(skin);

        // ═══ 3. PANEL & BUTTON TEXTURES ═══
        try {
            // Original Panel (for compatibility)
            com.badlogic.gdx.graphics.Texture panelTex = assetManager.get("images/ui/panels/panel_basic.png", com.badlogic.gdx.graphics.Texture.class);
            skin.add("panel-bg", new NinePatchDrawable(new NinePatch(panelTex, 30, 30, 30, 30)), Drawable.class);

            // Textfield background
            skin.add("textfield-bg", skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.8f)), Drawable.class);

            // PREMIUM PANEL (Craftpix)
            com.badlogic.gdx.graphics.Texture windowTex = assetManager.get("images/ui/panels/window_premium.png", com.badlogic.gdx.graphics.Texture.class);
            skin.add("panel-neon", new NinePatchDrawable(new NinePatch(windowTex, 60, 60, 90, 60)), Drawable.class);

            // PREMIUM BUTTON BASE
            com.badlogic.gdx.graphics.Texture btnBaseTex = assetManager.get("images/ui/buttons/table_02.png", com.badlogic.gdx.graphics.Texture.class);
            skin.add("button-neon-base", new NinePatchDrawable(new NinePatch(btnBaseTex, 20, 20, 10, 10)), Drawable.class);

            // Neon Button Style
            VisTextButton.VisTextButtonStyle neonBtnStyle = new VisTextButton.VisTextButtonStyle();
            neonBtnStyle.up = skin.getDrawable("button-neon-base");
            neonBtnStyle.over = skin.newDrawable("button-neon-base", NEON_CYAN);
            neonBtnStyle.down = skin.newDrawable("button-neon-base", Color.GRAY);
            neonBtnStyle.font = font18;
            neonBtnStyle.fontColor = Color.WHITE;
            neonBtnStyle.overFontColor = NEON_GOLD;
            skin.add("neon-button", neonBtnStyle);

        } catch (Exception e) {
            System.err.println("[SkinLoader] Error loading premium assets: " + e.getMessage());
        }

        // ═══ 4. SOCIAL LOGIN BUTTONS ═══
        try {
            com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle googleStyle = new com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle();
            googleStyle.up = new TextureRegionDrawable(new TextureRegion(assetManager.get("images/ui/social/google_up.png", com.badlogic.gdx.graphics.Texture.class)));
            googleStyle.over = new TextureRegionDrawable(new TextureRegion(assetManager.get("images/ui/social/google_over.png", com.badlogic.gdx.graphics.Texture.class)));
            skin.add("google-social", googleStyle, com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle.class);

            com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle fbStyle = new com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle();
            fbStyle.up = new TextureRegionDrawable(new TextureRegion(assetManager.get("images/ui/social/facebook_up.png", com.badlogic.gdx.graphics.Texture.class)));
            fbStyle.over = new TextureRegionDrawable(new TextureRegion(assetManager.get("images/ui/social/facebook_over.png", com.badlogic.gdx.graphics.Texture.class)));
            skin.add("facebook-social", fbStyle, com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle.class);
        } catch (Exception e) {
            System.err.println("[SkinLoader] Error loading social buttons: " + e.getMessage());
        }

        // ═══ 5. ICONS ═══
        try {
            String iconPath = "images/ui/icons/";
            skin.add("icon-shop", new TextureRegionDrawable(new TextureRegion(assetManager.get(iconPath + "coin.png", com.badlogic.gdx.graphics.Texture.class))), Drawable.class);
            skin.add("icon-list", new TextureRegionDrawable(new TextureRegion(assetManager.get(iconPath + "list.png", com.badlogic.gdx.graphics.Texture.class))), Drawable.class);
            skin.add("icon-gps", new TextureRegionDrawable(new TextureRegion(assetManager.get(iconPath + "gps.png", com.badlogic.gdx.graphics.Texture.class))), Drawable.class);
            skin.add("icon-chest", new TextureRegionDrawable(new TextureRegion(assetManager.get(iconPath + "chest.png", com.badlogic.gdx.graphics.Texture.class))), Drawable.class);
            skin.add("icon-check", new TextureRegionDrawable(new TextureRegion(assetManager.get(iconPath + "check.png", com.badlogic.gdx.graphics.Texture.class))), Drawable.class);
        } catch (Exception e) {
            System.err.println("[SkinLoader] Error loading icons: " + e.getMessage());
        }

        // ═══ 6. TYPOGRAPHY STYLES ═══
        try {
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle labelStyle = skin.get("default", com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class);
            labelStyle.font = font18;

            // Small
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle smallStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            smallStyle.font = font14;
            skin.add("small", smallStyle);

            // Title
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle titleStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            titleStyle.font = font24;
            skin.add("title", titleStyle);

            // Title Neon (Cyan)
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle titleNeon = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(titleStyle);
            titleNeon.fontColor = NEON_CYAN;
            skin.add("title-neon", titleNeon);

            // Nameplate
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle nameplateNeon = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            nameplateNeon.fontColor = NEON_CYAN;
            skin.add("nameplate-neon", nameplateNeon);

            // Metadata (dim text)
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle metadataLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(smallStyle);
            metadataLabel.fontColor = TEXT_DIM;
            skin.add("metadata-label", metadataLabel);

            // Tab Active (Cyan, underlined feel)
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle tabActive = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            tabActive.fontColor = NEON_CYAN;
            skin.add("tab-active", tabActive);

            // Tab Inactive (Dim)
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle tabInactive = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            tabInactive.fontColor = TEXT_DIM;
            skin.add("tab-inactive", tabInactive);

            // HUD Label (large, cyan)
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle hudLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            hudLabel.font = font32;
            hudLabel.fontColor = NEON_CYAN;
            skin.add("hud-neon", hudLabel);

            // Gold accent
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle goldStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            goldStyle.fontColor = NEON_GOLD;
            skin.add("gold", goldStyle);

            // Success/Error styles
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle successStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            successStyle.fontColor = NEON_LIME;
            skin.add("success", successStyle);

            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle errorStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            errorStyle.fontColor = NEON_RED;
            skin.add("error", errorStyle);

        } catch (Exception e) {
            System.err.println("[SkinLoader] Error initializing typography: " + e.getMessage());
        }
    }

    /**
     * Creates all runtime drawables using Pixmap (no external texture files needed).
     */
    private static void createRuntimeDrawables(Skin skin) {
        // Card Background (dark with slight border)
        Pixmap cardPm = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        cardPm.setColor(BG_CARD);
        cardPm.fill();
        // Subtle border
        cardPm.setColor(new Color(NEON_CYAN.r, NEON_CYAN.g, NEON_CYAN.b, 0.15f));
        cardPm.drawRectangle(0, 0, 20, 20);
        Texture cardTex = new Texture(cardPm);
        skin.add("card-bg", new NinePatchDrawable(new NinePatch(cardTex, 4, 4, 4, 4)), Drawable.class);
        cardPm.dispose();

        // HUD Bar Background (glassmorphism-like)
        Pixmap hudBarPm = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        hudBarPm.setColor(BG_SURFACE);
        hudBarPm.fill();
        Texture hudBarTex = new Texture(hudBarPm);
        skin.add("hud-bar-bg", new NinePatchDrawable(new NinePatch(hudBarTex, 6, 6, 6, 6)), Drawable.class);
        hudBarPm.dispose();

        // Badge Circle (red notification badge)
        Pixmap badgePm = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        badgePm.setColor(NEON_RED);
        badgePm.fillCircle(8, 8, 7);
        Texture badgeTex = new Texture(badgePm);
        skin.add("badge-circle", new TextureRegionDrawable(new TextureRegion(badgeTex)), Drawable.class);
        badgePm.dispose();

        // Separator Neon (thin cyan line)
        Pixmap sepPm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        sepPm.setColor(new Color(NEON_CYAN.r, NEON_CYAN.g, NEON_CYAN.b, 0.4f));
        sepPm.fill();
        Texture sepTex = new Texture(sepPm);
        skin.add("separator-neon", new NinePatchDrawable(new NinePatch(sepTex, 1, 1, 1, 1)), Drawable.class);
        sepPm.dispose();

        // Keycap background (for tutorial key bindings)
        Pixmap keycapPm = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        keycapPm.setColor(new Color(0.05f, 0.05f, 0.12f, 0.9f));
        keycapPm.fill();
        keycapPm.setColor(new Color(NEON_CYAN.r, NEON_CYAN.g, NEON_CYAN.b, 0.5f));
        keycapPm.drawRectangle(0, 0, 20, 20);
        Texture keycapTex = new Texture(keycapPm);
        skin.add("keycap", new NinePatchDrawable(new NinePatch(keycapTex, 5, 5, 5, 5)), Drawable.class);
        keycapPm.dispose();

        // Tab Indicator (underline bar for active tab)
        Pixmap tabPm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        tabPm.setColor(NEON_CYAN);
        tabPm.fill();
        Texture tabTex = new Texture(tabPm);
        skin.add("tab-indicator", new NinePatchDrawable(new NinePatch(tabTex, 1, 1, 1, 1)), Drawable.class);
        tabPm.dispose();

        // Dark Overlay (for behind open panels)
        Pixmap overlayPm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        overlayPm.setColor(new Color(0, 0, 0, 0.6f));
        overlayPm.fill();
        Texture overlayTex = new Texture(overlayPm);
        skin.add("dark-overlay", new TextureRegionDrawable(new TextureRegion(overlayTex)), Drawable.class);
        overlayPm.dispose();

        // Surface bg (slightly lighter than card for hover states)
        Pixmap surfPm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        surfPm.setColor(BG_SURFACE);
        surfPm.fill();
        Texture surfTex = new Texture(surfPm);
        skin.add("surface-bg", new NinePatchDrawable(new NinePatch(surfTex, 1, 1, 1, 1)), Drawable.class);
        surfPm.dispose();

        // Row highlight (subtle cyan tint for hover)
        Pixmap rowHlPm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        rowHlPm.setColor(new Color(NEON_CYAN.r, NEON_CYAN.g, NEON_CYAN.b, 0.08f));
        rowHlPm.fill();
        Texture rowHlTex = new Texture(rowHlPm);
        skin.add("row-highlight", new NinePatchDrawable(new NinePatch(rowHlTex, 1, 1, 1, 1)), Drawable.class);
        rowHlPm.dispose();

        // Neon TextField Background
        Pixmap tfPm = new Pixmap(10, 10, Pixmap.Format.RGBA8888);
        tfPm.setColor(new Color(0, 0, 0, 0.4f));
        tfPm.fill();
        tfPm.setColor(NEON_CYAN);
        tfPm.drawLine(0, 9, 9, 9);
        Texture tfTex = new Texture(tfPm);
        skin.add("textfield-neon-bg", new NinePatchDrawable(new NinePatch(tfTex, 2, 2, 2, 2)), Drawable.class);
        tfPm.dispose();

        // Neon TextField Focus Background
        Pixmap tfFocPm = new Pixmap(10, 10, Pixmap.Format.RGBA8888);
        tfFocPm.setColor(new Color(0, 0, 0, 0.6f));
        tfFocPm.fill();
        tfFocPm.setColor(NEON_GOLD);
        tfFocPm.drawLine(0, 9, 9, 9);
        tfFocPm.drawLine(0, 8, 9, 8); // thicker bottom border
        Texture tfFocTex = new Texture(tfFocPm);
        skin.add("textfield-neon-focus", new NinePatchDrawable(new NinePatch(tfFocTex, 2, 2, 2, 2)), Drawable.class);
        tfFocPm.dispose();

        // Register neon text field style
        if (skin.has("default", com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle.class)) {
            com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle defaultTfStyle =
                    skin.get("default", com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle.class);
            com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle neonTfStyle =
                    new com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle(defaultTfStyle);
            neonTfStyle.background = skin.getDrawable("textfield-neon-bg");
            neonTfStyle.focusedBackground = skin.getDrawable("textfield-neon-focus");
            neonTfStyle.fontColor = Color.WHITE;
            skin.add("neon", neonTfStyle, com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle.class);
        }
    }
}
