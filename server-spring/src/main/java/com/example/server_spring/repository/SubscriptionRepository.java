package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Quáº£n lĂ½ báº£ng ShopSubscribers â€” Ä‘Äƒng kĂ½ nháº­n thĂ´ng bĂ¡o Ä‘Æ¡n hĂ ng má»›i.
 */
@Repository
public class SubscriptionRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ShopSubscribers' AND xtype='U') " +
                        "CREATE TABLE ShopSubscribers (ShopID NVARCHAR(50) NOT NULL REFERENCES Shops(ShopID), " +
                        "UserID INT REFERENCES Users(UserID), " +
                        "SubscribedAt DATETIME DEFAULT GETDATE(), PRIMARY KEY (ShopID, UserID))");
        System.out.println("[DB] SubscriptionRepository: Table ready.");
    }

    public boolean subscribeToShop(int userId, String shopId) {
        try {
            jdbc.update(
                    "MERGE ShopSubscribers AS t USING (SELECT ? AS S, ? AS U) AS s " +
                            "ON t.ShopID=s.S AND t.UserID=s.U " +
                            "WHEN NOT MATCHED THEN INSERT (ShopID,UserID) VALUES(s.S,s.U);",
                    shopId, userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean unsubscribeFromShop(int userId, String shopId) {
        try {
            jdbc.update("DELETE FROM ShopSubscribers WHERE ShopID=? AND UserID=?", shopId, userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPlayerSubscribed(int userId, String shopId) {
        try {
            Integer c = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM ShopSubscribers WHERE ShopID=? AND UserID=?",
                    Integer.class, shopId, userId);
            return c != null && c > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Integer> getShopSubscribers(String shopId) {
        return jdbc.query("SELECT UserID FROM ShopSubscribers WHERE ShopID=?",
                (rs, n) -> rs.getInt("UserID"), shopId);
    }
}
