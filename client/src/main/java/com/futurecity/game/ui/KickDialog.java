package com.futurecity.game.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.esotericsoftware.kryonet.Client;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;

/**
 * Dialog thông báo toàn màn hình khi người chơi bị kick do đăng nhập trùng tài khoản.
 * Modal — không thể tắt bằng ESC hay click ra ngoài.
 * Người chơi phải bấm OK để xác nhận, sau đó kết nối mới được đóng.
 */
public class KickDialog extends VisDialog {

    // Tham chiếu để result() truy cập được sau khi constructor kết thúc
    private final Client kryoClient;
    private final Runnable afterKick;

    public KickDialog(Stage stage, Client kryoClient, Runnable afterKick) {
        super("\u26a0  Phiên đăng nhập kết thúc");
        this.kryoClient = kryoClient;
        this.afterKick = afterKick;

        // Block toàn bộ input phía sau
        setModal(true);
        setMovable(false);

        // Nội dung thông báo
        VisLabel msg = new VisLabel("Your account has been logged in\nfrom another device.");
        msg.setAlignment(Align.center);
        getContentTable().add(msg).pad(30).row();

        // Nút OK duy nhất — object "kick_ok" được chuyển vào result()
        button("OK", "kick_ok");
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
        kryoClient.close();
        if (afterKick != null) afterKick.run();
    }
}
