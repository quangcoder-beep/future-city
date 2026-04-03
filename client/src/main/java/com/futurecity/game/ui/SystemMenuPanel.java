package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.futurecity.game.core.Main;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.TokenStoreManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

/**
 * Premium Neural Hub — System Menu Panel.
 * Features: Icon+text vertical layout, neon styling.
 */
public class SystemMenuPanel extends BasePanel {
    private final Main game;
    private final NetworkManager networkManager;
    private final GameHUD gameHUD;

    public SystemMenuPanel(Skin skin, Main game, NetworkManager networkManager, GameHUD gameHUD) {
        super("", skin);
        this.game = game;
        this.networkManager = networkManager;
        this.gameHUD = gameHUD;
        setSize(440, 520);
        populateContent();
    }

    @Override
    protected void populateContent() {
        contentTable.clear();
        contentTable.top().pad(25);

        // Header
        VisLabel hubTitle = new VisLabel("NEURAL HUB", "title-neon");
        contentTable.add(hubTitle).row();

        // Neon separator
        Image separator = new Image(VisUI.getSkin().getRegion("white"));
        separator.setColor(SkinLoader.NEON_CYAN);
        contentTable.add(separator).width(200).height(2).padTop(5).padBottom(8).row();

        VisLabel subStatus = new VisLabel("SYSTEM STATUS: OPERATIONAL", "metadata-label");
        contentTable.add(subStatus).padBottom(30).row();

        // Action Grid (2×2 + 1)
        VisTable gridTable = new VisTable();

        VisTable profileCell = createMenuCell("IDENTITY", "images/ui/icons/gps.png", () -> {
            if (gameHUD != null) gameHUD.showPanel(gameHUD.getNicknameSelectionPanel());
        });
        VisTable settingsCell = createMenuCell("SETTINGS", "images/ui/icons/check.png", () -> {
            if (gameHUD != null) gameHUD.showPanel(gameHUD.getSettingsPanel());
        });
        VisTable guideCell = createMenuCell("GUIDE", "images/ui/icons/list.png", () -> {
            if (gameHUD != null) gameHUD.showPanel(gameHUD.getTutorialPanel());
        });
        VisTable logoutCell = createMenuCell("LOGOUT", null, this::handleLogout);

        gridTable.add(profileCell).size(170, 90).pad(8);
        gridTable.add(settingsCell).size(170, 90).pad(8).row();
        gridTable.add(guideCell).size(170, 90).pad(8);
        gridTable.add(logoutCell).size(170, 90).pad(8).row();

        contentTable.add(gridTable).expandX().fillX().row();

        // Terminate button (full width)
        VisTextButton exitBtn = new VisTextButton("QUIT GAME", "neon-button");
        exitBtn.setColor(SkinLoader.NEON_RED);
        if (game.audioManager != null) game.audioManager.attachDefaultTo(exitBtn);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        contentTable.add(exitBtn).width(356).height(45).padTop(20).row();

        // Footer
        VisLabel footer = new VisLabel("LOC: FUTURE CITY // SYNC: 100%", "metadata-label");
        contentTable.add(footer).padTop(30);
    }

    /**
     * Creates a vertical menu cell with icon on top, text on bottom.
     */
    private VisTable createMenuCell(String text, String iconPath, Runnable action) {
        VisTable cell = new VisTable();
        if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            cell.setBackground(skin.getDrawable("card-bg"));
        }
        cell.pad(10);

        // Icon
        if (iconPath != null) {
            try {
                Texture iconTex = game.myAssetManager.get(iconPath, Texture.class);
                Image iconImg = new Image(iconTex);
                cell.add(iconImg).size(28, 28).padBottom(8).row();
            } catch (Exception ignored) {
                cell.add(new VisLabel("●")).padBottom(8).row();
            }
        } else {
            VisLabel placeholder = new VisLabel("⏻");
            placeholder.setColor(SkinLoader.NEON_RED);
            cell.add(placeholder).padBottom(8).row();
        }

        // Text
        VisLabel label = new VisLabel(text, "small");
        label.setColor(SkinLoader.TEXT_SECONDARY);
        cell.add(label);

        // Hover + click
        cell.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (pointer == -1) {
                    label.setColor(SkinLoader.NEON_CYAN);
                    if (game.audioManager != null) game.audioManager.playSfx("sounds/ui/hover.ogg");
                }
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (pointer == -1) label.setColor(SkinLoader.TEXT_SECONDARY);
            }
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.audioManager != null) game.audioManager.playSfx("sounds/ui/click.ogg");
                if (action != null) action.run();
            }
        });

        return cell;
    }

    private void handleLogout() {
        TokenStoreManager.clear();
        networkManager.disconnect();
        game.setScreen(new LoginScreen(game));
    }
}
