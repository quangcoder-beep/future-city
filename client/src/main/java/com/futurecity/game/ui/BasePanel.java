package com.futurecity.game.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;

/**
 * Base class cho táº¥t cáº£ panel UI (Shop, Dialogue, Inventory...).
 * Cung cáº¥p: nĂºt [X], animation má»Ÿ/Ä‘Ă³ng, layout chuáº©n.
 */
public abstract class BasePanel extends VisWindow {
    protected Skin skin;
    protected VisTable contentTable;
    private boolean panelVisible = false;
    protected boolean fullScreenMode = false;

    public BasePanel(String title, Skin skin) {
        super(title);
        this.skin = skin;
        setSkin(skin); // Cáº§n thiáº¿t Ä‘á»ƒ getSkin() hoáº¡t Ä‘á»™ng á»Ÿ cĂ¡c lá»›p con

        setModal(false);
        setMovable(false);
        setResizable(false);
        setTouchable(Touchable.disabled);

        // NĂ¢ng cáº¥p giao diá»‡n Panel (NinePatch + Font sáº¯c nĂ©t)
        try {
            com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle style = new com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(skin.get(com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle.class));
            style.background = skin.getDrawable("panel-bg");
            try { style.titleFont = skin.getFont("title-font"); } catch (Exception e) {}
            this.setStyle(style);
        } catch (Exception e) {
            System.out.println("Could not setup BasePanel style: " + e.getMessage());
        }

        // VisWindow có sẵn addCloseButton()
        addCloseButton();

        // Content area
        contentTable = new VisTable();
        add(contentTable).expand().fill().pad(10);

        // Máº·c Ä‘á»‹nh áº©n
        setVisible(false);
        setColor(1, 1, 1, 0);
    }

    /**
     * Override close() cá»§a VisWindow.
     * NĂºt [X] (addCloseButton) gá»i close() ná»™i bá»™ â†’ chuyá»ƒn sang hidePanel()
     * Ä‘á»ƒ Ä‘áº£m báº£o panelVisible Ä‘Æ°á»£c cáº­p nháº­t vĂ  cursor Ä‘Æ°á»£c khĂ³a láº¡i.
     */
    @Override
    protected void close() {
        hidePanel();
    }

    /**
     * Hiá»‡n panel vá»›i animation fade in.
     */
    public void showPanel() {
        if (panelVisible)
            return;
        panelVisible = true;
        setVisible(true);
        setTouchable(Touchable.enabled);
        setModal(true);

        // CÄƒn chá»‰nh vá»‹ trĂ­ vĂ  kĂ­ch thÆ°á»›c
        Stage stage = getStage();
        if (stage != null) {
            if (fullScreenMode) {
                setSize(stage.getWidth(), stage.getHeight());
                setPosition(0, 0);
            } else {
                setPosition(
                        (stage.getWidth() - getWidth()) / 2,
                        (stage.getHeight() - getHeight()) / 2);
            }
        }

        // Animation
        clearActions();
        setColor(1, 1, 1, 0);
        addAction(Actions.fadeIn(0.2f));
    }

    /**
     * áº¨n panel vá»›i animation fade out.
     */
    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * áº¨n panel vá»›i animation fade out.
     */
    public void hidePanel() {
        // Äáº£m báº£o modal luĂ´n Ä‘Æ°á»£c táº¯t ngay láº­p tá»©c
        setModal(false);

        if (!panelVisible)
            return;
        panelVisible = false;

        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

        // Cá»°C Ká»² QUAN TRá»ŒNG: Má»Ÿ khĂ³a bĂ n phĂ­m NGAY Láº¬P Tá»¨C Ä‘á»ƒ Stage khĂ´ng Äƒn máº¥t phĂ­m
        // WASD
        // náº¿u ngÆ°á»i chÆ¡i báº¥m Ä‘i chuyá»ƒn trong lĂºc panel Ä‘ang cháº¡y animation má» dáº§n 0.2s.
        if (getStage() != null) {
            getStage().unfocusAll();
        }

        System.out.println("DEBUG: hidePanel called for " + getClass().getSimpleName());

        clearActions();
        addAction(Actions.sequence(
                Actions.fadeOut(0.2f),
                Actions.run(() -> {
                    System.out.println("DEBUG: FadeOut complete for " + getClass().getSimpleName());
                    setVisible(false);
                    // Reset for next show (optional, but safely)
                    setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);

                    if (onClose != null) {
                        onClose.run();
                    }
                })));
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    /**
     * Toggle hiá»ƒn thá»‹ panel (F key).
     */
    public void toggle() {
        if (panelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    /**
     * Cáº­p nháº­t kĂ­ch thÆ°á»›c khi mĂ n hĂ¬nh thay Ä‘á»•i.
     */
    public void resize(float width, float height) {
        if (fullScreenMode && panelVisible) {
            setSize(width, height);
            setPosition(0, 0);
        } else if (panelVisible) {
            setPosition((width - getWidth()) / 2, (height - getHeight()) / 2);
        }
    }

    /**
     * Subclass override Ä‘á»ƒ populate ná»™i dung trÆ°á»›c khi show.
     */
    protected abstract void populateContent();
}
