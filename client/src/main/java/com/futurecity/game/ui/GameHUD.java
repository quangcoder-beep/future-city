package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.futurecity.game.core.Main;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.managers.CameraManager;
import com.futurecity.game.managers.MapManager;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.TokenStoreManager;
import com.futurecity.game.ui.minimap.MinimapWidget;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import com.futurecity.shared.packets.resonse.DeliveryGPSUpdate;
import com.kotcrab.vis.ui.widget.VisImageButton;

import net.mgsx.gltf.scene3d.scene.SceneManager;

/**
 * Premium GameHUD (Heads-Up Display)
 * Features: Status bar, tooltips, notification badges, context hints.
 */
public class GameHUD {
    private Stage stage;
    private Skin skin;
    private VisLabel interactionLabel;
    private VisTable rootTable;
    private MinimapWidget minimapWidget;
    private SceneManager sceneManager;
    private InteractionBubble interactionBubble;

    // Panel management
    private java.util.List<BasePanel> activePanels = new java.util.ArrayList<>();
    private InteractionPanel interactionPanel;
    private SystemMenuPanel systemMenuPanel;
    private NicknameSelectionPanel nicknameSelectionPanel;
    private SettingsPanel settingsPanel;
    private TutorialPanel tutorialPanel;
    private NotificationPanel notificationPanel;
    private com.futurecity.game.managers.NotificationManager notificationManager;
    private NetworkManager networkManager;
    private MainCharactor player;
    private java.util.List<InventoryResponse.OrderInfo> clientActiveDeliveries = new java.util.ArrayList<>();
    private Main game;
    private Texture menuIconTexture;
    private Texture inventoryIconTexture;
    private Texture bellIconTexture;
    private ShaderProgram crtShader;
    private float shaderTime = 0;
    private float minimapTimer = 0;
    private static final float MINIMAP_UPDATE_INTERVAL = 1f / 15f;

    // Premium HUD elements
    private VisLabel coinLabel;
    private VisLabel nicknameLabel;
    private VisTable contextBar;
    private float contextBarTimer = 10f; // Auto-hide after 10 seconds
    private HudTooltip tooltip;
    private VisLabel badgeLabel;
    private Image badgeBg;

    public GameHUD(Main game, SpriteBatch spriteBatch, MainCharactor player,
            MapManager mapManager, SceneManager sceneManager,
            CameraManager cameraManager, NetworkManager networkManager, boolean needsNickname) {
        this.game = game;
        this.player = player;
        this.sceneManager = sceneManager;
        this.networkManager = networkManager;
        this.notificationManager = new com.futurecity.game.managers.NotificationManager();
        stage = new Stage(new ExtendViewport(800, 600), spriteBatch);

        skin = com.kotcrab.vis.ui.VisUI.getSkin();

        setupUI();

        // Minimap
        minimapWidget = new MinimapWidget(player, mapManager, skin, cameraManager, networkManager);
        minimapWidget.setUiFocusChecker(() -> stage.getKeyboardFocus() instanceof com.badlogic.gdx.scenes.scene2d.ui.TextField);
        stage.addActor(minimapWidget);

        // InteractionPanel (Dashboard with merged PhonePanel)
        interactionPanel = new InteractionPanel(skin, networkManager, game, player, notificationManager);
        interactionPanel.setOnGpsRequested(targetPos -> this.setNavigationTarget(targetPos, true));
        interactionPanel.setOnStopGpsRequested(() -> {
            if (minimapWidget != null)
                minimapWidget.stopNavigation();
        });
        registerPanel(interactionPanel);

        // System Menu
        systemMenuPanel = new SystemMenuPanel(skin, game, networkManager, this);
        registerPanel(systemMenuPanel);

        // Nickname Panel
        nicknameSelectionPanel = new NicknameSelectionPanel(skin, game, TokenStoreManager.load()[0], game.avatarUrl, needsNickname);
        registerPanel(nicknameSelectionPanel);

        // Settings
        settingsPanel = new SettingsPanel(skin);
        registerPanel(settingsPanel);

        // Tutorial
        tutorialPanel = new TutorialPanel(skin);
        registerPanel(tutorialPanel);

        // Notifications
        notificationPanel = new NotificationPanel(skin, game, notificationManager);
        registerPanel(notificationPanel);

        // CRT Shader
        crtShader = new ShaderProgram(
                Gdx.files.internal("shaders/ui_glitch.vert"),
                Gdx.files.internal("shaders/ui_glitch.frag"));
        if (!crtShader.isCompiled()) {
            Gdx.app.error("CRT", "Shader failed to compile: " + crtShader.getLog());
        }

        closeAllPanels();

        if (needsNickname) {
            showPanel(nicknameSelectionPanel);
        }
    }

    public void updateMinimap(float delta) {
        if (minimapWidget != null) {
            minimapTimer += delta;
            if (minimapTimer >= MINIMAP_UPDATE_INTERVAL) {
                minimapTimer = 0;
                minimapWidget.updateSceneRender(sceneManager);
            }
        }
    }

    private void setupUI() {
        rootTable = new VisTable();
        rootTable.setFillParent(true);
        rootTable.center();
        stage.addActor(rootTable);

        // Status bar removed — info available in inventory panel
        coinLabel = new VisLabel("0", "small"); // Keep reference for updateCoinDisplay()

        // ═══ HUD BUTTONS (Top Right) ═══
        inventoryIconTexture = new Texture(Gdx.files.internal("images/ui/icons/inventory.png"));
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable inventoryIcon = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                inventoryIconTexture);
        VisImageButton inventoryBtn = new VisImageButton(inventoryIcon);

        menuIconTexture = new Texture(Gdx.files.internal("images/ui/icons/logout.png"));
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable logoutIcon = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                menuIconTexture);
        VisImageButton menuBtn = new VisImageButton(logoutIcon);

        bellIconTexture = new Texture(Gdx.files.internal("images/ui/icons/bell.png"));
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable bellIcon = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                bellIconTexture);
        VisImageButton bellBtn = new VisImageButton(bellIcon);

        // Sounds
        if (game.audioManager != null) {
            game.audioManager.attachDefaultTo(inventoryBtn);
            game.audioManager.attachDefaultTo(menuBtn);
            game.audioManager.attachDefaultTo(bellBtn);
        }

        // ═══ NOTIFICATION BADGE (circle + number) ═══
        badgeLabel = new VisLabel("", "small");
        badgeLabel.setColor(Color.WHITE);
        badgeLabel.setFontScale(0.7f);
        badgeLabel.setVisible(false);

        badgeBg = null;
        try {
            if (skin.has("badge-circle", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                badgeBg = new Image(skin.getDrawable("badge-circle"));
                badgeBg.setVisible(false);
            }
        } catch (Exception ignored) {}

        notificationManager.addListener(new com.futurecity.game.managers.NotificationManager.NotificationListener() {
            @Override
            public void onNewNotification(com.futurecity.game.entities.NotificationEntry entry) {
                updateBadge();
                // Pulse animation on new notification
                if (badgeBg != null && badgeBg.isVisible()) {
                    badgeBg.clearActions();
                    badgeBg.addAction(Actions.sequence(
                            Actions.scaleTo(1.3f, 1.3f, 0.1f),
                            Actions.scaleTo(1f, 1f, 0.15f)));
                }
            }

            @Override
            public void onNotificationsRead() {
                updateBadge();
            }
        });

        // Button row
        VisTable topButtons = new VisTable();
        topButtons.setFillParent(true);
        topButtons.top().right().pad(15);

        // Bell button with badge
        Stack bellStack = new Stack();
        bellStack.add(bellBtn);
        VisTable badgeOverlay = new VisTable();
        badgeOverlay.top().right();
        if (badgeBg != null) {
            Stack badgeStack = new Stack();
            badgeStack.add(badgeBg);
            VisTable badgeLabelContainer = new VisTable();
            badgeLabelContainer.add(badgeLabel).center();
            badgeStack.add(badgeLabelContainer);
            badgeOverlay.add(badgeStack).size(18, 18);
        } else {
            badgeOverlay.add(badgeLabel).padRight(2).padTop(2);
        }
        bellStack.add(badgeOverlay);

        topButtons.add(bellStack).size(50, 50).padRight(15);
        topButtons.add(inventoryBtn).size(50, 50).padRight(15);
        topButtons.add(menuBtn).size(50, 50);
        stage.addActor(topButtons);

        // ═══ TOOLTIPS ═══
        tooltip = new HudTooltip(skin);
        stage.addActor(tooltip);
        tooltip.attachTo(bellBtn, "NOTIFICATIONS");
        tooltip.attachTo(inventoryBtn, "INVENTORY");
        tooltip.attachTo(menuBtn, "SYSTEM MENU");

        // ═══ CONTEXT BAR (Bottom Center) — auto-hides ═══
        contextBar = new VisTable();
        contextBar.setFillParent(true);
        contextBar.bottom().center().padBottom(20);

        VisTable contextCard = new VisTable();
        if (skin.has("hud-bar-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            contextCard.setBackground(skin.getDrawable("hud-bar-bg"));
        }
        contextCard.pad(6, 20, 6, 20);

        VisLabel hint = new VisLabel("F: Interact  |  H: Inventory  |  P: Phone  |  ESC: Menu  |  R: Map", "small");
        hint.setColor(SkinLoader.TEXT_DIM);
        contextCard.add(hint);
        contextBar.add(contextCard);
        stage.addActor(contextBar);

        // ═══ BUTTON LISTENERS ═══
        inventoryBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                toggleInventory();
            }
        });

        menuBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                toggleSystemMenu();
            }
        });

        bellBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                toggleNotifications();
            }
        });

        interactionBubble = new InteractionBubble(skin);
        stage.addActor(interactionBubble);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle hudStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(
                skin.get(com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class));
        try {
            hudStyle.font = skin.getFont("hud-font");
        } catch (Exception e) {
        }

        interactionLabel = new VisLabel("", hudStyle);
        interactionLabel.setColor(Color.YELLOW);
        rootTable.add(interactionLabel).padBottom(100);
    }

    private void updateBadge() {
        int unread = notificationManager.getUnreadCount();
        if (unread > 0) {
            badgeLabel.setText(unread > 9 ? "9+" : String.valueOf(unread));
            badgeLabel.setVisible(true);
            if (badgeBg != null) badgeBg.setVisible(true);
        } else {
            badgeLabel.setVisible(false);
            if (badgeBg != null) badgeBg.setVisible(false);
        }
    }

    /**
     * Updates the coin display on HUD (called when coins change).
     */
    public void updateCoinDisplay(int coins) {
        if (coinLabel != null) {
            coinLabel.setText(String.valueOf(coins));
        }
    }

    public VisLabel getInteractionLabel() {
        return interactionLabel;
    }

    public void setInteractionMessage(String message, com.badlogic.gdx.math.Vector3 targetPos,
            com.badlogic.gdx.graphics.Camera camera) {
        if (message == null || message.isEmpty()) {
            interactionLabel.setVisible(false);
            if (interactionBubble != null)
                interactionBubble.setVisible(false);
        } else {
            interactionLabel.setText(message);
            interactionLabel.setVisible(true);

            if (interactionBubble != null && targetPos != null && camera != null) {
                interactionBubble.update(targetPos, message.replace("[F] ", ""), camera);
                if (interactionBubble.isVisible()) {
                    interactionLabel.setVisible(false);
                }
            }
        }
    }

    // --- Panel Management ---

    public void registerPanel(BasePanel panel) {
        activePanels.add(panel);
        stage.addActor(panel);

        panel.setOnClose(() -> {
            if (player != null && player.getInput() != null) {
                player.getInput().clearInput();
            }
            if (!isAnyPanelOpen()) {
                Gdx.input.setCursorCatched(true);
            }
        });
    }

    public void showPanel(BasePanel panel) {
        for (BasePanel p : activePanels) {
            if (p.isPanelVisible()) {
                p.hidePanel();
            }
        }
        stage.unfocusAll();
        Gdx.input.setCursorCatched(false);
        panel.showPanel();
    }

    public void closeAllPanels() {
        if (player != null && player.getInput() != null) {
            player.getInput().clearInput();
        }

        for (BasePanel panel : activePanels) {
            if (panel.isPanelVisible()) {
                panel.hidePanel();
            }
        }
        stage.unfocusAll();
        Gdx.input.setCursorCatched(true);
    }

    public boolean isAnyPanelOpen() {
        for (BasePanel panel : activePanels) {
            if (panel.isPanelVisible()) {
                return true;
            }
        }
        return false;
    }

    public void toggleInventory() {
        if (interactionPanel.isPanelVisible()
                && interactionPanel.getCurrentTab() == InteractionPanel.DashboardTab.INVENTORY) {
            closeAllPanels();
        } else {
            interactionPanel.switchToTab(InteractionPanel.DashboardTab.INVENTORY);
        }
    }

    public void togglePhone() {
        if (interactionPanel.isPanelVisible()
                && interactionPanel.getCurrentTab() == InteractionPanel.DashboardTab.PHONE) {
            closeAllPanels();
        } else {
            interactionPanel.switchToTab(InteractionPanel.DashboardTab.PHONE);
        }
    }

    public void toggleSystemMenu() {
        if (systemMenuPanel.isPanelVisible()) {
            closeAllPanels();
        } else {
            showPanel(systemMenuPanel);
        }
    }

    public NicknameSelectionPanel getNicknameSelectionPanel() {
        return nicknameSelectionPanel;
    }

    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }

    public InteractionPanel getDashboard() {
        return interactionPanel;
    }

    // --- Response routing methods ---

    public void showShopFromResponse(InteractionResponse response) {
        if (response.uiType == com.futurecity.shared.enums.InteractionUIType.PICKUP_ACTION) {
            interactionPanel.updateAndShow(response);
            return;
        }
        interactionPanel.updateAndShow(response);
    }

    public void showDialogueFromResponse(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showHouseFromResponse(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showPlayerFromResponse(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showPickupPanel(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showHandoverPanel(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showDeliveryConfirmDialog(String courierName, String itemName,
            final java.util.function.Consumer<Boolean> callback) {
        Gdx.input.setCursorCatched(false);
        com.kotcrab.vis.ui.widget.VisDialog dialog = new com.kotcrab.vis.ui.widget.VisDialog(
                "--- DELIVERY NOTIFICATION ---") {
            @Override
            protected void result(Object object) {
                callback.accept((Boolean) object);
            }
        };

        com.badlogic.gdx.scenes.scene2d.ui.Table content = dialog.getContentTable();
        content.add(new VisLabel("Courier: ")).left();
        VisLabel nameLbl = new VisLabel(courierName.toUpperCase());
        nameLbl.setColor(SkinLoader.NEON_CYAN);
        content.add(nameLbl).left().row();

        content.add(new VisLabel("Item: ")).left();
        VisLabel itemLbl = new VisLabel(itemName);
        itemLbl.setColor(SkinLoader.NEON_GOLD);
        content.add(itemLbl).left().row();

        content.add(new VisLabel("\nDo you accept payment and receive the item?")).colspan(2).center().padTop(10);

        dialog.button("REJECT (NO)", false);
        dialog.button("ACCEPT (YES)", true);
        dialog.padBottom(20);
        dialog.centerWindow();
        dialog.show(stage);
    }

    public void showHandoverStatus(String msg) {
        showNotification("💬 " + msg);
    }

    public void showSuccessFX() {
        VisLabel fxLabel = new VisLabel("DELIVERY SUCCESS!",
                skin.get(com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class));
        fxLabel.setColor(SkinLoader.NEON_LIME);
        fxLabel.setFontScale(2.0f);
        fxLabel.setPosition(stage.getWidth() / 2f - fxLabel.getPrefWidth() / 2f, stage.getHeight() / 2f);
        stage.addActor(fxLabel);

        fxLabel.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(0, 100, 1.5f),
                        Actions.fadeOut(1.5f),
                        Actions.scaleTo(1.2f, 1.2f, 1.5f)),
                Actions.removeActor()));
    }

    public void onBuySuccess(int remainingCoins, int itemId) {
        System.err.println("[DEBUG-HUD] onBuySuccess coins=" + remainingCoins);
        if (game != null) game.credits = remainingCoins;
        updateCoinDisplay(remainingCoins);
        if (interactionPanel != null)
            interactionPanel.onBuySuccess(remainingCoins, itemId);
    }

    public void onBuyFailed(String message) {
        showNotification("❌ " + message);
    }

    public void updateInventory(InventoryResponse response) {
        if (interactionPanel != null)
            interactionPanel.updateInventory(response);
        if (response.activeDeliveries != null) {
            this.clientActiveDeliveries = response.activeDeliveries;
        }
        // Update HUD coin display
        updateCoinDisplay(response.playerCoins);
    }

    public java.util.List<InventoryResponse.OrderInfo> getActiveDeliveries() {
        return clientActiveDeliveries;
    }

    public void showNotification(String message) {
        showNotification(message, null);
    }

    public void showNotification(String message, String tag) {
        if (notificationManager != null) {
            notificationManager.addNotification(message, com.futurecity.game.entities.NotificationEntry.NotificationType.INFO, tag);
        }
    }

    public void removeNotification(String tag) {
        if (notificationManager != null) {
            notificationManager.removeByTag(tag);
        }
    }

    public void toggleNotifications() {
        if (notificationPanel == null) return;
        if (notificationPanel.isPanelVisible()) {
            notificationPanel.hidePanel();
            Gdx.input.setCursorCatched(true);
        } else {
            showPanel(notificationPanel);
        }
    }

    public void setNavigationTarget(Vector3 targetWorldPos, boolean manual) {
        if (minimapWidget != null) {
            minimapWidget.setNavigationTarget(targetWorldPos, manual);
        }
    }

    public void setNavigationTarget(Vector3 targetWorldPos) {
        setNavigationTarget(targetWorldPos, false);
    }

    public void updateDeliveryGPS(DeliveryGPSUpdate res) {
        if (minimapWidget != null) {
            minimapWidget.onDeliveryGPSUpdate(res);
        }
    }

    public void updateShopSubscribePanel(NewOrderNotification notif) {
        if (interactionPanel != null && notif != null) {
            interactionPanel.addNewOrder(notif);
        }
    }

    public void setDeliveryTargetName(String name) {
        if (name != null) {
            showNotification("📍 Navigation to: " + name);
        }
    }

    public Skin getSkin() {
        return skin;
    }

    public void render(float delta) {
        // Auto-hide context bar after initial period
        if (contextBar != null && contextBar.isVisible()) {
            contextBarTimer -= delta;
            if (contextBarTimer <= 0) {
                contextBar.addAction(Actions.sequence(
                        Actions.fadeOut(1f),
                        Actions.visible(false)));
                contextBarTimer = Float.MAX_VALUE; // Don't trigger again
            }
        }

        if (isAnyPanelOpen()) {
            shaderTime += delta;
            stage.getBatch().setShader(crtShader);
            if (crtShader != null && crtShader.isCompiled()) {
                crtShader.setUniformf("u_time", shaderTime);
            }
        } else {
            stage.getBatch().setShader(null);
        }

        stage.act(delta);
        stage.draw();

        stage.getBatch().setShader(null);
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        float logicW = stage.getWidth();
        float logicH = stage.getHeight();
        for (BasePanel panel : activePanels) {
            panel.resize(logicW, logicH);
        }
        if (minimapWidget != null) {
            minimapWidget.updateLayout();
        }
    }

    public void dispose() {
        if (menuIconTexture != null) {
            menuIconTexture.dispose();
        }
        if (inventoryIconTexture != null) {
            inventoryIconTexture.dispose();
        }
        stage.dispose();
        if (skin != null)
            skin.dispose();
        if (crtShader != null)
            crtShader.dispose();
    }

    public void showKickDialog(com.esotericsoftware.kryonet.Client kryoClient, Runnable afterKick) {
        Gdx.input.setCursorCatched(false);
        new KickDialog(stage, game, networkManager, afterKick);
    }

    public Stage getStage() {
        return stage;
    }

    public TutorialPanel getTutorialPanel() {
        return tutorialPanel;
    }
}
