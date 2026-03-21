package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.futurecity.game.core.Main;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.TokenStoreManager;
import com.kotcrab.vis.ui.widget.VisTextButton;

/**
 * SystemMenuPanel
 * Menu hệ thống chứa nút Đăng xuất và Thoát game.
 */
public class SystemMenuPanel extends BasePanel {

    private final Main game;
    private final NetworkManager networkManager;

    public SystemMenuPanel(Skin skin, Main game, NetworkManager networkManager) {
        super("SYSTEM MENU", skin);
        this.game = game;
        this.networkManager = networkManager;
        
        setSize(320, 220); // Giảm chiều cao xuống vì đã bỏ 1 nút
        populateContent();
    }

    @Override
    protected void populateContent() {
        contentTable.clear();
        contentTable.center();

        // Nút Đăng xuất (Logout)
        VisTextButton logoutBtn = new VisTextButton("LOGOUT");
        logoutBtn.setColor(Color.YELLOW);
        logoutBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleLogout();
            }
        });

        // Nút Thoát Game (Exit Game)
        VisTextButton exitBtn = new VisTextButton("EXIT GAME");
        exitBtn.setColor(Color.RED);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        contentTable.add(logoutBtn).width(240).height(55).padBottom(15).row();
        contentTable.add(exitBtn).width(240).height(55);
    }

    private void handleLogout() {
        // 1. Xóa token để không tự động đăng nhập lần sau
        TokenStoreManager.clear();
        
        // 2. Ngắt kết nối Server
        if (networkManager != null) {
            networkManager.disconnect();
        }
        
        // 3. Chuyển về màn hình Đăng nhập
        game.setScreen(new LoginScreen(game));
    }
}
