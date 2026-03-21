package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.context.annotation.DependsOn;

/**
 * Manages Order data in the Database.
 */
@Repository
@DependsOn("shopRepository")
public class OrderRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlayerRepository playerRepo;

    @Autowired
    private TransactionRepository transactionRepo;

    public static class OrderData {
        public int orderId, itemId, reward, itemPrice, timeoutMinutes, courierId;
        public String buyerType, buyerRefId, itemName, shopId, shopName, status, destinationName;
        public float destX, destY, destZ;
        public float shopX, shopY, shopZ;
        public java.sql.Timestamp acceptedAt;
    }

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Orders' AND xtype='U') " +
                        "CREATE TABLE Orders (OrderID INT PRIMARY KEY IDENTITY(1,1), " +
                        "BuyerType NVARCHAR(10) NOT NULL, BuyerRefID NVARCHAR(50) NOT NULL, " +
                        "ItemID INT REFERENCES Items(ItemID), " +
                        "ShopID NVARCHAR(50) NOT NULL REFERENCES Shops(ShopID), Status NVARCHAR(20) DEFAULT 'PENDING', "
                        +
                        "CourierID INT NULL REFERENCES Users(UserID), " +
                        "DestX FLOAT, DestY FLOAT, DestZ FLOAT, " +
                        "Reward INT NOT NULL, ItemPrice INT NOT NULL, " +
                        "CreatedAt DATETIME DEFAULT GETDATE(), AcceptedAt DATETIME NULL, " +
                        "TimeoutMinutes INT DEFAULT 15)");
        System.out.println("[DB] OrderRepository: Tables ready.");
    }

    public int createOrder(String shopId, int itemId, int itemPrice,
            String buyerType, String buyerRefId,
            int reward, float destX, float destY, float destZ) {
        KeyHolder kh = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Orders (ShopID,ItemID,ItemPrice,BuyerType,BuyerRefID," +
                                "Reward,DestX,DestY,DestZ,Status,CreatedAt) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,'PENDING',GETDATE())",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, shopId);
                ps.setInt(2, itemId);
                ps.setInt(3, itemPrice);
                ps.setString(4, buyerType);
                ps.setString(5, buyerRefId);
                ps.setInt(6, reward);
                ps.setFloat(7, destX);
                ps.setFloat(8, destY);
                ps.setFloat(9, destZ);
                return ps;
            }, kh);
            return kh.getKey() != null ? kh.getKey().intValue() : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<OrderData> getPendingOrders(String shopId) {
        return jdbc.query(
                "SELECT o.OrderID, o.BuyerType, o.BuyerRefID, o.ItemID, i.ItemName, " +
                        "o.ShopID, s.ShopName, o.Reward, o.ItemPrice, o.DestX, o.DestY, o.DestZ, o.Status " +
                        "FROM Orders o " +
                        "JOIN Items i ON o.ItemID=i.ItemID " +
                        "LEFT JOIN Shops s ON o.ShopID=s.ShopID " +
                        "WHERE o.ShopID=? AND o.Status='PENDING'",
                (rs, n) -> {
                    OrderData od = mapOrderData(rs);
                    od.shopName = rs.getString("ShopName");
                    if (od.shopName == null) od.shopName = od.shopId;
                    return od;
                }, shopId);
    }

    public boolean acceptDelivery(int courierId, int orderId) {
        try {
            List<Integer> activeCount = jdbc.query(
                    "SELECT COUNT(*) FROM Orders WHERE CourierID=? AND Status IN ('PENDING_PICKUP', 'DELIVERING')",
                    (rs, n) -> rs.getInt(1), courierId);
            if (!activeCount.isEmpty() && activeCount.get(0) > 0) return false;

            int updated = jdbc.update(
                    "UPDATE Orders SET CourierID=?, Status='PENDING_PICKUP', AcceptedAt=GETDATE() " +
                            "WHERE OrderID=? AND Status='PENDING'",
                    courierId, orderId);
            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int completeDelivery(int courierId, int orderId) {
        try {
            List<OrderData> orders = jdbc.query(
                    "SELECT Reward, ItemPrice, BuyerType, BuyerRefID, ItemID FROM Orders " +
                            "WHERE OrderID=? AND CourierID=? AND Status='DELIVERING'",
                    (rs, n) -> {
                        OrderData od = new OrderData();
                        od.reward = rs.getInt("Reward");
                        od.itemPrice = rs.getInt("ItemPrice");
                        od.buyerType = rs.getString("BuyerType");
                        od.buyerRefId = rs.getString("BuyerRefID");
                        od.itemId = rs.getInt("ItemID");
                        return od;
                    }, orderId, courierId);
            if (orders.isEmpty()) return -1;
            OrderData od = orders.get(0);

            int courierBalance = playerRepo.getPlayerCoins(courierId);
            playerRepo.updateCoins(courierId, courierBalance + od.reward);
            int newBal = playerRepo.getPlayerCoins(courierId);
            transactionRepo.logTransaction(courierId, "DELIVERY_REWARD", od.reward, newBal, "Delivery Reward - Order #" + orderId);
            System.out.println("[DB] COURIER #" + courierId + " RECEIVED REWARD: " + od.reward + ". New Balance: " + newBal);

            if ("PLAYER".equals(od.buyerType)) {
                int buyerId = Integer.parseInt(od.buyerRefId);
                jdbc.update(
                        "MERGE PlayerInventory AS t USING (SELECT ? AS U, ? AS I) AS s " +
                                "ON t.UserID=s.U AND t.ItemID=s.I " +
                                "WHEN MATCHED THEN UPDATE SET Quantity=t.Quantity+1 " +
                                "WHEN NOT MATCHED THEN INSERT (UserID,ItemID,Quantity) VALUES(s.U,s.I,1);",
                        buyerId, od.itemId);
                System.out.println("[DB] Buyer #" + buyerId + " inventory updated (Order completed).");
            }
            jdbc.update("UPDATE Orders SET Status='COMPLETED' WHERE OrderID=?", orderId);
            return od.reward;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean compensateShipper(int orderId, int amount) {
        try {
            int courierId = getCourierId(orderId);
            OrderData od = getOrderData(orderId);
            if (courierId <= 0 || od == null || !"PLAYER".equals(od.buyerType)) return false;
            int buyerId = Integer.parseInt(od.buyerRefId);

            jdbc.update("UPDATE Players SET Coins = CASE WHEN Coins >= ? THEN Coins - ? ELSE 0 END WHERE UserID = ?",
                    amount, amount, buyerId);
            jdbc.update("UPDATE Players SET Coins = Coins + ? WHERE UserID = ?", amount, courierId);

            int courierBal = playerRepo.getPlayerCoins(courierId);
            transactionRepo.logTransaction(courierId, "DELIVERY_COMPENSATION", amount, courierBal,
                    "Compensation - Order #" + orderId);

            int buyerBal = playerRepo.getPlayerCoins(buyerId);
            transactionRepo.logTransaction(buyerId, "CANCEL_PENALTY", -amount, buyerBal,
                    "Cancellation Penalty - Order #" + orderId);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int cancelDelivery(int courierId, int orderId) {
        try {
            List<Integer> rewards = jdbc.query(
                    "SELECT Reward FROM Orders WHERE OrderID=? AND CourierID=? AND Status IN ('PENDING_PICKUP', 'DELIVERING')",
                    (rs, n) -> rs.getInt("Reward"), orderId, courierId);
            if (rewards.isEmpty()) return -1;
            int penalty = (int) (rewards.get(0) * 0.10);
            jdbc.update("UPDATE Players SET Coins=Coins-? WHERE UserID=?", penalty, courierId);
            int newBal = playerRepo.getPlayerCoins(courierId);
            transactionRepo.logTransaction(courierId, "PENALTY", -penalty, newBal, "Cancellation Penalty - Order #" + orderId);

            OrderData od = getOrderData(orderId);
            if (od != null) {
                jdbc.update("UPDATE ShopItems SET CurrentStock=CurrentStock+1 WHERE ShopID=? AND ItemID=?", od.shopId, od.itemId);
            }
            jdbc.update("UPDATE Orders SET CourierID=NULL, Status='PENDING', AcceptedAt=NULL WHERE OrderID=?", orderId);
            return penalty;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void checkTimeouts() {
        try {
            List<OrderData> timedOut = jdbc.query(
                    "SELECT OrderID, CourierID, Reward, ShopID, ItemID FROM Orders " +
                            "WHERE Status IN ('PENDING_PICKUP', 'DELIVERING') AND DATEADD(MINUTE, TimeoutMinutes, AcceptedAt) < GETDATE()",
                    (rs, n) -> {
                        OrderData od = new OrderData();
                        od.orderId = rs.getInt("OrderID");
                        od.shopId = rs.getString("ShopID");
                        od.reward = rs.getInt("Reward");
                        od.itemId = rs.getInt("ItemID");
                        od.buyerRefId = String.valueOf(rs.getInt("CourierID"));
                        return od;
                    });
            for (OrderData od : timedOut) {
                int courierId = Integer.parseInt(od.buyerRefId);
                int penalty = (int) (od.reward * 0.20);
                jdbc.update("UPDATE Players SET Coins=Coins-? WHERE UserID=?", penalty, courierId);
                int newBal = playerRepo.getPlayerCoins(courierId);
                transactionRepo.logTransaction(courierId, "PENALTY", -penalty, newBal, "Timeout Penalty - Order #" + od.orderId);
                jdbc.update("UPDATE ShopItems SET CurrentStock=CurrentStock+1 WHERE ShopID=? AND ItemID=?", od.shopId, od.itemId);
                jdbc.update("UPDATE Orders SET Status='TIMEOUT', CourierID=NULL WHERE OrderID=?", od.orderId);
                System.out.println("[DB] Order #" + od.orderId + " TIMEOUT! Courier " + courierId + " penalized " + penalty);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<OrderData> getActiveDeliveries(int courierId) {
        try {
            return jdbc.query(
                    "SELECT o.OrderID, o.BuyerType, o.BuyerRefID, o.ItemID, i.ItemName, " +
                            "o.ShopID, s.ShopName, o.Reward, o.ItemPrice, o.DestX, o.DestY, o.DestZ, o.Status, o.AcceptedAt, o.TimeoutMinutes, " +
                            "s.LocationX, s.LocationY, s.LocationZ "
                            +
                            "FROM Orders o " +
                            "JOIN Items i ON o.ItemID=i.ItemID " +
                            "LEFT JOIN Shops s ON o.ShopID=s.ShopID " +
                            "WHERE o.CourierID=? AND o.Status IN ('PENDING_PICKUP', 'DELIVERING')",
                    (rs, n) -> {
                        OrderData od = mapOrderData(rs);
                        od.acceptedAt = rs.getTimestamp("AcceptedAt");
                        od.timeoutMinutes = rs.getInt("TimeoutMinutes");
                        od.shopName = rs.getString("ShopName");
                        od.shopX = rs.getFloat("LocationX");
                        od.shopY = rs.getFloat("LocationY");
                        od.shopZ = rs.getFloat("LocationZ");
                        if (od.shopName == null) od.shopName = od.shopId;
                        return od;
                    }, courierId);
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Tối ưu N+1: Lấy tất cả các đơn hàng đang giao trong 1 query duy nhất.
     */
    public List<OrderData> getAllActiveDeliveries() {
        try {
            return jdbc.query(
                    "SELECT o.OrderID, o.BuyerType, o.BuyerRefID, o.CourierID, o.ItemID, i.ItemName, " +
                            "o.ShopID, s.ShopName, o.Reward, o.ItemPrice, o.DestX, o.DestY, o.DestZ, o.Status " +
                            "FROM Orders o " +
                            "JOIN Items i ON o.ItemID=i.ItemID " +
                            "LEFT JOIN Shops s ON o.ShopID=s.ShopID " +
                            "WHERE o.Status='DELIVERING' AND o.CourierID IS NOT NULL",
                    (rs, n) -> {
                        OrderData od = mapOrderData(rs);
                        od.shopName = rs.getString("ShopName");
                        od.courierId = rs.getInt("CourierID"); // Properly populate courierId
                        return od;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    public List<OrderData> getAllPendingOrders() {
        try {
            return jdbc.query(
                    "SELECT o.OrderID, o.BuyerType, o.BuyerRefID, o.ItemID, i.ItemName, " +
                            "o.ShopID, s.ShopName, o.Reward, o.ItemPrice, o.DestX, o.DestY, o.DestZ, o.Status, s.LocationX, s.LocationY, s.LocationZ "
                            +
                            "FROM Orders o " +
                            "JOIN Items i ON o.ItemID=i.ItemID " +
                            "LEFT JOIN Shops s ON o.ShopID=s.ShopID " +
                            "WHERE o.Status='PENDING'",
                    (rs, n) -> {
                        OrderData od = mapOrderData(rs);
                        od.shopName = rs.getString("ShopName");
                        od.shopX = rs.getFloat("LocationX");
                        od.shopY = rs.getFloat("LocationY");
                        od.shopZ = rs.getFloat("LocationZ");
                        if (od.shopName == null) od.shopName = od.shopId;
                        return od;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    public boolean updateOrderStatus(int orderId, String status) {
        try {
            int updated = jdbc.update("UPDATE Orders SET Status=? WHERE OrderID=?", status, orderId);
            if (updated > 0) System.out.println("[DB] Updated Order Status #" + orderId + " -> " + status);
            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean pickupDelivery(int orderId) {
        try {
            int updated = jdbc.update(
                    "UPDATE Orders SET Status='DELIVERING', AcceptedAt=GETDATE() WHERE OrderID=? AND Status='PENDING_PICKUP'",
                    orderId);
            if (updated > 0) System.out.println("[DB] COURIER PICKED UP ITEMS. Order #" + orderId + " changed to DELIVERING.");
            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public OrderData getOrderData(int orderId) {
        try {
            List<OrderData> r = jdbc.query(
                    "SELECT o.OrderID, o.BuyerType, o.BuyerRefID, o.ItemID, i.ItemName, " +
                            "o.ShopID, o.Reward, o.ItemPrice, o.DestX, o.DestY, o.DestZ, o.Status, s.LocationX, s.LocationY, s.LocationZ "
                            +
                            "FROM Orders o JOIN Items i ON o.ItemID=i.ItemID JOIN Shops s ON o.ShopID=s.ShopID " +
                            "WHERE o.OrderID=?",
                    (rs, n) -> {
                        OrderData od = mapOrderData(rs);
                        od.shopX = rs.getFloat("LocationX");
                        od.shopY = rs.getFloat("LocationY");
                        od.shopZ = rs.getFloat("LocationZ");
                        return od;
                    }, orderId);
            return r.isEmpty() ? null : r.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<OrderData> getBuyerOrders(int buyerId) {
        try {
            return jdbc.query(
                    "SELECT o.OrderID, o.BuyerType, o.BuyerRefID, o.ItemID, i.ItemName, " +
                            "o.ShopID, o.Reward, o.ItemPrice, o.DestX, o.DestY, o.DestZ, o.Status " +
                            "FROM Orders o JOIN Items i ON o.ItemID=i.ItemID " +
                            "WHERE o.BuyerType='PLAYER' AND o.BuyerRefID=? AND o.Status IN ('PENDING', 'PENDING_PICKUP', 'DELIVERING')",
                    (rs, n) -> {
                        OrderData od = mapOrderData(rs);
                        return od;
                    }, String.valueOf(buyerId));
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    private OrderData mapOrderData(java.sql.ResultSet rs) throws java.sql.SQLException {
        OrderData od = new OrderData();
        od.orderId = rs.getInt("OrderID");
        od.buyerType = rs.getString("BuyerType");
        od.buyerRefId = rs.getString("BuyerRefID");
        od.itemId = rs.getInt("ItemID");
        od.itemName = rs.getString("ItemName");
        od.shopId = rs.getString("ShopID");
        od.reward = rs.getInt("Reward");
        od.itemPrice = rs.getInt("ItemPrice");
        od.destX = rs.getFloat("DestX");
        od.destY = rs.getFloat("DestY");
        od.destZ = rs.getFloat("DestZ");
        od.status = rs.getString("Status");
        od.destinationName = "Delivery Point (" + (int) od.destX + ", " + (int) od.destZ + ")";
        
        return od;
    }

    public int getCourierId(int orderId) {
        try {
            List<Integer> list = jdbc.query("SELECT CourierID FROM Orders WHERE OrderID=?",
                    (rs, n) -> {
                        int c = rs.getInt("CourierID");
                        return rs.wasNull() ? -1 : c;
                    }, orderId);
            return list.isEmpty() ? -1 : list.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
