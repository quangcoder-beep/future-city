package com.example.server_spring.services;

import com.esotericsoftware.kryonet.Connection;
import com.example.server_spring.entity.ServerPlayer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quáº£n lĂ½ danh sĂ¡ch ngÆ°á»i chÆ¡i Ä‘ang online.
 * TÆ°Æ¡ng Ä‘Æ°Æ¡ng PlayerManager cÅ©.
 */
@Service
public class PlayerService {

    @org.springframework.beans.factory.annotation.Autowired
    private AoIGrid aoiGrid;

    private final Map<Integer, ServerPlayer> players = new ConcurrentHashMap<>();

    public void add(ServerPlayer player) {
        players.put(player.getId(), player);
        System.out.println("[PLAYER] + " + player.getUsername() + " (ID=" + player.getId() + ")");
    }

    public void remove(int id) {
        ServerPlayer removed = players.remove(id);
        if (removed != null) {
            aoiGrid.removePlayer(removed);
            System.out.println("[PLAYER] - " + removed.getUsername() + " (ID=" + id + ")");
        }
    }

    public ServerPlayer get(int id) {
        return players.get(id);
    }

    public Collection<ServerPlayer> getAll() {
        return players.values();
    }

    public Collection<ServerPlayer> getAllPlayers() {
        return players.values();
    }

    public void broadcastTCP(Object packet) {
        for (ServerPlayer p : new ArrayList<>(players.values())) {
            Connection c = p.getConnection();
            if (c != null)
                c.sendTCP(packet);
        }
    }

    public void broadcastUDP(Object packet) {
        for (ServerPlayer p : players.values()) {
            try {
                p.getConnection().sendUDP(packet);
            } catch (Exception e) {
                /* disconnected */ }
        }
    }

    public void disconnectAll() {
        for (ServerPlayer p : players.values()) {
            Connection c = p.getConnection();
            if (c != null)
                c.close();
        }
        players.clear();
    }
}
