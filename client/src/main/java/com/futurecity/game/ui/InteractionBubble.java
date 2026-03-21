package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Hiá»ƒn thá»‹ má»™t bĂ³ng bĂ³ng tÆ°Æ¡ng tĂ¡c (Bubble) ná»•i trĂªn Ä‘áº§u má»¥c tiĂªu.
 * Tá»± Ä‘á»™ng chuyá»ƒn Ä‘á»•i tá»a Ä‘á»™ tá»« 3D sang 2D (Screen Space).
 */
public class InteractionBubble extends VisTable {
    private VisLabel actionLabel;
    private VisLabel keyLabel;
    private Vector3 targetPosition = new Vector3();

    public InteractionBubble(Skin skin) {
        setSkin(skin);
        
        // Background phong cĂ¡ch Sci-Fi
        try {
            setBackground(skin.getDrawable("textfield-bg"));
        } catch (Exception e) {}

        keyLabel = new VisLabel("[F]");
        keyLabel.setColor(Color.YELLOW);
        
        actionLabel = new VisLabel("");
        actionLabel.setColor(Color.CYAN);

        add(keyLabel).pad(5);
        add(actionLabel).pad(5);
        
        pack();
        setVisible(false);
    }

    public void update(Vector3 worldPos, String action, com.badlogic.gdx.graphics.Camera camera) {
        if (worldPos == null || action == null || action.isEmpty()) {
            setVisible(false);
            return;
        }

        this.targetPosition.set(worldPos);
        // ThĂªm má»™t khoáº£ng offset lĂªn phĂ­a trĂªn (Ä‘á»ƒ bubble bay trĂªn Ä‘áº§u)
        this.targetPosition.y += 180f; // TÆ°Æ¡ng Ä‘Æ°Æ¡ng chiá»u cao nhĂ¢n váº­t

        // Chuyá»ƒn sang mĂ n hĂ¬nh 2D
        Vector3 screenPos = camera.project(new Vector3(targetPosition));
        
        // Kiá»ƒm tra xem má»¥c tiĂªu cĂ³ á»Ÿ phĂ­a trÆ°á»›c camera khĂ´ng (z > 0 vĂ  z < 1)
        if (screenPos.z < 0 || screenPos.z > 1) {
            setVisible(false);
            return;
        }

        setVisible(true);
        actionLabel.setText(action.toUpperCase());
        
        // Cáº­p nháº­t vá»‹ trĂ­ cá»§a báº£ng UI trĂªn Stage
        // Note: Stage dĂ¹ng tá»a Ä‘á»™ Y hÆ°á»›ng lĂªn, LibGDX Camera.project dĂ¹ng Y hÆ°á»›ng lĂªn
        setPosition(screenPos.x - getWidth() / 2, screenPos.y);
        
        // Hiá»‡u á»©ng scale theo khoáº£ng cĂ¡ch (cĂ ng xa bĂ³ng cĂ ng nhá»)
        float dist = camera.position.dst(worldPos);
        float scale = Math.max(0.5f, 1.0f - (dist / 1000f));
        setScale(scale);
    }
}
