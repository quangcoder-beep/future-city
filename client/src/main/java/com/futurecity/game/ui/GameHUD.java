package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.futurecity.game.core.Main;
import com.futurecity.game.ui.BasePanel;
import com.futurecity.game.ui.SystemMenuPanel;
import com.futurecity.game.ui.InteractionPanel;
import com.futurecity.game.ui.InteractionBubble;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.managers.CameraManager;
import com.futurecity.game.managers.MapManager;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.ui.minimap.MinimapWidget;
import com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import com.futurecity.shared.packets.resonse.DeliveryGPSUpdate;
import com.futurecity.shared.packets.request.ShopListRequest;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisTextButton;

import net.mgsx.gltf.scene3d.scene.SceneManager;

/**
 * GameHUD (Heads-Up Display)
 * Responsible for managing the UI overlays on the screen.
 * Does not contain game logic, only receives data and displays it.
 */
public class GameHUD {
    private Stage stage;
    private Skin skin;
    private VisLabel interactionLabel;
    private VisLabel notificationLabel;
    private VisTable rootTable;
    private MinimapWidget minimapWidget;
    private SceneManager sceneManager;
    private InteractionBubble interactionBubble;

    // Panel management
    private java.util.List<BasePanel> activePanels = new java.util.ArrayList<>();
    private BasePanel currentPanel = null;
    private InteractionPanel interactionPanel;
    private SystemMenuPanel systemMenuPanel;
    private NetworkManager networkManager;
    private MainCharactor player;
    private java.util.List<InventoryResponse.OrderInfo> clientActiveDeliveries = new java.util.ArrayList<>();
    private Main game;
    private Texture menuIconTexture;
    private Texture inventoryIconTexture;

    public GameHUD(Main game, SpriteBatch spriteBatch, MainCharactor player,
            MapManager mapManager, SceneManager sceneManager,
            CameraManager cameraManager, NetworkManager networkManager) {
        this.game = game;
        this.player = player;
        this.sceneManager = sceneManager;
        this.networkManager = networkManager;
        // Use ExtendViewport to ensure UI scales safely on all screens
        stage = new Stage(new ExtendViewport(800, 600), spriteBatch);

        // Default Skin is already loaded in LoadingScreen -> SkinLoader
        skin = com.kotcrab.vis.ui.VisUI.getSkin();

        setupUI();

        // Initialize and add Minimap
        minimapWidget = new MinimapWidget(player, mapManager, skin, cameraManager, networkManager);
        stage.addActor(minimapWidget);

        // Initialize Grand Dashboard
        interactionPanel = new InteractionPanel(skin, networkManager, player);
        interactionPanel.setOnGpsRequested(targetPos -> this.setNavigationTarget(targetPos, true));
        interactionPanel.setOnStopGpsRequested(() -> {
            if (minimapWidget != null)
                minimapWidget.stopNavigation();
        });
        registerPanel(interactionPanel);

        // Initialize System Menu
        systemMenuPanel = new SystemMenuPanel(skin, game, networkManager);
        registerPanel(systemMenuPanel);
    }

    public void updateMinimap(float delta) {
        if (minimapWidget != null) {
            minimapWidget.updateSceneRender(sceneManager);
        }
    }

    private void setupUI() {
        rootTable = new VisTable();
        rootTable.setFillParent(true);
        rootTable.center();
        stage.addActor(rootTable);

        // --- Menu Button (Top Right) ---
        inventoryIconTexture = new Texture(Gdx.files.internal("ui/inventory.png"));
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable inventoryIcon = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                inventoryIconTexture);
        VisImageButton inventoryBtn = new VisImageButton(inventoryIcon);

        menuIconTexture = new Texture(Gdx.files.internal("ui/logout.png"));
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable logoutIcon = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                menuIconTexture);
        VisImageButton menuBtn = new VisImageButton(logoutIcon);

        // We put it in a separate table for positioning
        VisTable topButtons = new VisTable();
        topButtons.setFillParent(true);
        topButtons.top().right().pad(15);
        topButtons.add(inventoryBtn).size(50, 50).padRight(15); // Icon Túi đồ / Dashboard
        topButtons.add(menuBtn).size(50, 50); // Icon Menu/Logout
        stage.addActor(topButtons);

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

        interactionBubble = new InteractionBubble(skin);
        stage.addActor(interactionBubble);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle hudStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(
                skin.get(com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class));
        try {
            hudStyle.font = skin.getFont("hud-font");
        } catch (Exception e) {
        }

        // Interaction notification label (Hidden by default)
        interactionLabel = new VisLabel("", hudStyle);
        interactionLabel.setColor(Color.YELLOW);

        // Add to root (slightly above center)
        rootTable.add(interactionLabel).padBottom(100);
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
                // Hide HUD label if bubble is showing to avoid redundancy
                if (interactionBubble.isVisible()) {
                    interactionLabel.setVisible(false);
                }
            }
        }
    }

    // --- Panel Management ---

    /**
     * Registers a panel with the HUD.
     */
    public void registerPanel(BasePanel panel) {
        activePanels.add(panel);
        stage.addActor(panel);

        // When panel closes, re-catch cursor if no other panels are open
        panel.setOnClose(() -> {
            // Reset input when panel closes to avoid stuck keys
            if (player != null && player.getInput() != null) {
                player.getInput().clearInput();
            }
            if (!isAnyPanelOpen()) {
                Gdx.input.setCursorCatched(true);
            }
        });
    }

    /**
     * Shows panel matching server response. Hides current panel and unlocks cursor.
     */
    public void showPanel(BasePanel panel) {
        for (BasePanel p : activePanels) {
            if (p.isPanelVisible()) {
                p.hidePanel();
            }
        }
        stage.unfocusAll();

        currentPanel = panel;
        panel.showPanel();
        Gdx.input.setCursorCatched(false);
    }

    /**
     * Closes all panels.
     */
    public void closeAllPanels() {
        // Reset input immediately to avoid stuck keys when closing UI
        if (player != null && player.getInput() != null) {
            player.getInput().clearInput();
        }

        for (BasePanel panel : activePanels) {
            if (panel.isPanelVisible()) {
                panel.hidePanel();
            }
        }
        currentPanel = null;
        stage.unfocusAll();

        // Re-catch cursor immediately
        Gdx.input.setCursorCatched(true);
    }

    /**
     * Checks if any panel is currently open.
     */
    public boolean isAnyPanelOpen() {
        for (BasePanel panel : activePanels) {
            if (panel.isPanelVisible()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggle inventory panel (H key).
     */
    public void toggleInventory() {
        if (interactionPanel.isPanelVisible()
                && interactionPanel.getCurrentTab() == InteractionPanel.DashboardTab.INVENTORY) {
            closeAllPanels();
        } else {
            interactionPanel.switchToTab(InteractionPanel.DashboardTab.INVENTORY);
        }
    }

    /**
     * Open/Close phone panel (P key).
     */
    public void togglePhone() {
        if (interactionPanel.isPanelVisible()
                && interactionPanel.getCurrentTab() == InteractionPanel.DashboardTab.PHONE) {
            closeAllPanels();
        } else {
            interactionPanel.switchToTab(InteractionPanel.DashboardTab.PHONE);
        }
    }

    /**
     * Open/Close system menu (ESC key or Menu button).
     */
    public void toggleSystemMenu() {
        if (systemMenuPanel.isPanelVisible()) {
            closeAllPanels();
        } else {
            showPanel(systemMenuPanel);
        }
    }

    public InteractionPanel getDashboard() {
        return interactionPanel;
    }

    // --- Response routing methods ---

    /**
     * Shows ShopPanel from InteractionResponse.
     */
    public void showShopFromResponse(InteractionResponse response) {
        if (response.uiType == com.futurecity.shared.enums.InteractionUIType.PICKUP_ACTION) {
            interactionPanel.updateAndShow(response);
            return;
        }
        interactionPanel.updateAndShow(response);
    }

    /**
     * Shows DialoguePanel from InteractionResponse.
     */
    public void showDialogueFromResponse(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showHouseFromResponse(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    /**
     * Shows PlayerPanel from InteractionResponse.
     */
    public void showPlayerFromResponse(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showPickupPanel(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    public void showHandoverPanel(InteractionResponse response) {
        interactionPanel.updateAndShow(response);
    }

    /**
     * Shows confirmation dialog for incoming delivery (Buyer).
     */
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
        nameLbl.setColor(Color.CYAN);
        content.add(nameLbl).left().row();

        content.add(new VisLabel("Item: ")).left();
        VisLabel itemLbl = new VisLabel(itemName);
        itemLbl.setColor(Color.GOLD);
        content.add(itemLbl).left().row();

        content.add(new VisLabel("\nDo you accept payment and receive the item?")).colspan(2).center().padTop(10);

        dialog.button("REJECT (NO)", false);
        dialog.button("ACCEPT (YES)", true);
        dialog.padBottom(20);
        dialog.centerWindow();
        dialog.show(stage);
    }

    /**
     * Shows handover status for Shipper.
     */
    public void showHandoverStatus(String msg) {
        showNotification("💬 " + msg);
    }

    /**
     * Success effect for delivery.
     */
    public void showSuccessFX() {
        VisLabel fxLabel = new VisLabel("DELIVERY SUCCESS!",
                skin.get(com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class));
        fxLabel.setColor(Color.GREEN);
        fxLabel.setFontScale(2.0f);
        fxLabel.setPosition(Gdx.graphics.getWidth() / 2f - fxLabel.getPrefWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage.addActor(fxLabel);

        fxLabel.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(0, 100, 1.5f),
                        Actions.fadeOut(1.5f),
                        Actions.scaleTo(1.2f, 1.2f, 1.5f)),
                Actions.removeActor()));
    }

    /**
     * Updates ShopPanel and InventoryPanel after coin change.
     */
    public void onBuySuccess(int remainingCoins, int itemId) {
        if (interactionPanel != null)
            interactionPanel.onBuySuccess(remainingCoins, itemId);
    }

    public void onBuyFailed(String message) {
        // Handled via status labels usually, or notification
        showNotification("❌ " + message);
    }

    /**
     * Updates InventoryPanel.
     */
    public void updateInventory(InventoryResponse response) {
        if (interactionPanel != null)
            interactionPanel.updateInventory(response);
        if (response.activeDeliveries != null) {
            this.clientActiveDeliveries = response.activeDeliveries;
        }
    }

    public java.util.List<InventoryResponse.OrderInfo> getActiveDeliveries() {
        return clientActiveDeliveries;
    }

    /**
     * Shows short notification toast.
     */
    public void showNotification(String message) {
        if (notificationLabel == null) {
            com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle titleStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(
                    skin.get(com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class));
            try {
                titleStyle.font = skin.getFont("title-font");
            } catch (Exception e) {
            }
            notificationLabel = new VisLabel("", titleStyle);
            notificationLabel.setColor(Color.GREEN);
            stage.addActor(notificationLabel);
        }
        notificationLabel.setText(message);
        notificationLabel.setPosition(Gdx.graphics.getWidth() / 2f - notificationLabel.getPrefWidth() / 2f,
                Gdx.graphics.getHeight() - 60);
        notificationLabel.setVisible(true);
        notificationLabel.clearActions();
        notificationLabel.getColor().a = 1; // Reset alpha
        notificationLabel.addAction(Actions.sequence(
                Actions.delay(3f),
                Actions.fadeOut(0.5f),
                Actions.run(() -> notificationLabel.setVisible(false))));
    }

    /**
     * Turns on GPS navigation to target.
     */
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
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        for (BasePanel panel : activePanels) {
            panel.resize(width, height);
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
    }

    /**
     * Hiển thị dialog thông báo khi tài khoản bị kick do đăng nhập trùng.
     * Dialog modal — block toàn bộ HUD. Người chơi bấm OK mới disconnect và quay về
     * màn đăng nhập.
     */
    public void showKickDialog(com.esotericsoftware.kryonet.Client kryoClient, Runnable afterKick) {
        Gdx.input.setCursorCatched(false);
        new KickDialog(stage, kryoClient, afterKick);
    }

    public Stage getStage() {
        return stage;
    }
}
