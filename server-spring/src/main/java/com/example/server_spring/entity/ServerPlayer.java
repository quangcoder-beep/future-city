package com.example.server_spring.entity;

import com.esotericsoftware.kryonet.Connection;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.AnimationName;

public class ServerPlayer {
    private final int id;
    private final String username;
    private final Connection connection;
    private final PlayerState state;
    private int dbUserId = -1;

    public final java.util.Queue<com.futurecity.shared.packets.request.MovementRequest> pendingInputs = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private float groundHeight = 0f;

    public void setCredits(int c) { this.state.credits = c; }
    public void setDeliveries(int d) { this.state.deliveries = d; }
    public void setReputation(float r) { this.state.reputation = r; }

    public int getCredits() { return this.state.credits; }
    public int getDeliveries() { return this.state.deliveries; }
    public float getReputation() { return this.state.reputation; }

    public ServerPlayer(int id, String username, Connection connection) {
        this.id = id;
        this.username = username;
        this.connection = connection;
        this.state = new PlayerState();
        this.state.id = id;
        this.state.username = username;
        this.state.position.set(560f, 50f, 0f);
        this.state.currentAnimation = AnimationName.IDLE.getValue();
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setNickname(String nick) {
        this.state.username = nick;
    }

    public String getNickname() {
        return this.state.username;
    }

    public PlayerState getState() {
        return state;
    }

    public int getDbUserId() {
        return dbUserId;
    }

    public float getGroundHeight() {
        return groundHeight;
    }

    public void setDbUserId(int id) {
        this.dbUserId = id;
        this.state.dbUserId = id;
    }

    public void setGroundHeight(float h) {
        this.groundHeight = h;
    }
}
