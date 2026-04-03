package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.futurecity.game.core.GameScreen;
import com.futurecity.game.core.Main;
import com.futurecity.game.managers.HttpAuthClientManager;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.TokenStoreManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import java.util.Random;
import java.util.UUID;

/**
 * High-Fidelity Cyberpunk Login Screen.
 * Featuring Data-Stream Particles, Clean Card UI, and Swing-out Entrance.
 */
public class LoginScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private NetworkManager networkManager;

    private VisTable mainContainer;
    private VisTable cardTable;
    private VisTable formTable;
    private VisTextField userField;
    private VisTextField passField;
    private VisTextField nicknameField;
    private VisLabel statusLabel;
    private VisTextButton actionBtn;
    private VisLabel switchModeLink;

    private boolean isRegisterMode = false;
    private String currentPollingSessionId;
    private Timer.Task pollingTask;

    // Particle System (from LoadingScreen style)
    private static final int PARTICLE_COUNT = 80;
    private float[] particleX = new float[PARTICLE_COUNT];
    private float[] particleY = new float[PARTICLE_COUNT];
    private float[] particleSpeed = new float[PARTICLE_COUNT];
    private float[] particleAlpha = new float[PARTICLE_COUNT];
    private float[] particleSize = new float[PARTICLE_COUNT];
    private Color[] particleColors = new Color[PARTICLE_COUNT];
    private Random random = new Random();

    public LoginScreen(Main game) {
        this.game = game;
        this.stage = new Stage(new ExtendViewport(1280, 720)); // High-def baseline
        this.networkManager = new NetworkManager(game);

        initParticles();
        setupUI();
        setupNetwork();
        tryAutoLogin();
    }

    private void initParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i, true);
        }
    }

    private void resetParticle(int i, boolean randomizeY) {
        particleX[i] = random.nextFloat() * 1920; // assumed max width
        particleY[i] = randomizeY ? random.nextFloat() * 1080 : -50f;
        particleSpeed[i] = 20f + random.nextFloat() * 80f;
        particleAlpha[i] = 0.2f + random.nextFloat() * 0.6f;
        particleSize[i] = 2f + random.nextFloat() * 4f;

        // Colors: Cyan, Magenta, White
        int colorType = random.nextInt(3);
        if (colorType == 0) particleColors[i] = SkinLoader.NEON_CYAN;
        else if (colorType == 1) particleColors[i] = SkinLoader.NEON_MAGENTA;
        else particleColors[i] = new Color(1f, 1f, 1f, 0.8f);
    }

    private void updateAndDrawParticles(float delta) {
        com.badlogic.gdx.graphics.g2d.Batch batch = stage.getBatch();
        batch.begin();
        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        Texture whiteRegion = VisUI.getSkin().getRegion("white").getTexture();
        
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleY[i] += particleSpeed[i] * delta;
            
            // Subtly sway X
            particleX[i] += (float)Math.sin(particleY[i] * 0.01f + i) * 0.5f;

            if (particleY[i] > 1200) { // Off screen top
                resetParticle(i, false);
            }

            Color c = particleColors[i];
            batch.setColor(c.r, c.g, c.b, particleAlpha[i]);
            batch.draw(whiteRegion, particleX[i], particleY[i], particleSize[i], particleSize[i] * 4); // Elongated
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void setupUI() {
        Stack rootStack = new Stack();
        rootStack.setFillParent(true);
        stage.addActor(rootStack);

        // 1. Background layer (Static Image)
        try {
            Texture bgTex = game.myAssetManager.get("images/backgrounds/login_bg.png", Texture.class);
            Image bgImage = new Image(bgTex);
            bgImage.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            bgImage.setColor(new Color(0.3f, 0.3f, 0.4f, 1f)); // Dimmed
            rootStack.add(bgImage);

            // Dark Gradient/Grid Overlay
            Image overlay = new Image(VisUI.getSkin().getRegion("white"));
            overlay.setColor(SkinLoader.BG_DARK);
            overlay.getColor().a = 0.85f; // Heavy dimming for contrast
            rootStack.add(overlay);
        } catch (Exception ignored) {}

        // 2. Main UI Layer
        mainContainer = new VisTable();
        mainContainer.setFillParent(true);
        rootStack.add(mainContainer);

        // 3. Login Card (with dark background and animations)
        cardTable = new VisTable();
        cardTable.setBackground(VisUI.getSkin().getDrawable("card-bg"));
        cardTable.pad(50, 45, 40, 45);

        // Entrance Animation
        cardTable.setTransform(true);
        cardTable.setOrigin(com.badlogic.gdx.utils.Align.center);
        cardTable.addAction(Actions.sequence(
            Actions.scaleTo(0.8f, 0.8f),
            Actions.alpha(0),
            Actions.parallel(
                Actions.scaleTo(1f, 1f, 0.6f, Interpolation.swingOut),
                Actions.fadeIn(0.3f)
            )
        ));

        mainContainer.add(cardTable).width(520).center();

        buildCard();
    }

    private void buildCard() {
        cardTable.clear();

        // Logo
        try {
            Texture logoTex = new Texture(Gdx.files.internal("delivery-man.png"));
            Image logoImg = new Image(logoTex);
            cardTable.add(logoImg).size(64).padBottom(10).row();
        } catch (Exception ignored) {}

        // Header with Typing Effect
        final String fullTitle = "NEURAL LINK: FUTURE CITY";
        final VisLabel titleLabel = new VisLabel("", "title-neon");
        cardTable.add(titleLabel).padBottom(10).row();
        startTypingAnimation(titleLabel, fullTitle);

        // Technical Sub-header
        VisLabel subHeader = new VisLabel("SYSTEM AUTHORIZATION REQUIRED", "metadata-label");
        cardTable.add(subHeader).padBottom(30).row();

        // Separator
        Image sep = new Image(VisUI.getSkin().getDrawable("separator-neon"));
        cardTable.add(sep).height(2).width(150).padBottom(30).row();

        // Form Section
        formTable = new VisTable();
        refreshFormFields();
        cardTable.add(formTable).expandX().fillX().padBottom(30).row();

        // Main Action Button
        actionBtn = new VisTextButton(isRegisterMode ? "REGISTER IDENTITY" : "INITIALIZE LOGIN", "neon-button");
        cardTable.add(actionBtn).width(340).height(55).padBottom(20).row();

        // Switch Mode Link
        switchModeLink = new VisLabel(
                isRegisterMode ? "[ EXISTING CITIZEN ? LOGIN HERE ]" : "[ NEW CITIZEN ? REGISTER HERE ]",
                "metadata-label");
        switchModeLink.setColor(SkinLoader.TEXT_SECONDARY);
        cardTable.add(switchModeLink).padBottom(35).row();

        switchModeLink.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                switchModeLink.setColor(SkinLoader.NEON_CYAN);
                if (pointer == -1) game.audioManager.playSfx("sounds/ui/hover.ogg");
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                switchModeLink.setColor(SkinLoader.TEXT_SECONDARY);
            }
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.audioManager.playSfx("sounds/ui/click.ogg");
                isRegisterMode = !isRegisterMode;
                buildCard();
            }
        });

        // ═══ SOCIAL LOGIN ═══
        VisTable socialTable = new VisTable();
        VisLabel orLabel = new VisLabel("EXTERNAL PROTOCOLS", "small");
        orLabel.setColor(SkinLoader.TEXT_DIM);
        socialTable.add(orLabel).padRight(15);

        VisImageButton googleBtn = new VisImageButton(VisUI.getSkin().get("google-social", VisImageButton.VisImageButtonStyle.class));
        VisImageButton facebookBtn = new VisImageButton(VisUI.getSkin().get("facebook-social", VisImageButton.VisImageButtonStyle.class));
        socialTable.add(googleBtn).size(45, 45).padRight(15);
        socialTable.add(facebookBtn).size(45, 45);
        cardTable.add(socialTable).padBottom(25).row();

        // ═══ SYSTEM CONSOLE (Status Log) ═══
        VisTable consoleTable = new VisTable();
        consoleTable.setBackground(VisUI.getSkin().getDrawable("surface-bg"));
        consoleTable.pad(10, 15, 10, 15);

        VisLabel consolePrefix = new VisLabel("> ", "metadata-label");
        consolePrefix.setColor(SkinLoader.NEON_CYAN);
        consoleTable.add(consolePrefix).left();

        statusLabel = new VisLabel("SYSTEM READY. AWAITING INPUT.");
        statusLabel.setStyle(VisUI.getSkin().get("metadata-label", VisLabel.LabelStyle.class));
        statusLabel.setColor(SkinLoader.TEXT_SECONDARY);
        consoleTable.add(statusLabel).expandX().left();
        
        cardTable.add(consoleTable).expandX().fillX().row();

        setupListeners(googleBtn, facebookBtn);
        
        game.audioManager.attachDefaultTo(actionBtn);
        game.audioManager.attachDefaultTo(googleBtn);
        game.audioManager.attachDefaultTo(facebookBtn);
    }

    private void startTypingAnimation(final VisLabel label, final String text) {
        label.setText("");
        final String typeSoundPath = "sounds/ui/tap.ogg";
        
        Timer.schedule(new Timer.Task() {
            int index = 0;
            @Override
            public void run() {
                if (index <= text.length()) {
                    label.setText(text.substring(0, index));
                    if (index > 0 && index % 2 == 0) { // Play every 2nd char to avoid audio clipping
                        game.audioManager.playSfx(typeSoundPath, 0.2f, 0.05f, true);
                    }
                    index++;
                } else {
                    this.cancel();
                }
            }
        }, 0.2f, 0.06f, text.length());
    }

    private void refreshFormFields() {
        formTable.clear();
        String styleName = "neon"; // Use the new skin style
        
        if (isRegisterMode) {
            formTable.add(new VisLabel("CITIZEN NAME", "metadata-label")).left().padBottom(4).row();
            nicknameField = new VisTextField("", styleName);
            formTable.add(nicknameField).expandX().fillX().height(40).padBottom(18).row();
        }

        formTable.add(new VisLabel("LOGIN ID", "metadata-label")).left().padBottom(4).row();
        userField = new VisTextField("", styleName);
        formTable.add(userField).expandX().fillX().height(40).padBottom(18).row();

        formTable.add(new VisLabel("ENCRYPTED PASSKEY", "metadata-label")).left().padBottom(4).row();
        passField = new VisTextField("", styleName);
        passField.setPasswordMode(true);
        passField.setPasswordCharacter('•');
        formTable.add(passField).expandX().fillX().height(40).row();
    }

    private void updateConsole(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setColor(color);
    }

    private void setupListeners(VisImageButton google, VisImageButton fb) {
        actionBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleAuth(isRegisterMode);
            }
        });

        google.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startSocialLogin("google");
            }
        });
        fb.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startSocialLogin("facebook");
            }
        });
    }

    private void setupNetwork() {
        networkManager.setLoginListener(response -> {
            Gdx.app.postRunnable(() -> {
                if (response.status.equals("SUCCESS")) {
                    game.setScreen(new GameScreen(game, networkManager, response.needsNickname));
                } else {
                    TokenStoreManager.clear();
                    updateConsole("ERROR: " + response.message.toUpperCase(), SkinLoader.NEON_RED);
                }
            });
        });
    }

    private void tryAutoLogin() {
        String[] tokens = TokenStoreManager.load();
        if (tokens == null) return;

        updateConsole("RESTORING NEURAL LINK...", SkinLoader.TEXT_SECONDARY);
        HttpAuthClientManager httpClient = new HttpAuthClientManager("futurecity.duckdns.org", 443, true);

        httpClient.refresh(tokens[1], new HttpAuthClientManager.AuthCallback() {
            @Override
            public void onSuccess(int userId, String access, String refresh, String nick, String avatar, boolean needs) {
                createAuthCallback().onSuccess(userId, access, refresh, nick, avatar, needs);
            }

            @Override
            public void onError(String message) {
                Gdx.app.postRunnable(() -> {
                    updateConsole("LINK EXPIRED. MANUAL LOGIN REQUIRED.", SkinLoader.NEON_GOLD);
                });
            }
        });
    }

    private void startSocialLogin(String provider) {
        currentPollingSessionId = UUID.randomUUID().toString();
        String url = "https://futurecity.duckdns.org/api/auth/social-login?provider=" + provider + "&sessionId=" + currentPollingSessionId;
        Gdx.net.openURI(url);

        updateConsole("WAITING FOR EXTERNAL PROTOCOL [" + provider.toUpperCase() + "]...", SkinLoader.NEON_CYAN);

        if (pollingTask != null) pollingTask.cancel();
        pollingTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                HttpAuthClientManager httpClient = new HttpAuthClientManager("futurecity.duckdns.org", 443, true);
                httpClient.pollStatus(currentPollingSessionId, createAuthCallback());
            }
        }, 2, 2, 30);
    }

    private void handleAuth(boolean registering) {
        String user = userField.getText();
        String pass = passField.getText();
        String nick = isRegisterMode ? (nicknameField != null ? nicknameField.getText() : "") : "";

        if (user.isEmpty() || pass.isEmpty() || (registering && nick.isEmpty())) {
            updateConsole("HARDWARE ERROR: DATA FIELDS EMPTY", SkinLoader.NEON_RED);
            return;
        }

        HttpAuthClientManager httpClient = new HttpAuthClientManager("futurecity.duckdns.org", 443, true);
        if (registering) {
            updateConsole("TRANSMITTING NEW IDENTITY DATA...", SkinLoader.NEON_CYAN);
            httpClient.register(user, pass, nick, new HttpAuthClientManager.AuthCallback() {
                @Override
                public void onSuccess(int u, String a, String r, String n, String av, boolean nrd) {
                    Gdx.app.postRunnable(() -> {
                        isRegisterMode = false;
                        buildCard();
                        updateConsole("ID REGISTERED. ESTABLISHING LINK...", SkinLoader.NEON_LIME);
                    });
                }

                @Override
                public void onError(String msg) {
                    Gdx.app.postRunnable(() -> {
                        updateConsole("REGISTRATION FAILED: " + msg.toUpperCase(), SkinLoader.NEON_RED);
                    });
                }
            });
        } else {
            updateConsole("SYNCING NEURAL SIGNATURE...", SkinLoader.NEON_CYAN);
            httpClient.login(user, pass, createAuthCallback());
        }
    }

    private HttpAuthClientManager.AuthCallback createAuthCallback() {
        return new HttpAuthClientManager.AuthCallback() {
            @Override
            public void onSuccess(int userId, String access, String refresh, String nick, String avatar, boolean needs) {
                Gdx.app.postRunnable(() -> {
                    if (pollingTask != null) pollingTask.cancel();
                    TokenStoreManager.save(access, refresh);
                    try {
                        networkManager.connect("futurecity.duckdns.org");
                        networkManager.loginWithToken(access);
                        game.setScreen(new GameScreen(game, networkManager, needs));
                        updateConsole("LINK ESTABLISHED. ENTERING WORLD...", SkinLoader.NEON_LIME);
                    } catch (Exception e) {
                        updateConsole("GATEWAY TIMEOUT. SERVER UNREACHABLE.", SkinLoader.NEON_RED);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Gdx.app.postRunnable(() -> {
                    updateConsole("INVALID SIGNATURE. ACCESS DENIED.", SkinLoader.NEON_RED);
                });
            }
        };
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update and draw floating particles behind the UI
        updateAndDrawParticles(delta);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
