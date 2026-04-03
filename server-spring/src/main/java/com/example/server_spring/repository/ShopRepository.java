package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import java.util.List;

/**
 * Quáº£n lĂ½ báº£ng Shops + ShopItems â€” cá»­a hĂ ng vĂ  kho hĂ ng.
 */
@Repository
@DependsOn("itemRepository")
public class ShopRepository {

    @Autowired
    private JdbcTemplate jdbc;

    // ===== Data class =====
    public static class ShopItemData {
        public String shopId;
        public int itemId;
        public String itemName;
        public int price;
        public int currentStock;
    }

    @PostConstruct
    public void initTables() {
        try {
            jdbc.execute(
                    "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Shops' AND xtype='U') " +
                            "CREATE TABLE Shops (ShopID NVARCHAR(50) PRIMARY KEY, " +
                            "ShopName NVARCHAR(100) NOT NULL, LocationX FLOAT, LocationY FLOAT, LocationZ FLOAT)");
            jdbc.execute(
                    "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ShopItems' AND xtype='U') " +
                            "CREATE TABLE ShopItems (ShopID NVARCHAR(50) NOT NULL REFERENCES Shops(ShopID), " +
                            "ItemID INT REFERENCES Items(ItemID), " +
                            "Price INT NOT NULL, MinStock INT DEFAULT 5, MaxStock INT DEFAULT 20, " +
                            "CurrentStock INT DEFAULT 10, LastRestock DATETIME DEFAULT GETDATE(), " +
                            "PRIMARY KEY (ShopID, ItemID))");
            System.out.println("[DB] ShopRepository: Tables ready.");
        } catch (Exception e) {
            System.err.println("[DB] Error initializing tables in ShopRepository: " + e.getMessage());
        }
    }

    public void upsertShop(String shopId, String name, float x, float y, float z) {
        jdbc.update(
                "MERGE Shops AS t USING (SELECT ? AS id, ? AS n, ? AS x, ? AS y, ? AS z) AS s " +
                        "ON t.ShopID = s.id " +
                        "WHEN MATCHED THEN UPDATE SET ShopName=s.n, LocationX=s.x, LocationY=s.y, LocationZ=s.z " +
                        "WHEN NOT MATCHED THEN INSERT (ShopID, ShopName, LocationX, LocationY, LocationZ) " +
                        "VALUES(s.id, s.n, s.x, s.y, s.z);",
                shopId, name, x, y, z);
    }

    public void restockIfNewDay(String shopId) {
        jdbc.update(
                "UPDATE ShopItems SET CurrentStock = FLOOR(RAND()*(MaxStock-MinStock+1))+MinStock, " +
                        "LastRestock = GETDATE() WHERE ShopID=? AND CAST(LastRestock AS DATE) < CAST(GETDATE() AS DATE)",
                shopId);
    }

    public List<ShopItemData> getShopItems(String shopId) {
        restockIfNewDay(shopId);
        return jdbc.query(
                "SELECT si.ItemID, i.ItemName, si.Price, si.CurrentStock " +
                        "FROM ShopItems si JOIN Items i ON si.ItemID=i.ItemID WHERE si.ShopID=?",
                (rs, n) -> {
                    ShopItemData d = new ShopItemData();
                    d.shopId = shopId;
                    d.itemId = rs.getInt("ItemID");
                    d.itemName = rs.getString("ItemName");
                    d.price = rs.getInt("Price");
                    d.currentStock = rs.getInt("CurrentStock");
                    return d;
                }, shopId);
    }

    public List<ShopItemData> getAllShopItems() {
        return jdbc.query("SELECT si.ShopID, si.ItemID, i.ItemName, si.Price, si.CurrentStock " +
                "FROM ShopItems si JOIN Items i ON si.ItemID=i.ItemID " +
                "WHERE si.CurrentStock > 0", (rs, n) -> {
                    ShopItemData d = new ShopItemData();
                    d.shopId = rs.getString("ShopID");
                    d.itemId = rs.getInt("ItemID");
                    d.itemName = rs.getString("ItemName");
                    d.price = rs.getInt("Price");
                    d.currentStock = rs.getInt("CurrentStock");
                    return d;
                });
    }

    /**
     * Mua váº­t pháº©m: trá»« stock, trá»« coin, thĂªm vĂ o inventory.
     * 
     * @return Sá»‘ coin cĂ²n láº¡i, hoáº·c -1 (háº¿t hĂ ng), -2 (khĂ´ng Ä‘á»§ coin).
     */
    @Transactional
    public int buyItem(int userId, String shopId, int itemId,
            PlayerRepository playerRepo, InventoryRepository inventoryRepo,
            TransactionRepository transactionRepo) {
        try {
            List<Integer> prices = jdbc.query(
                    "SELECT Price FROM ShopItems WHERE ShopID=? AND ItemID=? AND CurrentStock > 0",
                    (rs, n) -> rs.getInt("Price"), shopId, itemId);
            if (prices.isEmpty()) {
                return -1;
            }
            int price = prices.get(0);

            int coins = playerRepo.getPlayerCoins(userId);
            if (coins < price) {
                return -2;
            }

            int remaining = coins - price;
            playerRepo.updateCoins(userId, remaining);
            jdbc.update("UPDATE ShopItems SET CurrentStock=CurrentStock-1 WHERE ShopID=? AND ItemID=?", shopId, itemId);
            inventoryRepo.addItem(userId, itemId, 1);
            transactionRepo.logTransaction(userId, "BUY", -price, remaining, "Mua ItemID=" + itemId);
            return remaining;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void updateStock(String shopId, int itemId, int delta) {
        jdbc.update("UPDATE ShopItems SET CurrentStock = CurrentStock + ? WHERE ShopID=? AND ItemID=?", delta, shopId, itemId);
    }
}
