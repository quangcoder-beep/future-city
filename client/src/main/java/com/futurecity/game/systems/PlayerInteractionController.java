package com.futurecity.game.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.futurecity.game.entities.Interactable;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.inputs.IInputController;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.ui.GameHUD;

import java.util.List;

/**
 * Điều khiển tương tác: F mở/đóng panel, H mở/đóng inventory.
 */
public class PlayerInteractionController {
    private MainCharactor player;
    private IInputController inputController;
    private InteractionSystem interactionSystem;
    private NetworkManager networkManager;
    private GameHUD gameHUD;
    private java.util.function.Supplier<Boolean> uiFocusChecker;

    // Lưu lại mục tiêu hiện tại để GameScreen lấy ra vẽ UI
    private Interactable currentTarget = null;

    public PlayerInteractionController(MainCharactor player, IInputController inputController,
            NetworkManager networkManager) {
        this.player = player;
        this.inputController = inputController;
        this.interactionSystem = new InteractionSystem();
        this.networkManager = networkManager;
    }

    /**
     * Set a checker to determine if the UI currently has focus on an input field.
     */
    public void setUiFocusChecker(java.util.function.Supplier<Boolean> checker) {
        this.uiFocusChecker = checker;
    }

    private boolean isUiFocused() {
        return uiFocusChecker != null && uiFocusChecker.get();
    }

    public void setGameHUD(GameHUD gameHUD) {
        this.gameHUD = gameHUD;
    }

    public void update(float delta,
            Camera camera,
            boolean isFPS,
            List<Interactable> interactables) {

        // 1. Cập nhật hệ thống tương tác (Quét tìm Shop/NPC)
        interactionSystem.update(player, camera, isFPS, interactables);

        // Lấy kết quả mục tiêu đang được focus (để vẽ UI "Bấm F...")
        currentTarget = interactionSystem.getCurrentFocusTarget();

        // 2. Xử lý Input (Skip if UI is typing)
        if (isUiFocused()) return;

        handleInteractionInput();
        handleInventoryInput();
        handlePhoneInput();
        handleEscapeInput();
        // Driver panel removed; phone handles delivery tasks now.
    }

    /**
     * ESC key: Nếu panel đang mở → đóng tất cả.
     * Nếu không có panel nào mở → Hiện Menu hệ thống.
     */
    private void handleEscapeInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (gameHUD != null) {
                if (gameHUD.isAnyPanelOpen()) {
                    gameHUD.closeAllPanels();
                } else {
                    gameHUD.toggleSystemMenu();
                }
            }
        }
    }

    /**
     * F key: Nếu panel đang mở → đóng tất cả.
     * Nếu có target → gửi request mở panel.
     */
    private void handleInteractionInput() {
        if (inputController.isInteractJustPressed()) {
            System.out.println("DEBUG: F key pressed!");
            // Panel đang mở → đóng
            if (gameHUD != null && gameHUD.isAnyPanelOpen()) {
                System.out.println("DEBUG: Panel is open, closing all.");
                gameHUD.closeAllPanels();
                return;
            }

            // Có target → gửi interaction request
            if (currentTarget != null) {
                System.out.println("DEBUG: Interacting with " + currentTarget.getNameId());
                networkManager.sendInteraction(currentTarget.getNameId(), currentTarget.getAction());
            } else {
                System.out.println("DEBUG: Nothing to interact with! (currentTarget is null)");
            }
        }
    }

    /**
     * H key: Toggle inventory panel.
     */
    private void handleInventoryInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            if (gameHUD != null) {
                gameHUD.toggleInventory();
            }
        }
    }

    /**
     * P key: Toggle phone panel.
     */
    private void handlePhoneInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            if (gameHUD != null) {
                gameHUD.togglePhone();
            }
        }
    }

    // Getter để GameScreen lấy dữ liệu vẽ chữ lên màn hình
    public Interactable getCurrentTarget() {
        return currentTarget;
    }

    public com.badlogic.gdx.graphics.Camera getCamera() {
        return interactionSystem.getCamera();
    }
}
