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

    // --- UI ---
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    private float debugTimer = 0f;

    // --- IBL & Skybox ---
    private Cubemap diffuseCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;

    public GameScreen(Main game) {
        this(game, new NetworkManager());
    }

    public GameScreen(Main game, NetworkManager networkManager) {
        this.game = game;
        this.networkManager = networkManager;

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

        sceneManager.environment.add(sun);
    }

    private void setupMap() {
        mapManager = new MapManager(game.myAssetManager.get("models/currentcity.glb", SceneAsset.class));
        mapManager.prepareMap();
        mapManager.setMapScale(GameConstants.MAP_SCALE);

        System.out.println("DEBUG: Obstacles count: " + mapManager.getObstacles().size());

        sceneManager.addScene(mapManager.getCityScene());
        collisionSystem = new CollisionSystem();
    }

    private void setupPlayerAndSystems() {
        playerState = new PlayerState();
        playerState.position.set(560f, 50f, 0f);

        inputController = new KeyboardInputController();

        networkManager.setSpawnCallback((id, initialState) -> {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                System.out.println("SPAWNING REMOTE PLAYER VID=" + id);
                SceneAsset asset = game.myAssetManager.get("models/mainCharactor.glb", SceneAsset.class);
                Scene scene = new Scene(asset.scene);
                RemotePlayer remote = new RemotePlayer(scene.modelInstance, initialState);
                sceneManager.addScene(scene);
                networkManager.getRemotePlayers().put(id, remote);
            });
        });

        networkManager.setOnKickCallback(() -> {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                game.setScreen(new com.futurecity.game.ui.LoginScreen(game));
            });
        });

        SceneAsset playerAsset = game.myAssetManager.get("models/mainCharactor.glb", SceneAsset.class);
        Scene playerScene = new Scene(playerAsset.scene);
        sceneManager.addScene(playerScene);

        player = new MainCharactor(playerScene.modelInstance, inputController, playerState);
        playerController = new PlayerInteractionController(player, inputController, networkManager);

        renderSystem = new RenderSystem();
        networkSyncSystem = new ClientNetworkSyncSystem();
        uiManager = new UIManager(game, spriteBatch, playerController, player, mapManager,
                sceneManager, cameraManager, networkManager);

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
        networkManager.setMapManager(mapManager);
    }

    @Override
    public void render(float delta) {
        updateGameLogic(delta);
        renderFrame();
        logDebugInfo(delta);
    }

    private void updateGameLogic(float delta) {
        networkSyncSystem.update(delta, networkManager);
        networkManager.update(delta, cameraManager.getYaw());

        boolean isFPS = (cameraManager.getCurrentView() == CameraViewManager.FIRST_PERSON);
        player.update(delta, cameraManager.getYaw(), isFPS, collisionSystem, mapManager.getObstacles());

        List<com.futurecity.game.entities.Interactable> allInteractables = new java.util.ArrayList<>(
                mapManager.getInteractables());
        allInteractables.addAll(networkManager.getRemoteInteractables());
        playerController.update(delta, cameraManager.camera, isFPS, allInteractables);

        cameraManager.update(delta, player);
        sceneManager.update(delta);
    }

    private void renderFrame() {
        renderSystem.render(sceneManager, collisionSystem, cameraManager.camera);
        float delta = Gdx.graphics.getDeltaTime();
        uiManager.update(delta);
    }

    private void logDebugInfo(float delta) {
        debugTimer += delta;
        if (debugTimer > 2.0f) {
            debugTimer = 0f;
            printCollisionDebug();
        }
    }

    private void printCollisionDebug() {
        com.badlogic.gdx.math.collision.BoundingBox playerBox = player.getWorldBounds();
        com.badlogic.gdx.math.Vector3 playerMin = new com.badlogic.gdx.math.Vector3();
        com.badlogic.gdx.math.Vector3 playerMax = new com.badlogic.gdx.math.Vector3();
        playerBox.getMin(playerMin);
        playerBox.getMax(playerMax);

        System.out.println("\n--- DEBUG COLLISION ---");
        System.out.println("Player Pos: " + player.getPosition());
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
    }
}
