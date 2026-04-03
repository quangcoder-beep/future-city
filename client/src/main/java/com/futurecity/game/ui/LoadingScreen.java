package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.futurecity.game.core.Main;

/**
 * Premium Cyberpunk Loading Screen.
 * Features: Gradient bar, Orbitron font, floating particles, scanlines.
 */
public class LoadingScreen extends ScreenAdapter {
    private final Main game;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    // Fonts loaded directly (AssetManager not ready yet)
    private BitmapFont titleFont;
    private BitmapFont progressFont;
    private BitmapFont smallFont;
    private FreeTypeFontGenerator fontGenerator;

    // Background
    private Texture bgTexture;

    // Progress
    private float progress = 0f;
    private float smoothProgress = 0f;
    private float timeElapsed = 0f;

    // Particles for "data streaming" effect
    private static final int PARTICLE_COUNT = 60;
    private float[] particleX = new float[PARTICLE_COUNT];
    private float[] particleY = new float[PARTICLE_COUNT];
    private float[] particleSpeed = new float[PARTICLE_COUNT];
    private float[] particleAlpha = new float[PARTICLE_COUNT];
    private float[] particleSize = new float[PARTICLE_COUNT];

    // Title animation
    private float titleAlpha = 0f;
    private float subtitleAlpha = 0f;

    // Layout
    private GlyphLayout glyphLayout = new GlyphLayout();

    // Cyberpunk colors
    private static final Color CYAN = new Color(0f, 0.9f, 1f, 1f);
    private static final Color MAGENTA = new Color(1f, 0f, 0.6f, 1f);
    private static final Color DARK_BG = new Color(0.02f, 0.02f, 0.06f, 1f);

    public enum LoadingType {
        ASSETS, INDETERMINATE
    }

    private LoadingType type;
    private String message;

    public LoadingScreen(Main game) {
        this(game, LoadingType.ASSETS, "INITIALIZING SYSTEMS");
        game.myAssetManager.loadAssets();
    }

    public LoadingScreen(Main game, LoadingType type, String message) {
        this.game = game;
        this.type = type;
        this.message = message;
        this.viewport = new ExtendViewport(1280, 720);
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();

        loadDirectResources();
        initParticles();
    }

    /**
     * Load fonts and textures directly (bypassing AssetManager).
     */
    private void loadDirectResources() {
        try {
            fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Orbitron-Bold.ttf"));

            // Title font - large
            FreeTypeFontGenerator.FreeTypeFontParameter titleParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
            titleParams.size = 42;
            titleParams.color = Color.WHITE;
            titleParams.minFilter = Texture.TextureFilter.Linear;
            titleParams.magFilter = Texture.TextureFilter.Linear;
            titleFont = fontGenerator.generateFont(titleParams);

            // Progress font - medium
            FreeTypeFontGenerator.FreeTypeFontParameter progressParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
            progressParams.size = 18;
            progressParams.color = Color.WHITE;
            progressParams.minFilter = Texture.TextureFilter.Linear;
            progressParams.magFilter = Texture.TextureFilter.Linear;
            progressFont = fontGenerator.generateFont(progressParams);

            // Small font - status text
            FreeTypeFontGenerator.FreeTypeFontParameter smallParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
            smallParams.size = 13;
            smallParams.color = Color.WHITE;
            smallParams.minFilter = Texture.TextureFilter.Linear;
            smallParams.magFilter = Texture.TextureFilter.Linear;
            smallFont = fontGenerator.generateFont(smallParams);
        } catch (Exception e) {
            System.err.println("[LoadingScreen] Font load failed, using fallback: " + e.getMessage());
            titleFont = new BitmapFont();
            progressFont = new BitmapFont();
            smallFont = new BitmapFont();
        }

        try {
            bgTexture = new Texture(Gdx.files.internal("images/backgrounds/login_bg.png"));
        } catch (Exception e) {
            System.err.println("[LoadingScreen] Background load failed: " + e.getMessage());
        }
    }

    private void initParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i, true);
        }
    }

    private void resetParticle(int i, boolean randomY) {
        particleX[i] = MathUtils.random(0f, 1280f);
        particleY[i] = randomY ? MathUtils.random(0f, 720f) : 720f + MathUtils.random(20f, 100f);
        particleSpeed[i] = MathUtils.random(30f, 120f);
        particleAlpha[i] = MathUtils.random(0.1f, 0.5f);
        particleSize[i] = MathUtils.random(1f, 3f);
    }

    @Override
    public void render(float delta) {
        timeElapsed += delta;

        // Update progress
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
            smoothProgress = Interpolation.linear.apply(smoothProgress, progress, 0.08f);
        } else {
            timeElapsed += delta;
            smoothProgress = (float) (Math.sin(timeElapsed * 2f) + 1f) / 2f;
        }

        // Animate title
        titleAlpha = Math.min(1f, titleAlpha + delta * 1.5f);
        subtitleAlpha = Math.min(1f, Math.max(0f, subtitleAlpha + delta * 1.2f - 0.3f));

        // Update particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleY[i] -= particleSpeed[i] * delta;
            particleX[i] += MathUtils.sin(timeElapsed + i) * 0.3f;
            if (particleY[i] < -10f) {
                resetParticle(i, false);
            }
        }

        // --- DRAW ---
        Gdx.gl.glClearColor(DARK_BG.r, DARK_BG.g, DARK_BG.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        float stageW = viewport.getWorldWidth();
        float stageH = viewport.getWorldHeight();

        // 1. Background image
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        if (bgTexture != null) {
            batch.setColor(1, 1, 1, 0.15f); // Very subtle background
            batch.draw(bgTexture, 0, 0, stageW, stageH);
            batch.setColor(1, 1, 1, 1);
        }
        batch.end();

        // 2. Particles (data streaming)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(timeElapsed * 3f + i);
            float alpha = particleAlpha[i] * pulse;
            // Alternate between cyan and magenta particles
            if (i % 3 == 0) {
                shapeRenderer.setColor(CYAN.r, CYAN.g, CYAN.b, alpha);
            } else if (i % 3 == 1) {
                shapeRenderer.setColor(MAGENTA.r, MAGENTA.g, MAGENTA.b, alpha * 0.6f);
            } else {
                shapeRenderer.setColor(1f, 1f, 1f, alpha * 0.3f);
            }
            shapeRenderer.circle(particleX[i], particleY[i], particleSize[i]);
        }
        shapeRenderer.end();

        // 3. Progress bar (gradient CYAN → MAGENTA)
        float barWidth = 500f;
        float barHeight = 6f;
        float barX = (stageW - barWidth) / 2f;
        float barY = stageH * 0.28f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Bar background
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.8f);
        shapeRenderer.rect(barX - 2, barY - 2, barWidth + 4, barHeight + 4);

        // Bar fill (gradient)
        float fillWidth = barWidth * smoothProgress;
        if (fillWidth > 1f) {
            // Draw gradient by segments
            int segments = (int) fillWidth;
            float segWidth = fillWidth / Math.max(1, segments);
            for (int i = 0; i < segments; i++) {
                float t = (float) i / Math.max(1, segments);
                float r = CYAN.r + (MAGENTA.r - CYAN.r) * t;
                float g = CYAN.g + (MAGENTA.g - CYAN.g) * t;
                float b = CYAN.b + (MAGENTA.b - CYAN.b) * t;
                shapeRenderer.setColor(r, g, b, 1f);
                shapeRenderer.rect(barX + i * segWidth, barY, segWidth + 1, barHeight);
            }
        }

        // Glow effect at the tip of the bar
        if (fillWidth > 5f) {
            float glowX = barX + fillWidth;
            for (int g = 0; g < 3; g++) {
                float ga = 0.3f - g * 0.1f;
                float gs = 4f + g * 4f;
                shapeRenderer.setColor(CYAN.r, CYAN.g, CYAN.b, ga);
                shapeRenderer.circle(glowX, barY + barHeight / 2f, gs);
            }
        }
        shapeRenderer.end();

        // 4. Scanline overlay effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (float y = 0; y < stageH; y += 4) {
            shapeRenderer.setColor(0, 0, 0, 0.06f);
            shapeRenderer.rect(0, y, stageW, 2);
        }
        shapeRenderer.end();

        // 5. Text elements
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        // Title: "FUTURE CITY"
        titleFont.setColor(CYAN.r, CYAN.g, CYAN.b, titleAlpha);
        glyphLayout.setText(titleFont, "FUTURE CITY");
        float titleX = (stageW - glyphLayout.width) / 2f;
        float titleY = stageH * 0.65f;
        titleFont.draw(batch, "FUTURE CITY", titleX, titleY);

        // Subtitle: "DELIVERY PROTOCOL"
        smallFont.setColor(1f, 1f, 1f, subtitleAlpha * 0.5f);
        glyphLayout.setText(smallFont, "DELIVERY PROTOCOL v2.0");
        titleFont.setColor(1, 1, 1, 1); // Reset
        smallFont.draw(batch, "DELIVERY PROTOCOL v2.0",
                (stageW - glyphLayout.width) / 2f, titleY - 40);

        // Progress percentage
        int pct = (int) (smoothProgress * 100);
        String progressText = type == LoadingType.ASSETS
                ? message + " // " + pct + "%"
                : message;
        progressFont.setColor(CYAN.r, CYAN.g, CYAN.b, 0.9f);
        glyphLayout.setText(progressFont, progressText);
        progressFont.draw(batch, progressText,
                (stageW - glyphLayout.width) / 2f, barY - 15);

        // Bottom-left system text
        smallFont.setColor(1f, 1f, 1f, 0.25f);
        smallFont.draw(batch, "SYS: LOADING RESOURCES | NODE: LOCAL | STATUS: NOMINAL",
                20, 25);

        // Bottom-right version
        String version = "BUILD 2026.04";
        glyphLayout.setText(smallFont, version);
        smallFont.draw(batch, version, stageW - glyphLayout.width - 20, 25);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (titleFont != null) titleFont.dispose();
        if (progressFont != null) progressFont.dispose();
        if (smallFont != null) smallFont.dispose();
        if (fontGenerator != null) fontGenerator.dispose();
        if (bgTexture != null) bgTexture.dispose();
    }
}
