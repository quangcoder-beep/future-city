package com.futurecity.game.systems;

import com.futurecity.game.entities.RemotePlayer;
import com.futurecity.game.managers.NetworkManager;

/**
 * Há»‡ thá»‘ng Ä‘á»“ng bá»™ máº¡ng phĂ­a Client.
 * Chá»‹u trĂ¡ch nhiá»‡m cáº­p nháº­t vá»‹ trĂ­ vĂ  animation cá»§a cĂ¡c ngÆ°á»i chÆ¡i khĂ¡c tá»«
 * Server.
 */
public class ClientNetworkSyncSystem {

    /**
     * Cáº­p nháº­t tráº¡ng thĂ¡i cá»§a táº¥t cáº£ ngÆ°á»i chÆ¡i khĂ¡c
     * 
     * @param delta          Thá»i gian trĂ´i qua giá»¯a 2 frame
     * @param networkManager Quáº£n lĂ½ káº¿t ná»‘i máº¡ng
     */
    public void update(float delta, NetworkManager networkManager) {
        if (networkManager == null)
            return;

        // Cáº­p nháº­t tá»«ng ngÆ°á»i chÆ¡i tá»« xa (Remote Players)
        // Viá»‡c nĂ y bao gá»“m ná»™i suy vá»‹ trĂ­ (interpolation) Ä‘á»ƒ di chuyá»ƒn mÆ°á»£t mĂ 
        for (RemotePlayer remote : networkManager.getRemotePlayers().values()) {
            remote.update(delta);
        }
    }
}
