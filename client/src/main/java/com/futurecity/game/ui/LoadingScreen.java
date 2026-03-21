package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.futurecity.game.core.Main;

public class LoadingScreen extends ScreenAdapter {
    private final Main game;
    private Viewport viewport;
    private SpriteBatch batch;
    private BitmapFont font;
    
    // Náº¡p táº¡m 2 bá»©c áº£nh thanh Loading trá»±c tiáº¿p (vĂ¬ chĂºng dung lÆ°á»£ng cá»±c nhá», cáº§n hiá»ƒn thá»‹ ngay)
    private Texture barBgTex;
    private Texture barFgTex;
    private NinePatch barBgPatch;
    private NinePatch barFgPatch;

    private float progress = 0f;
    private float smoothProgress = 0f;

    public enum LoadingType {
        ASSETS, INDETERMINATE
    }

    private LoadingType type;
    private String message;
    private float timeElapsed = 0f;

    // Constructor máº·c Ä‘á»‹nh náº¡p Asset
    public LoadingScreen(Main game) {
        this(game, LoadingType.ASSETS, "LOADING ASSETS...");
        // Báº¯t Ä‘áº§u ra lá»‡nh cho AssetManager tiáº¿n hĂ nh táº£i
        game.myAssetManager.loadAssets();
    }

    // Constructor dĂ¹ng chung cho Chá» máº¡ng (Login, v.v...)
    public LoadingScreen(Main game, LoadingType type, String message) {
        this.game = game;
        this.type = type;
        this.message = message;
        this.viewport = new ExtendViewport(800, 600);
        this.batch = new SpriteBatch();
        this.font = new BitmapFont(); 
        
        try {
            barBgTex = new Texture(Gdx.files.internal("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/hud/timer-bg.png"));
            barFgTex = new Texture(Gdx.files.internal("ui/Sci-Fi-UI-Game-Asset-Pack/green assets/hud/timer-bar.png"));
            barBgPatch = new NinePatch(barBgTex, 10, 10, 10, 10);
            barFgPatch = new NinePatch(barFgTex, 10, 10, 10, 10);
        } catch(Exception e) {
            System.err.println("Could not load loading bar textures: " + e.getMessage());
        }
    }

    @Override
    public void render(float delta) {
        if (type == LoadingType.ASSETS) {
            if (game.myAssetManager.manager.update()) {
                progress = 1.0f;
                if (smoothProgress >= 0.99f) {
                    SkinLoader.initializeFromAssetManager(game.myAssetManager);
                    game.setScreen(new LoginScreen(game));
                    return;
                }
            } else {
                progress = game.myAssetManager.manager.getProgress();
            }
            smoothProgress = Interpolation.linear.apply(smoothProgress, progress, 0.1f);
        } else {
            // Cháº¿ Ä‘á»™ chá» vĂ´ Ä‘á»‹nh (Chá» máº¡ng) - Thanh cháº¡y ngang láº¡i
            timeElapsed += delta * 2f;
            smoothProgress = (float) (Math.sin(timeElapsed) + 1f) / 2f; // Cháº¡y tá»« 0 -> 1 -> 0
        }

        // Váº½ mĂ n hĂ¬nh
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        float stageW = viewport.getWorldWidth();
        float stageH = viewport.getWorldHeight();

        float barWidth = 400;
        float barHeight = 40;
        float barX = (stageW - barWidth) / 2;
        float barY = stageH / 4;

        // Váº½ chá»¯
        String text = message;
        if (type == LoadingType.ASSETS) {
            text += " " + (int)(smoothProgress * 100) + "%";
        }
        font.setColor(Color.LIME);
        font.draw(batch, text, barX, barY + barHeight + 20);

        // Váº½ thanh Loading
        if (barBgPatch != null && barFgPatch != null) {
            barBgPatch.draw(batch, barX, barY, barWidth, barHeight);
            float fgWidth = Math.max(0, barWidth * smoothProgress);
            if (fgWidth > 10) { 
                barFgPatch.draw(batch, barX, barY, fgWidth, barHeight);
            }
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (barBgTex != null) barBgTex.dispose();
        if (barFgTex != null) barFgTex.dispose();
    }
}
