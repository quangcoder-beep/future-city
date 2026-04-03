package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Lightweight tooltip that appears on hover above HUD buttons.
 * Auto-hides when mouse leaves the target actor.
 */
public class HudTooltip extends VisTable {
    private VisLabel label;

    public HudTooltip(Skin skin) {
        super();
        setVisible(false);
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

        // Background
        if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            setBackground(skin.getDrawable("card-bg"));
        }
        pad(6, 12, 6, 12);

        // Label
        label = new VisLabel("", "small");
        label.setColor(SkinLoader.NEON_CYAN);
        add(label);
    }

    /**
     * Attach this tooltip to an Actor. Shows on enter, hides on exit.
     */
    public void attachTo(Actor target, String text) {
        target.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer != -1) return; // Only mouse, not touch drag
                show(target, text);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer != -1) return;
                hide();
            }
        });
    }

    private void show(Actor target, String text) {
        label.setText(text);
        pack();

        // Position above the target
        float tx = target.getX() + target.getWidth() / 2f - getWidth() / 2f;
        float ty = target.getY() - getHeight() - 5f;
        setPosition(tx, ty);

        setVisible(true);
        clearActions();
        setColor(1, 1, 1, 0);
        addAction(Actions.parallel(
                Actions.fadeIn(0.12f),
                Actions.moveBy(0, -3, 0.12f)
        ));
    }

    private void hide() {
        clearActions();
        addAction(Actions.sequence(
                Actions.fadeOut(0.1f),
                Actions.visible(false)
        ));
    }
}
