package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.math.Interpolation;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;

/**
 * Premium BasePanel with smooth animations and dark overlay backdrop.
 * All panels in the game extend this class.
 */
public abstract class BasePanel extends VisWindow {
    protected Skin skin;
    protected VisTable contentTable;
    private boolean panelVisible = false;
    protected boolean fullScreenMode = false;

    // Dark overlay backdrop
    private Image darkOverlay;

    public BasePanel(String title, Skin skin) {
        super(title);
        this.skin = skin;
        setSkin(skin);

        setModal(false);
        setMovable(false);
        setResizable(false);
        setTouchable(Touchable.disabled);

        // Upgrade Panel visuals
        try {
            com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle style = new com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(
                    skin.get(com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle.class));

            if (skin.has("panel-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                style.background = skin.getDrawable("panel-neon");
            } else {
                style.background = skin.getDrawable("panel-bg");
            }

            try {
                style.titleFont = skin.getFont("title-font");
            } catch (Exception e) {
            }
            this.setStyle(style);
        } catch (Exception e) {
            System.out.println("Could not setup BasePanel style: " + e.getMessage());
        }

        // Close button with sound
        addCloseButton();
        try {
            Actor closeBtn = null;
            for (Actor actor : getChildren()) {
                if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.ImageButton) {
                    closeBtn = actor;
                    break;
                }
            }
            if (closeBtn != null) {
                com.futurecity.game.core.Main main = (com.futurecity.game.core.Main) Gdx.app.getApplicationListener();
                if (main.audioManager != null) {
                    main.audioManager.attachDefaultTo(closeBtn);
                }
            }
        } catch (Exception ignored) {
        }

        // Content area
        contentTable = new VisTable();
        add(contentTable).expand().fill().pad(10);

        // Hidden by default
        setVisible(false);
        setColor(1, 1, 1, 0);
    }

    /**
     * Creates the dark overlay backdrop (called lazily on first show).
     */
    private void ensureOverlay() {
        if (darkOverlay != null) return;
        try {
            if (skin.has("dark-overlay", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                darkOverlay = new Image(skin.getDrawable("dark-overlay"));
                darkOverlay.setFillParent(true);
                darkOverlay.setTouchable(Touchable.disabled);
                darkOverlay.setVisible(false);
                darkOverlay.setColor(1, 1, 1, 0);
            }
        } catch (Exception e) {
            // Overlay is optional, not critical
        }
    }

    @Override
    protected void close() {
        hidePanel();
    }

    /**
     * Show panel with premium scale+fade animation and dark backdrop.
     */
    public void showPanel() {
        if (panelVisible)
            return;
        panelVisible = true;
        setVisible(true);
        setTouchable(Touchable.enabled);
        setModal(true);
        Gdx.input.setCursorCatched(false);

        Stage stage = getStage();
        if (stage != null) {
            // Show dark overlay behind the panel
            ensureOverlay();
            if (darkOverlay != null) {
                darkOverlay.setVisible(true);
                darkOverlay.setTouchable(Touchable.enabled);
                // Ensure overlay is behind this panel
                if (darkOverlay.getParent() == null) {
                    stage.addActor(darkOverlay);
                }
                darkOverlay.toFront();
                this.toFront();
                darkOverlay.clearActions();
                darkOverlay.setColor(1, 1, 1, 0);
                darkOverlay.addAction(Actions.alpha(1f, 0.25f));
            }

            // Position
            if (fullScreenMode) {
                setSize(stage.getWidth(), stage.getHeight());
                setPosition(0, 0);
            } else {
                setPosition(
                        (stage.getWidth() - getWidth()) / 2,
                        (stage.getHeight() - getHeight()) / 2);
            }
        }

        // Premium open animation: scale up + fade in
        clearActions();
        setColor(1, 1, 1, 0);
        setOrigin(getWidth() / 2f, getHeight() / 2f);
        setScale(0.92f);
        addAction(Actions.parallel(
                Actions.fadeIn(0.25f, Interpolation.fade),
                Actions.scaleTo(1f, 1f, 0.3f, Interpolation.swingOut)));
    }

    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Hide panel with premium shrink+fade animation.
     */
    public void hidePanel() {
        setModal(false);

        if (!panelVisible)
            return;
        panelVisible = false;

        setTouchable(Touchable.disabled);

        // Immediately release keyboard focus
        if (getStage() != null) {
            getStage().unfocusAll();
        }

        // Fade out dark overlay
        if (darkOverlay != null && darkOverlay.isVisible()) {
            darkOverlay.setTouchable(Touchable.disabled);
            darkOverlay.clearActions();
            darkOverlay.addAction(Actions.sequence(
                    Actions.alpha(0f, 0.2f),
                    Actions.visible(false)));
        }

        // Premium close animation: scale down + fade out
        clearActions();
        setOrigin(getWidth() / 2f, getHeight() / 2f);
        addAction(Actions.sequence(
                Actions.parallel(
                        Actions.fadeOut(0.2f, Interpolation.fade),
                        Actions.scaleTo(0.95f, 0.95f, 0.2f, Interpolation.exp5In)),
                Actions.run(() -> {
                    setVisible(false);
                    setScale(1f); // Reset scale for next show
                    setTouchable(Touchable.enabled);
                    if (onClose != null) {
                        onClose.run();
                    }
                })));
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void toggle() {
        if (panelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    public void resize(float width, float height) {
        if (fullScreenMode && panelVisible) {
            setSize(width, height);
            setPosition(0, 0);
        } else if (panelVisible) {
            setPosition((width - getWidth()) / 2, (height - getHeight()) / 2);
        }
    }

    /**
     * Makes a ScrollPane auto-focus on mouse hover so scroll wheel works without clicking first.
     */
    protected static void enableAutoScrollFocus(final com.kotcrab.vis.ui.widget.VisScrollPane scrollPane) {
        scrollPane.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y,
                    int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (scrollPane.getStage() != null) {
                    scrollPane.getStage().setScrollFocus(scrollPane);
                }
            }

            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y,
                    int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (scrollPane.getStage() != null && scrollPane.getStage().getScrollFocus() == scrollPane) {
                    scrollPane.getStage().setScrollFocus(null);
                }
            }
        });
    }

    protected abstract void populateContent();
}
