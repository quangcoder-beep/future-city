package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.futurecity.game.core.Main;
import com.futurecity.game.managers.HttpAuthClientManager;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

/**
 * Expert Redesign: Profile & Nickname Selection Panel.
 * Centered Avatar Layout with Inline Rename.
 */
public class NicknameSelectionPanel extends BasePanel {
    private final Main game;
    private final String accessToken;
    private final String avatarUrl;

    private VisLabel nicknameLabel;
    private VisImageButton renameBtn;
    private VisLabel statusLabel; // This is the main panel status
    private VisLabel dialogStatusLabel; // NEW: for the dialog
    private VisTextButton confirmBtn;
    private Image avatarImage;
    private VisTextField dialogNicknameField;
    private boolean needsInitialRename;

    private VisLabel credVal;
    private VisLabel delivVal;
    private VisLabel repVal;

    public NicknameSelectionPanel(Skin skin, Main game, String accessToken, String avatarUrl, boolean needsInitialRename) {
        super("", skin);
        getTitleLabel().setText("");
        this.game = game;
        this.accessToken = accessToken;
        this.avatarUrl = avatarUrl;
        this.needsInitialRename = needsInitialRename;

        setSize(400, 600);
        populateContent();
        loadAvatar();
    }

    @Override
    protected void populateContent() {
        contentTable.clear();

        // --- PRO-HEADER ---
        VisLabel header = new VisLabel("NEURAL IDENTITY", "title-neon");
        contentTable.add(header).padTop(70).padBottom(30).row();

        // --- CENTERED AVATAR ---
        com.badlogic.gdx.scenes.scene2d.ui.Stack avatarStack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
        Image bgBox = new Image(VisUI.getSkin().getDrawable("white"));
        bgBox.setColor(new Color(0.3f, 0.8f, 1.0f, 0.2f));
        avatarStack.add(bgBox);

        avatarImage = new Image(VisUI.getSkin().getDrawable("white"));
        avatarImage.setColor(new Color(1, 1, 1, 0.15f));
        avatarImage.setScaling(com.badlogic.gdx.utils.Scaling.fill);

        VisTable imgContainer = new VisTable();
        imgContainer.add(avatarImage).size(160);
        avatarStack.add(imgContainer);

        contentTable.add(avatarStack).size(180).padBottom(25).row();

        // --- NAME ROW ---
        VisTable nameRow = new VisTable();
        nicknameLabel = new VisLabel(game.nickname != null ? game.nickname : "CYBER_NOMAD_01", "title");
        nicknameLabel.setColor(Color.CYAN);
        nameRow.add(nicknameLabel).padRight(10);

        renameBtn = new VisImageButton(VisUI.getSkin().getDrawable("icon-list"));
        renameBtn.setColor(Color.WHITE);
        nameRow.add(renameBtn).size(24);
        contentTable.add(nameRow).padBottom(20).row();

        // --- METADATA SECTION ---
        VisTable meta = new VisTable();
        meta.setBackground(VisUI.getSkin().getDrawable("button-over"));
        meta.pad(20);

        meta.add(new VisLabel("CREDITS: ", "metadata-label")).left();
        credVal = new VisLabel(game.credits + " FC", "metadata-label");
        credVal.setColor(Color.GOLD);
        meta.add(credVal).left().row();

        meta.add(new VisLabel("DELIVERIES: ", "metadata-label")).left().padTop(8);
        delivVal = new VisLabel(String.valueOf(game.deliveries), "metadata-label");
        delivVal.setColor(Color.WHITE);
        meta.add(delivVal).left().row();

        meta.add(new VisLabel("SOCIAL REP: ", "metadata-label")).left().padTop(8);
        repVal = new VisLabel(String.valueOf(game.reputation) + "%", "metadata-label");
        repVal.setColor(Color.CYAN);
        meta.add(repVal).left().row();

        contentTable.add(meta).width(340).padBottom(30).row();

        // --- FOOTER / STATUS ---
        statusLabel = new VisLabel("NEURAL HANDSHAKE: STABLE", "small");
        statusLabel.setColor(new Color(1, 1, 1, 0.3f));
        contentTable.add(statusLabel).padBottom(20).row();

        confirmBtn = new VisTextButton("CLOSE LINK", "neon-button");
        if (game.audioManager != null) {
            game.audioManager.attachDefaultTo(confirmBtn);
            game.audioManager.attachDefaultTo(renameBtn);
        }
        contentTable.add(confirmBtn).width(280).height(55).padBottom(30).row();

        // Listeners
        renameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                showRenameDialog();
            }
        });

        confirmBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                hidePanel();
            }
        });
    }

    private void showRenameDialog() {
        VisDialog dialog = new VisDialog("UPDATE CITIZEN ALIAS");
        dialog.addCloseButton(); // ADDED [X] BUTTON
        dialog.setKeepWithinStage(true);
        dialog.setModal(true);

        VisTable dialogContent = new VisTable();
        dialogContent.pad(20);

        dialogNicknameField = new VisTextField(game.nickname != null ? game.nickname : "");
        dialogNicknameField.setMessageText("ENTER NEW NICKNAME");
        dialogContent.add(new VisLabel("ENTER NEW ALIAS:")).padBottom(10).row();
        dialogContent.add(dialogNicknameField).width(250).height(40).row();

        VisTextButton updateBtn = new VisTextButton("UPDATE ID", "neon-button");
        VisTextButton cancelBtn = new VisTextButton("CANCEL", "neon-button");
        cancelBtn.setColor(Color.SCARLET);

        dialogStatusLabel = new VisLabel("");
        dialogStatusLabel.setColor(Color.YELLOW);
        dialogContent.add(dialogStatusLabel).padTop(10).row();
        
        dialog.getContentTable().add(dialogContent);
        dialog.getButtonsTable().add(updateBtn).size(150, 45).pad(10);
        dialog.getButtonsTable().add(cancelBtn).size(150, 45).pad(10);

        updateBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                handleConfirmRename(dialog);
            }
        });

        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                dialog.hide();
            }
        });

        dialog.show(getStage());
    }

    private void handleConfirmRename(VisDialog dialog) {
        final String nick = dialogNicknameField.getText().trim();
        if (nick.length() < 3)
            return;

        HttpAuthClientManager httpClient = new HttpAuthClientManager("futurecity.duckdns.org", 443, true);
        httpClient.setNickname(accessToken, nick, new HttpAuthClientManager.SimpleCallback() {
            @Override
            public void onSuccess(String result) {
                Gdx.app.postRunnable(() -> {
                    if ("SUCCESS".equals(result)) {
                        game.nickname = nick;
                        nicknameLabel.setText(nick);
                        dialog.hide();
                    } else {
                        dialogStatusLabel.setText("REJECTED: " + result);
                        dialogStatusLabel.setColor(Color.RED);
                    }
                });
            }
            @Override 
            public void onError(String message) {
                Gdx.app.postRunnable(() -> {
                    dialogStatusLabel.setText("ERR: " + message.toUpperCase());
                    dialogStatusLabel.setColor(Color.RED);
                });
            }
        });
    }

    private void loadAvatar() {
        if (avatarUrl == null || avatarUrl.isEmpty() || avatarUrl.equals("null"))
            return;

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(avatarUrl);
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final byte[] bytes = httpResponse.getResult();
                Gdx.app.postRunnable(() -> {
                    try {
                        Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                        Texture texture = new Texture(pixmap);
                        avatarImage.setDrawable(new TextureRegionDrawable(new TextureRegion(texture)));
                        avatarImage.setColor(Color.WHITE);
                        avatarImage.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                        pixmap.dispose();
                    } catch (Exception e) {
                        Gdx.app.error("Avatar", "Failed to load profile avatar");
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
            }

            @Override
            public void cancelled() {
            }
        });
    }

    @Override
    public void showPanel() {
        refresh();
        super.showPanel();
        if (needsInitialRename) {
            showRenameDialog();
            // Reset to false after opening so it doesn't pop up again every time the panel is opened
            needsInitialRename = false;
        }
    }

    public void refresh() {
        if (nicknameLabel != null) nicknameLabel.setText(game.nickname != null ? game.nickname : "CYBER_NOMAD_01");
        if (credVal != null) credVal.setText(game.credits + " FC");
        if (delivVal != null) delivVal.setText(String.valueOf(game.deliveries));
        if (repVal != null) repVal.setText(String.format("%.1f%%", game.reputation));
    }
}
