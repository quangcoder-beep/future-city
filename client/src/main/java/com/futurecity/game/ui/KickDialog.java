package com.futurecity.game.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.futurecity.game.core.Main;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.TokenStoreManager;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;

/**
 * Dialog thông báo toàn màn hình khi người chơi bị kick do đăng nhập trùng tài khoản.
 * Modal — không thể tắt bằng ESC hay click ra ngoài.
 * Người chơi phải bấm OK để xác nhận, sau đó kết nối mới được đóng.
 */
public class KickDialog extends VisDialog {

    private final Main game;
    private final NetworkManager networkManager;
    private final Runnable afterKick;

    public KickDialog(Stage stage, Main game, NetworkManager networkManager, Runnable afterKick) {
        super("\u26a0  Phiên đăng nhập kết thúc");
        this.game = game;
        this.networkManager = networkManager;
        this.afterKick = afterKick;

        // Block toàn bộ input phía sau
        setModal(true);
        setMovable(false);

        // Nội dung thông báo
        VisLabel msg = new VisLabel("Your account has been logged in\nfrom another device.");
        msg.setAlignment(Align.center);
        getContentTable().add(msg).pad(30).row();

        // Nút OK duy nhất
        com.kotcrab.vis.ui.widget.VisTextButton okBtn = new com.kotcrab.vis.ui.widget.VisTextButton("OK");
        if (game.audioManager != null) {
            game.audioManager.attachDefaultTo(okBtn);
        }
        button(okBtn, "kick_ok");
        padBottom(16);

        setWidth(420);
        setHeight(210);
        centerWindow();
        show(stage);
    }

    /**
     * Được gọi khi người chơi bấm nút OK.
     * object = "kick_ok" (giá trị đã đặt trong button()).
     */
    @Override
    protected void result(Object object) {
        // 1. XĂ³a token lÆ°u á»Ÿ mĂ¡y
        TokenStoreManager.clear();

        // 2. Ngáº¯t káº¿t ná»‘i tá»›i Server
        if (networkManager != null) {
            networkManager.disconnect();
        }

        // 3. Chuyá»ƒn ngÆ°á» i chÆ¡i ra mĂ n hĂ¬nh Login
        game.setScreen(new LoginScreen(game));

        if (afterKick != null)
            afterKick.run();
    }
}
