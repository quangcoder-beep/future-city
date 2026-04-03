package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Displays an interaction bubble above a target object.
 * Automatically projects 3D world coordinates to 2D screen space.
 */
public class InteractionBubble extends VisTable {
    private VisLabel actionLabel;
    private VisLabel keyLabel;
    private Vector3 targetPosition = new Vector3();

    public InteractionBubble(Skin skin) {
        setSkin(skin);
        
        // Sci-Fi style background
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
        // Add an eye-level offset (so bubble floats above head)
        this.targetPosition.y += 180f; // Approx character height

        // Project to 2D screen space
        Vector3 screenPos = camera.project(new Vector3(targetPosition));
        
        // Check if target is in front of camera
        if (screenPos.z < 0 || screenPos.z > 1) {
            setVisible(false);
            return;
        }

        setVisible(true);
        actionLabel.setText(action.toUpperCase());
        
        // Update UI position on Stage
        // Note: Stage uses Y-up, Camera.project uses Y-up
        setPosition(screenPos.x - getWidth() / 2, screenPos.y);
        
        // Scale effect based on distance (farther bubbles are smaller)
        float dist = camera.position.dst(worldPos);
        float scale = Math.max(0.5f, 1.0f - (dist / 1000f));
        setScale(scale);
    }
}
