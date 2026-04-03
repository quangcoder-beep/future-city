package com.futurecity.game.core;

import com.badlogic.gdx.Game;
import com.futurecity.game.managers.MyAssetManager;

/**
 * Main
 * <p>
 * Class khá»Ÿi Ä‘áº§u cá»§a á»©ng dá»¥ng (Entry Point).
 * Káº¿ thá»«a tá»« com.badlogic.gdx.Game Ä‘á»ƒ quáº£n lĂ½ cĂ¡c Screen.
 */
public class Main extends Game {
    // Quáº£n lĂ½ tĂ i nguyĂªn táº­p trung (Model, Texture, Sound...)
    // Quá trình đăng nhập sẽ lưu thông tin người chơi vào đây
    public String nickname;
    public String avatarUrl;
    public long credits = 0;
    public int deliveries = 0;
    public float reputation = 100.0f;

    public MyAssetManager myAssetManager;
    public com.futurecity.game.managers.AudioManager audioManager;

    /**
     * Khá»Ÿi táº¡o cĂ¡c thĂ nh pháº§n cÆ¡ báº£n cá»§a game khi á»©ng dá»¥ng báº¯t
     * Ä‘áº§u.
     */
    @Override
    public void create() {
        // 1. Khá»Ÿi táº¡o Asset Manager má»›i
        myAssetManager = new MyAssetManager();
        audioManager = com.futurecity.game.managers.AudioManager.getInstance(myAssetManager);

        // 2. Chuyá»ƒn sang mĂ n hĂ¬nh LoadingScreen Ä‘á»ƒ load báº¥t Ä‘á»“ng bá»™
        setScreen(new com.futurecity.game.ui.LoadingScreen(this));
    }

    @Override
    public void render() {
        super.render(); // Gá»i render cá»§a Screen hiá»‡n táº¡i (GameScreen)
    }

    @Override
    public void pause() {
        super.pause();
        if (audioManager != null)
            audioManager.pauseAll();
    }

    @Override
    public void resume() {
        super.resume();
        if (audioManager != null)
            audioManager.resumeAll();
    }

    /**
     * Giáº£i phĂ³ng bá»™ nhá»› khi táº¯t trĂ² chÆ¡i.
     */
    @Override
    public void dispose() {
        // Giáº£i phĂ³ng tĂ i nguyĂªn khi thoĂ¡t game
        if (myAssetManager != null)
            myAssetManager.dispose();
    }
}
