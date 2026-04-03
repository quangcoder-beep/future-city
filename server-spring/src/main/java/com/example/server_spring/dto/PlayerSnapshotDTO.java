package com.example.server_spring.dto;

import com.example.server_spring.entity.ServerPlayer;

/**
 * Data Transfer Object (DTO) để cô lập dữ liệu người chơi cho Admin Dashboard.
 * Ngăn chặn lỗi NullPointerException và ConcurrentModificationException khi hiển thị Web.
 */
public class PlayerSnapshotDTO {
    private final int id;
    private final String username;
    private final Integer dbUserId;
    private final String ip;
    private final float x;
    private final float z;
    private final int credits;
    private final boolean hasPosition;

    public PlayerSnapshotDTO(ServerPlayer p) {
        this.id = p.getId();
        this.username = (p.getUsername() != null) ? p.getUsername() : "Unknown";
        this.dbUserId = p.getDbUserId();
        
        // Trích xuất IP an toàn
        String tempIp = "Connecting...";
        try {
            if (p.getConnection() != null && p.getConnection().getRemoteAddressTCP() != null) {
                tempIp = p.getConnection().getRemoteAddressTCP().getAddress().getHostAddress();
            }
        } catch (Exception e) {
            tempIp = "Unknown";
        }
        this.ip = tempIp;

        // Trích xuất trạng thái (State) an toàn
        float tempX = 0, tempZ = 0;
        boolean tempHasPos = false;
        int tempCredits = 0;

        if (p.getState() != null) {
            tempCredits = p.getCredits();
            if (p.getState().position != null) {
                tempX = p.getState().position.x;
                tempZ = p.getState().position.z;
                tempHasPos = java.lang.Float.isFinite(tempX) && java.lang.Float.isFinite(tempZ);
            }
        }
        
        this.x = tempX;
        this.z = tempZ;
        this.credits = tempCredits;
        this.hasPosition = tempHasPos;
    }

    // Getters cho Thymeleaf truy cập
    public int getId() { return id; }
    public String getUsername() { return username; }
    public Integer getDbUserId() { return dbUserId; }
    public String getIp() { return ip; }
    public float getX() { return x; }
    public float getZ() { return z; }
    public int getCredits() { return credits; }
    public boolean isHasPosition() { return hasPosition; }
}
