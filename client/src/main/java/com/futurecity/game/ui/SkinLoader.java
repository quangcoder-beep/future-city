package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.VisUI;

public class SkinLoader {

    public static void initializeFromAssetManager(com.futurecity.game.managers.MyAssetManager assetManager) {
        if (VisUI.isLoaded()) {
            return;
        }

        // 1. Nạp VisUI mặc định trước để đảm bảo cấu trúc JSON/Style chuẩn
        VisUI.load();
        Skin skin = VisUI.getSkin();

        // 2. Lấy các BitmapFont từ AssetManager
        BitmapFont font14 = assetManager.get("font14.ttf", BitmapFont.class);
        BitmapFont font18 = assetManager.get("font18.ttf", BitmapFont.class);
        BitmapFont font24 = assetManager.get("font24.ttf", BitmapFont.class);
        BitmapFont font32 = assetManager.get("font32.ttf", BitmapFont.class);

        // ÄÄƒng kĂ½ font vĂ o skin Ä‘á»ƒ dĂ¹ng trong JSON hoáº·c style
        skin.add("small-font", font14, BitmapFont.class);
        skin.add("default-font", font18, BitmapFont.class);
        skin.add("title-font", font24, BitmapFont.class);
        skin.add("hud-font", font32, BitmapFont.class);

        // 3. Nạp Texture NinePatch cho Panel
        try {
            com.badlogic.gdx.graphics.Texture panelTex = assetManager.get(
                    "ui/Sci-Fi-UI-Game-Asset-Pack/white assets/panels/panel-2.png",
                    com.badlogic.gdx.graphics.Texture.class);
            com.badlogic.gdx.graphics.g2d.NinePatch panelPatch = new com.badlogic.gdx.graphics.g2d.NinePatch(panelTex, 30, 30, 30, 30);
            skin.add("panel-bg", new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(panelPatch), com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);

            com.badlogic.gdx.graphics.Texture dialogTex = assetManager.get(
                    "ui/Sci-Fi-UI-Game-Asset-Pack/white assets/panels/panel-dialogue-1.png",
                    com.badlogic.gdx.graphics.Texture.class);
            com.badlogic.gdx.graphics.g2d.NinePatch dialogPatch = new com.badlogic.gdx.graphics.g2d.NinePatch(dialogTex, 30, 30, 30, 30);
            skin.add("dialogue-bg", new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(dialogPatch), com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);

            com.badlogic.gdx.graphics.Texture tfBgTex = assetManager.get(
                    "ui/Sci-Fi-UI-Game-Asset-Pack/white assets/panels/panel-dialogue-3.png",
                    com.badlogic.gdx.graphics.Texture.class);
            com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable tfBgDraw = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(tfBgTex, 10, 10, 10, 10));
            tfBgDraw.setMinHeight(42);
            tfBgDraw.setLeftWidth(12);
            tfBgDraw.setRightWidth(12);
            skin.add("textfield-bg", tfBgDraw, com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        } catch (Exception e) {
            // Error loading panel textures
        }

        // 4. Override Styles (Button, TextField, ScrollPane)
        try {
            com.badlogic.gdx.graphics.Texture btnRegTex = assetManager.get("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/buttons/button-regular.png", com.badlogic.gdx.graphics.Texture.class);
            com.badlogic.gdx.graphics.Texture btnOverTex = assetManager.get("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/buttons/button-highlight.png", com.badlogic.gdx.graphics.Texture.class);
            com.badlogic.gdx.graphics.Texture btnDownTex = assetManager.get("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/buttons/button-green.png", com.badlogic.gdx.graphics.Texture.class);

            com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable btnReg = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(btnRegTex, 15, 15, 15, 15));
            com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable btnOver = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(btnOverTex, 15, 15, 15, 15));
            com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable btnDown = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(btnDownTex, 15, 15, 15, 15));

            com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle btnStyle = skin.get("default", com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle.class);
            btnStyle.up = btnReg;
            btnStyle.over = btnOver;
            btnStyle.down = btnDown;
            btnStyle.font = font18;

            com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle tfStyle = skin.get("default", com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle.class);
            tfStyle.background = skin.getDrawable("textfield-bg");
            tfStyle.font = font18;

            com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle spStyle = skin.get("default", com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle.class);
            com.badlogic.gdx.graphics.Texture sbBgTex = assetManager.get("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/sliders and lines/scrollbar-bg.png", com.badlogic.gdx.graphics.Texture.class);
            com.badlogic.gdx.graphics.Texture sbKnobTex = assetManager.get("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/sliders and lines/scrollbar-1.png", com.badlogic.gdx.graphics.Texture.class);
            spStyle.vScroll = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(sbBgTex, 4, 4, 4, 4));
            spStyle.vScrollKnob = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(sbKnobTex, 4, 4, 4, 4));
        } catch (Exception e) {
            System.err.println("Error overriding styles: " + e.getMessage());
        }

        // 5. Icons
        try {
            String iconPath = "ui/Sci-Fi-UI-Game-Asset-Pack/green assets/icons/";
            skin.add("icon-shop", new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(assetManager.get(iconPath + "icon-coin-1.png", com.badlogic.gdx.graphics.Texture.class))), com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
            skin.add("icon-list", new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(assetManager.get(iconPath + "icon-list.png", com.badlogic.gdx.graphics.Texture.class))), com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
            skin.add("icon-gps", new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(assetManager.get(iconPath + "icon-pointer-1.png", com.badlogic.gdx.graphics.Texture.class))), com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
            skin.add("icon-chest", new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(assetManager.get(iconPath + "icon-chest.png", com.badlogic.gdx.graphics.Texture.class))), com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
            skin.add("icon-check", new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(assetManager.get(iconPath + "icon-check.png", com.badlogic.gdx.graphics.Texture.class))), com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        } catch (Exception e) {
            System.err.println("Error loading icons: " + e.getMessage());
        }

        // 6. Typography
        try {
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle labelStyle = skin.get("default", com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class);
            labelStyle.font = font18;
            labelStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;

            // ÄÄƒng kĂ½ cĂ¡c style bá»• sung
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle smallStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            smallStyle.font = font14;
            skin.add("small", smallStyle);

            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle titleStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(labelStyle);
            titleStyle.font = font24;
            skin.add("title", titleStyle);
        } catch (Exception e) {
            System.err.println("Error overriding typography: " + e.getMessage());
        }
    }
}
