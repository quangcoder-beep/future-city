package com.futurecity.game.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.futurecity.shared.config.GameConstants;
import java.util.List;

import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.entities.RemotePlayer;
import com.futurecity.game.inputs.KeyboardInputController;
import com.futurecity.game.managers.CameraManager;
import com.futurecity.game.managers.CameraViewManager;
import com.futurecity.game.managers.LightManager;
import com.futurecity.game.managers.MapManager;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.UIManager;
import com.futurecity.game.systems.CollisionSystem;
import com.futurecity.game.systems.RenderSystem;
import com.futurecity.game.systems.ClientNetworkSyncSystem;
import com.futurecity.game.systems.PlayerInteractionController;
import com.futurecity.shared.entities.PlayerState;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Main Gameplay Screen.
 * Manages 3D environment, players, camera, and lighting.
 */
public class GameScreen extends ScreenAdapter {
    private Main game;
    private SceneManager sceneManager;

    // --- Components ---
    private MainCharactor player;
    private PlayerState playerState;
    private PlayerInteractionController playerController;
    private KeyboardInputController inputController;

    private LightManager lightManager;
    private CameraManager cameraManager;
    private MapManager mapManager;
    private CollisionSystem collisionSystem;
    private RenderSystem renderSystem;
    private ClientNetworkSyncSystem networkSyncSystem;
    private com.futurecity.game.managers.UIManager uiManager;
    private NetworkManager networkManager;
    private com.futurecity.game.ui.NameplateManager nameplateManager;
    private boolean needsNickname;

    // --- UI ---
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    private float footstepTimer = 0;
    private static final float FOOTSTEP_INTERVAL = 0.45f;
    private boolean lastFootstepWas0 = false;
    private final List<com.futurecity.game.entities.Interactable> cachedInteractables = new java.util.ArrayList<>();

    // --- IBL & Skybox ---
    private Cubemap diffuseCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;

    public GameScreen(Main game) {
        this(game, new NetworkManager(game), false);
    }

    public GameScreen(Main game, NetworkManager networkManager) {
        this(game, networkManager, false);
    }

    public GameScreen(Main game, NetworkManager networkManager, boolean needsNickname) {
        this.game = game;
        this.networkManager = networkManager;
        this.needsNickname = needsNickname;

        setupUI();
        setup3DScene();
        setupLighting();
        setupMap();
        setupPlayerAndSystems();
    }

    private void setupUI() {
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f);
    }

    private void setup3DScene() {
        sceneManager = createSceneManager();
        cameraManager = new CameraManager(
                new PerspectiveCamera(GameConstants.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        sceneManager.setCamera(cameraManager.camera);
    }

    private void setupLighting() {
        lightManager = new LightManager(new DirectionalLightEx(), sceneManager);

        // --- IBL Implementation ---
        DirectionalLightEx sun = lightManager.getSun();
        sun.intensity = 1.0f;
        sun.color.set(new Color(1f, 1f, 0.95f, 1f));
        lightManager.changeDir(-0.5f, -1.0f, -0.3f);

        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(sun);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));
        iblBuilder.dispose();

        // PBR Environment setup
        sceneManager.setAmbientLight(0.5f);
        sceneManager.environment.set(new com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute(
                com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute(
                net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute.DiffuseEnv, diffuseCubemap));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute(
                net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute.SpecularEnv, specularCubemap));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute(
                net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute.BRDFLUTTexture, brdfLUT));

        // Add Skybox
        skybox = new SceneSkybox(specularCubemap);
        sceneManager.setSkyBox(skybox);
        lightManager.setSkybox(skybox);

        sceneManager.environment.add(sun);
    }

    private void setupMap() {
        mapManager = new MapManager(game.myAssetManager.get("models/city_map.glb", SceneAsset.class));
        mapManager.setMapScale(GameConstants.MAP_SCALE);
        mapManager.prepareMap();

        System.out.println("DEBUG: Obstacles count: " + mapManager.getObstacles().size());

        sceneManager.addScene(mapManager.getCityScene());
        collisionSystem = new CollisionSystem();
    }

    private void setupPlayerAndSystems() {
        playerState = new PlayerState();
        playerState.username = game.nickname;
        playerState.position.set(560f, 50f, 0f);

        inputController = new KeyboardInputController();

        networkManager.setSpawnCallback((id, initialState) -> {
            if (id == networkManager.getServerSessionId()) {
                networkManager.clearPendingSpawn(id);
                return;
            }

            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                try {
                    // Pre-spawn cleanup: If a player with this ID is already in the map, 
                    // remove its old scene first (occurs on quick re-join)
                    RemotePlayer old = networkManager.getRemotePlayers().get(id);
                    if (old != null && old.getScene() != null) {
                        sceneManager.removeScene(old.getScene());
                    }

                    System.out.println("SPAWNING REMOTE PLAYER VID=" + id);
                    SceneAsset asset = game.myAssetManager.get("models/player_char.glb", SceneAsset.class);
                    Scene scene = new Scene(asset.scene);
                    RemotePlayer remote = new RemotePlayer(scene.modelInstance, initialState);
                    remote.setScene(scene); // Store scene for removal
                    
                    sceneManager.addScene(scene);
                    networkManager.getRemotePlayers().put(id, remote);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    networkManager.clearPendingSpawn(id);
                }
            });
        });

        networkManager.setPlayerLeftCallback((id, player) -> {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                System.out.println("REMOVING REMOTE PLAYER VID=" + id);
                if (player.getScene() != null) {
                    sceneManager.removeScene(player.getScene());
                }
            });
        });

        networkManager.setOnKickCallback(() -> {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                game.setScreen(new com.futurecity.game.ui.LoginScreen(game));
            });
        });

        SceneAsset playerAsset = game.myAssetManager.get("models/player_char.glb", SceneAsset.class);
        Scene playerScene = new Scene(playerAsset.scene);
        sceneManager.addScene(playerScene);

        player = new MainCharactor(playerScene.modelInstance, inputController, playerState);
        playerController = new PlayerInteractionController(player, inputController, networkManager);

        renderSystem = new RenderSystem();
        networkSyncSystem = new ClientNetworkSyncSystem();
        uiManager = new UIManager(game, spriteBatch, playerController, player, mapManager,
                sceneManager, cameraManager, networkManager, needsNickname);

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        if (uiManager != null && uiManager.getStage() != null) {
            multiplexer.addProcessor(uiManager.getStage());
        }
        multiplexer.addProcessor(inputController);
        if (cameraManager != null) {
            multiplexer.addProcessor(cameraManager);
        }
        Gdx.input.setInputProcessor(multiplexer);

        networkManager.attachPlayer(player);
        networkManager.setWorldTimeListener(time -> {
            if (lightManager != null) {
                lightManager.updateTime(time);
            }
        });
        networkManager.setMapManager(mapManager);

        // Setup UI Focus Checker to block game inputs during typing
        java.util.function.Supplier<Boolean> focusChecker = () -> {
            if (uiManager == null || uiManager.getStage() == null) return false;
            com.badlogic.gdx.scenes.scene2d.Actor focus = uiManager.getStage().getKeyboardFocus();
            return focus instanceof com.badlogic.gdx.scenes.scene2d.ui.TextField;
        };
        inputController.setUiFocusChecker(focusChecker);
        playerController.setUiFocusChecker(focusChecker);
        cameraManager.setUiFocusChecker(focusChecker);

        nameplateManager = new com.futurecity.game.ui.NameplateManager();
        networkManager.setNameplateManager(nameplateManager);
    }

    @Override
    public void render(float delta) {
        try {
            updateGameLogic(delta);
            renderFrame();
        } catch (Exception e) {
            System.err.println("[CRITICAL] GameScreen crash averted. Error: " + e.getMessage());
            e.printStackTrace();
            // Optionally, fallback to login screen to avert continuous loop errors
            if (game != null) {
                game.setScreen(new com.futurecity.game.ui.LoginScreen(game));
            }
        }
    }

    private void updateGameLogic(float delta) {
        if (lightManager != null) {
            lightManager.update(delta);
        }

        // Footstep Audio Logic
        if (player.isMoving() && player.isOnGround()) {
            footstepTimer += delta;
            if (footstepTimer >= FOOTSTEP_INTERVAL) {
                footstepTimer = 0;
                String soundPath = lastFootstepWas0 ? "sounds/gameplay/footstep01.ogg"
                        : "sounds/gameplay/footstep00.ogg";
                game.audioManager.playSfx(soundPath, 0.4f, 0.02f, false);
                lastFootstepWas0 = !lastFootstepWas0;
            }
        } else {
            footstepTimer = FOOTSTEP_INTERVAL; // Ready to play immediately when starting to move
        }

        networkSyncSystem.update(delta, networkManager);
        networkManager.update(delta, cameraManager.getYaw());

        boolean isFPS = (cameraManager.getCurrentView() == CameraViewManager.FIRST_PERSON);
        player.update(delta, cameraManager.getYaw(), isFPS, collisionSystem, mapManager.getObstacles());

        cachedInteractables.clear();
        cachedInteractables.addAll(mapManager.getInteractables());
        cachedInteractables.addAll(networkManager.getRemoteInteractables());
        playerController.update(delta, cameraManager.camera, isFPS, cachedInteractables);

        cameraManager.update(delta, player);
        sceneManager.update(delta);

        if (nameplateManager != null) {
            nameplateManager.update(delta, cameraManager.camera, networkManager.getRemotePlayers(), player, isFPS);
        }
    }

    private void renderFrame() {
        renderSystem.render(sceneManager, collisionSystem, cameraManager.camera);

        if (nameplateManager != null) {
            nameplateManager.draw();
        }

        float delta = Gdx.graphics.getDeltaTime();
        uiManager.update(delta);
    }


    private SceneManager createSceneManager() {
        PBRShaderConfig pbrConfig = PBRShaderProvider.createDefaultConfig();
        pbrConfig.numBones = 120; // Increase for characters
        pbrConfig.numDirectionalLights = 1;

        com.badlogic.gdx.graphics.g3d.shaders.DepthShader.Config depthConfig = new com.badlogic.gdx.graphics.g3d.shaders.DepthShader.Config();
        depthConfig.numBones = 120;

        PBRShaderProvider colorProvider = new PBRShaderProvider(pbrConfig);
        PBRDepthShaderProvider depthProvider = new PBRDepthShaderProvider(depthConfig);

        return new SceneManager(colorProvider, depthProvider);
    }

    @Override
    public void resize(int width, int height) {
        if (uiManager != null) {
            uiManager.resize(width, height);
        }
        if (nameplateManager != null) {
            nameplateManager.resize(width, height);
        }
        cameraManager.camera.viewportWidth = width;
        cameraManager.camera.viewportHeight = height;
        cameraManager.camera.update();
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        if (diffuseCubemap != null)
            diffuseCubemap.dispose();
        if (specularCubemap != null)
            specularCubemap.dispose();
        if (brdfLUT != null)
            brdfLUT.dispose();
        if (skybox != null)
            skybox.dispose();

        spriteBatch.dispose();
        font.dispose();
        if (uiManager != null)
            uiManager.dispose();
        if (nameplateManager != null)
            nameplateManager.dispose();
    }
}
