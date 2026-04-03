package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import java.util.List;

/**
 * Quản lý bảng PlayerInventory — túi đồ của người chơi.
 */
@Repository
@DependsOn({ "userRepository", "itemRepository" })
public class InventoryRepository {

    @Autowired
    private JdbcTemplate jdbc;

    public static class PlayerItem {
        public int itemId;
        public String itemName;
        public int quantity;
    }

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='PlayerInventory' AND xtype='U') " +
                        "CREATE TABLE PlayerInventory (UserID INT REFERENCES Users(UserID), " +
                        "ItemID INT REFERENCES Items(ItemID), Quantity INT DEFAULT 1, " +
                        "PRIMARY KEY (UserID, ItemID))");
        System.out.println("[DB] InventoryRepository: Table ready.");
    }

    public List<PlayerItem> getPlayerInventory(int userId) {
        try {
            return jdbc.query(
                    "SELECT pi.ItemID, i.ItemName, pi.Quantity " +
                            "FROM PlayerInventory pi " +
                            "JOIN Items i ON pi.ItemID = i.ItemID " +
                            "WHERE pi.UserID = ?",
                    (rs, n) -> {
                        PlayerItem item = new PlayerItem();
                        item.itemId = rs.getInt("ItemID");
                        item.itemName = rs.getString("ItemName");
                        item.quantity = rs.getInt("Quantity");
                        return item;
                    }, userId);
        } catch (Exception e) {
            // System.err.println("[DB] getPlayerInventory error: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Thêm item vào túi (hoặc cộng thêm số lượng nếu đã có).
     */
    public void addItem(int userId, int itemId, int quantity) {
        jdbc.update(
                "MERGE PlayerInventory AS t USING (SELECT ? AS U, ? AS I) AS s " +
                        "ON t.UserID=s.U AND t.ItemID=s.I " +
                        "WHEN MATCHED THEN UPDATE SET Quantity=t.Quantity+? " +
                        "WHEN NOT MATCHED THEN INSERT (UserID,ItemID,Quantity) VALUES(s.U,s.I,?);",
                userId, itemId, quantity, quantity);
    }
}
