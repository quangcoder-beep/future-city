package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.futurecity.game.entities.RemotePlayer;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.shared.config.GameConstants;
import com.kotcrab.vis.ui.VisUI;
import java.util.HashMap;
import java.util.Map;

/**
 * NameplateManager
 * Triển khai hệ thống nhãn tên Cyberpunk cho người chơi.
 * Sử dụng Stage 2D để vẽ text sắc nét phía trên model 3D.
 */
public class NameplateManager {
    private final Stage stage;
    private final Map<Integer, NameplateTable> nameplates = new HashMap<>();
    private final Vector3 temp3 = new Vector3();

    // Độ cao hiển thị: Chiều cao model * Tỉ lệ thế giới * 0.98 (sát xuống trán theo
    // yêu cầu)
    private final float verticalOffset = GameConstants.PLAYER_HEIGHT * GameConstants.WORLD_SCALE * 0.61f;

    public NameplateManager() {
        this.stage = new Stage(new ScreenViewport());
    }

    public void addNameplate(int id, String name, Color color) {
        if (nameplates.containsKey(id)) {
            nameplates.get(id).setNameText(name);
            nameplates.get(id).setLabelColor(color);
            return;
        }
        NameplateTable table = new NameplateTable(name, color);
        nameplates.put(id, table);
        stage.addActor(table);
    }

    public void updateNickname(int id, String name) {
        if (nameplates.containsKey(id)) {
            nameplates.get(id).setNameText(name);
        }
    }

    public void removeNameplate(int id) {
        NameplateTable table = nameplates.remove(id);
        if (table != null)
            table.remove();
    }

    public void update(float delta, Camera camera, Map<Integer, RemotePlayer> remotePlayers, MainCharactor localPlayer,
            boolean isFPS) {
        // 1. Cập nhật Local Player (Màu Trắng/Bạc)
        if (localPlayer != null) {
            String nick = localPlayer.getState().username;
            if (nick == null || nick.isEmpty())
                nick = "Citizen";

            // Nếu ở góc nhìn thứ nhất (FPS), ẩn nhãn tên của chính mình
            if (isFPS) {
                if (nameplates.containsKey(localPlayer.getId())) {
                    nameplates.get(localPlayer.getId()).setVisible(false);
                }
            } else {
                updateSingle(localPlayer.getId(), nick, localPlayer.getPosition(), camera, Color.WHITE, localPlayer);
            }
        }

        // 2. Cập nhật Remote Players (Màu Cyan)
        for (RemotePlayer rp : remotePlayers.values()) {
            updateSingle(rp.getId(), rp.getUsername(), rp.getPosition(), camera, Color.CYAN, localPlayer);
        }

        // 3. Tự động dọn dẹp
        nameplates.entrySet().removeIf(entry -> {
            int id = entry.getKey();
            boolean stillExists = (localPlayer != null && localPlayer.getId() == id) || remotePlayers.containsKey(id);
            if (!stillExists) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });

        stage.act(delta);
    }

    private void updateSingle(int id, String name, Vector3 pos, Camera camera, Color color, MainCharactor localPlayer) {
        if (!nameplates.containsKey(id)) {
            addNameplate(id, name, color);
        }
        NameplateTable table = nameplates.get(id);
        table.setLabelColor(color);

        temp3.set(pos).add(0, verticalOffset, 0);

        if (!camera.frustum.pointInFrustum(temp3)) {
            table.setVisible(false);
            return;
        }

        camera.project(temp3);
        table.setPosition(temp3.x - table.getWidth() / 2, temp3.y);
        table.setVisible(true);

        float dist = camera.position.dst(pos);
        float scale = 1.0f;
        if (dist > 50f) {
            scale = Math.max(0.4f, 1.0f - ((dist - 50f) / 1000f));
        }
        table.setScale(scale);

        // Độ đậm nhãn cho người khác
        float baseAlpha = 0.85f;

        // Độ đậm nhãn chính mình
        if (localPlayer != null && id == localPlayer.getId())
            baseAlpha = 0.7f;

        float alpha = baseAlpha;
        if (dist > 1000f)
            alpha = baseAlpha * 0.3f;
        else if (dist > 300f)
            alpha = baseAlpha * (1.0f - ((dist - 300f) / 1000f) * 0.7f);
        table.getColor().a = alpha;
    }

    public void draw() {
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }

    /**
     * Component hiển thị Label với nền NinePatch tối.
     */
    private static class NameplateTable extends Table {
        private final Label label;

        public NameplateTable(String name, Color color) {
            Skin skin = VisUI.getSkin();
            this.label = new Label(name, skin, "nameplate-neon");
            this.label.setColor(color);

            // Nền đen mờ 0.3 alpha giúp nhìn xuyên qua được
            setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.3f)));

            add(label).pad(2, 8, 2, 8);
            pack();
            setTransform(true);
        }

        public void setNameText(String text) {
            if (!label.getText().toString().equals(text)) {
                label.setText(text);
                pack();
            }
        }

        public void setLabelColor(Color color) {
            label.setColor(color);
        }
    }
}
