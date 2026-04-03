package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import java.util.List;

/**
 * Quáº£n lĂ½ báº£ng Players â€” vá»‹ trĂ­, tiá»n tá»‡, cáº¥p Ä‘á»™ ngÆ°á»i chÆ¡i.
 */
@Repository
@DependsOn("userRepository")
public class PlayerRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public static class PlayerData {
        public float posX, posY, posZ;
        public int coins, level;
        public int deliveries;
        public float reputation;
    }

    @PostConstruct
    public void initTables() {
        // Create table
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Players' AND xtype='U') " +
                        "CREATE TABLE Players (UserID INT PRIMARY KEY FOREIGN KEY REFERENCES Users(UserID), " +
                        "PosX FLOAT DEFAULT 560, PosY FLOAT DEFAULT 1.0, PosZ FLOAT DEFAULT 0, " +
                        "Coins INT DEFAULT 0, UserLevel INT DEFAULT 1)");

        // Migration: Add new columns if missing
        addColumnIfMissing("Players", "TotalDeliveries", "INT DEFAULT 0");
        addColumnIfMissing("Players", "Reputation", "FLOAT DEFAULT 100.0");

        // 3. --- EMERGENCY ENFORCEMENT v3.8.3 ---
        // Ép toàn bộ người chơi cũ đang bị lơ lửng tại 50m quay về mặt đất (1.0m)
        jdbc.execute("UPDATE Players SET PosY = 1.0 WHERE PosY = 50.0 OR PosY IS NULL");
        
        System.out.println("[DB] PlayerRepository: Table ready and floating fix enforced.");
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnType) {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?) " +
                     "BEGIN " +
                     "    ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType + "; " +
                     "END";
        jdbc.update(sql, tableName, columnName);
    }

    public PlayerData getPlayerData(int userId) {
        try {
            List<PlayerData> r = jdbc.query(
                    "SELECT PosX, PosY, PosZ, Coins, UserLevel, TotalDeliveries, Reputation FROM Players WHERE UserID = ?",
                    (rs, n) -> {
                        PlayerData d = new PlayerData();
                        d.posX = rs.getFloat("PosX");
                        d.posY = rs.getFloat("PosY");
                        d.posZ = rs.getFloat("PosZ");
                        d.coins = rs.getInt("Coins");
                        d.level = rs.getInt("UserLevel");
                        d.deliveries = rs.getInt("TotalDeliveries");
                        d.reputation = rs.getFloat("Reputation");
                        return d;
                    }, userId);
            return r.isEmpty() ? null : r.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void savePlayerPosition(int userId, float x, float y, float z) {
        jdbc.update("UPDATE Players SET PosX=?, PosY=?, PosZ=? WHERE UserID=?", x, y, z, userId);
    }

    public int getPlayerCoins(int userId) {
        try {
            int coins = jdbc.queryForObject("SELECT Coins FROM Players WHERE UserID=?", Integer.class, userId);
            return coins;
        } catch (Exception e) {
            System.err.println("DB Read Error for Player #" + userId + ": " + e.getMessage());
            return 0;
        }
    }

    public void updateCoins(int userId, int newCoins) {
        jdbc.update("UPDATE Players SET Coins=? WHERE UserID=?", newCoins, userId);
    }

    public void updateStats(int userId, int deliveries, float reputation) {
        jdbc.update("UPDATE Players SET TotalDeliveries=?, Reputation=? WHERE UserID=?", deliveries, reputation, userId);
    }
}
