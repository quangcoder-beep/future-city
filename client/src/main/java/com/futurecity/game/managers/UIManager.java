package com.futurecity.game.managers;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.futurecity.game.core.Main;
import com.futurecity.game.entities.Interactable;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.entities.RemotePlayer;
import com.futurecity.game.systems.PlayerInteractionController;
import com.futurecity.game.ui.GameHUD;
import com.futurecity.shared.packets.resonse.InventoryResponse;

/**
 * UIManager
 * Chịu trách nhiệm LOGIC giao diện.
 * - Theo dõi trạng thái game (ví dụ: đang nhắm vào vật thể nào).
 * - Cập nhật dữ liệu cho GameHUD để hiển thị.
 */
public class UIManager {
    private GameHUD gameHUD;
    private PlayerInteractionController playerController;

    public UIManager(Main game, SpriteBatch spriteBatch, PlayerInteractionController playerController,
            MainCharactor player, MapManager mapManager,
            net.mgsx.gltf.scene3d.scene.SceneManager sceneManager, CameraManager cameraManager,
            NetworkManager networkManager) {
        this.gameHUD = new GameHUD(game, spriteBatch, player, mapManager, sceneManager, cameraManager, networkManager);
        networkManager.setGameHUD(this.gameHUD);
        this.playerController = playerController;
        this.playerController.setGameHUD(this.gameHUD);
    }

    public Stage getStage() {
        return gameHUD.getStage();
    }

    public void update(float delta) {
        // 1. Kiểm tra tương tác
        updateInteractionUI();

        // 2. Cập nhật Minimap (Vẽ FBO) trước khi render UI
        gameHUD.updateMinimap(delta);

        // 3. Render HUD
        gameHUD.render(delta);
    }

    private void updateInteractionUI() {
        Interactable target = playerController.getCurrentTarget();
        if (target != null) {
            String msg = "[F] " + target.getInteractionPrompt();

            // SMART PROMPT logic
            if (target instanceof RemotePlayer) {
                RemotePlayer rp = (RemotePlayer) target;
                int targetDbId = rp.getDbUserId();

                for (InventoryResponse.OrderInfo order : gameHUD
                        .getActiveDeliveries()) {
                    if ("DELIVERING".equals(order.status) && "PLAYER".equals(order.buyerType)) {
                        try {
                            int buyerRefId = Integer.parseInt(order.buyerRefId);
                            if (buyerRefId == targetDbId) {
                                msg = "[F] DELIVER TO " + rp.getUsername().toUpperCase();
                                break;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } else {
                String targetId = target.getNameId();
                for (InventoryResponse.OrderInfo order : gameHUD
                        .getActiveDeliveries()) {
                    if ("PENDING_PICKUP".equals(order.status)) {
                        if (order.shopId != null && order.shopId.equals(targetId)) {
                            msg = "[F] PICK UP ORDER (ORDER #" + order.orderId + ")";
                            break;
                        }
                    } else if ("DELIVERING".equals(order.status)) {
                        // If it's a building or NPC destination
                        if (targetId != null && targetId.equals(order.destinationName)) {
                            msg = "[F] DELIVER ORDER (ORDER #" + order.orderId + ")";
                            break;
                        }
                    }
                }
            }

            com.badlogic.gdx.math.collision.BoundingBox box = target.getCollisionBox();
            com.badlogic.gdx.math.Vector3 playerPos = playerController.getCamera().position;

            com.badlogic.gdx.math.Vector3 targetPos = new com.badlogic.gdx.math.Vector3(
                    Math.max(box.min.x, Math.min(playerPos.x, box.max.x)),
                    Math.max(box.min.y, Math.min(playerPos.y, box.max.y)),
                    Math.max(box.min.z, Math.min(playerPos.z, box.max.z)));

            if (targetPos.y < playerPos.y - 10f)
                targetPos.y = playerPos.y;
            if (targetPos.y > playerPos.y + 20f)
                targetPos.y = playerPos.y + 10f;

            gameHUD.setInteractionMessage(msg, targetPos, playerController.getCamera());
        } else {
            gameHUD.setInteractionMessage("", null, null);
        }
    }

    public void resize(int width, int height) {
        gameHUD.resize(width, height);
    }

    public void dispose() {
        gameHUD.dispose();
    }
}
