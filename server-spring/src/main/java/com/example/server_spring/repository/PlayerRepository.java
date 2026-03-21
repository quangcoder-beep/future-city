package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Quáº£n lĂ½ báº£ng Players â€” vá»‹ trĂ­, tiá»n tá»‡, cáº¥p Ä‘á»™ ngÆ°á»i chÆ¡i.
 */
@Repository
public class PlayerRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public static class PlayerData {
        public float posX, posY, posZ;
        public int coins, level;
    }

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Players' AND xtype='U') " +
                        "CREATE TABLE Players (UserID INT PRIMARY KEY FOREIGN KEY REFERENCES Users(UserID), " +
                        "PosX FLOAT DEFAULT 560, PosY FLOAT DEFAULT 50, PosZ FLOAT DEFAULT 0, " +
                        "Coins INT DEFAULT 0, UserLevel INT DEFAULT 1)");
        System.out.println("[DB] PlayerRepository: Table ready.");
    }

    public PlayerData getPlayerData(int userId) {
        try {
            List<PlayerData> r = jdbc.query(
                    "SELECT PosX, PosY, PosZ, Coins, UserLevel FROM Players WHERE UserID = ?",
                    (rs, n) -> {
                        PlayerData d = new PlayerData();
                        d.posX = rs.getFloat("PosX");
                        d.posY = rs.getFloat("PosY");
                        d.posZ = rs.getFloat("PosZ");
                        d.coins = rs.getInt("Coins");
                        d.level = rs.getInt("UserLevel");
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
}
