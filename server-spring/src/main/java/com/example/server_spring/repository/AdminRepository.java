package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * Repository cho các nghiệp vụ Quản trị (Admin) và Bảo mật (Security).
 * Sử dụng cú pháp T-SQL cho Microsoft SQL Server.
 */
@Repository
public class AdminRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void initTables() {
        // 1. Bảng AdminLogs - Lưu vết Audit Trail
        jdbc.execute("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AdminLogs') " +
                "CREATE TABLE AdminLogs (" +
                "LogID INT PRIMARY KEY IDENTITY, " +
                "AdminName NVARCHAR(50) NOT NULL, " +
                "ActionType VARCHAR(30) NOT NULL, " +
                "TargetID INT NULL, " +
                "Description NVARCHAR(MAX), " +
                "CreatedAt DATETIME DEFAULT GETDATE())");

        // 2. Bảng PlayerBans - Danh sách đen (Ban IP/UID)
        jdbc.execute("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PlayerBans') " +
                "CREATE TABLE PlayerBans (" +
                "BanID INT PRIMARY KEY IDENTITY, " +
                "UserID INT NULL, " +
                "IPAddress VARCHAR(45) NULL, " +
                "Reason NVARCHAR(MAX) NOT NULL, " +
                "BannedBy NVARCHAR(50) NOT NULL, " +
                "BanDuration INT NULL, " + // Thời gian ban (phút)
                "IsActive BIT DEFAULT 1, " + // Trạng thái: 1=Bị Ban, 0=Hủy Ban (Ân xá)
                "ExpiresAt DATETIME NULL, " +
                "CreatedAt DATETIME DEFAULT GETDATE())");

        // Đảm bảo cột mới tồn tại nếu bảng đã được tạo trước đó
        addColumnIfMissing("PlayerBans", "BanDuration", "INT NULL");
        addColumnIfMissing("PlayerBans", "IsActive", "BIT DEFAULT 1");

        // 3. Bảng PlayerWarnings - Thẻ phạt
        jdbc.execute("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PlayerWarnings') " +
                "CREATE TABLE PlayerWarnings (" +
                "WarningID INT PRIMARY KEY IDENTITY, " +
                "UserID INT NOT NULL, " +
                "WarningLevel INT DEFAULT 1, " +
                "Reason NVARCHAR(MAX) NOT NULL, " +
                "WarnedBy NVARCHAR(50) NOT NULL, " +
                "CreatedAt DATETIME DEFAULT GETDATE())");

        System.out.println("[DB] AdminRepository: Admin tables ready (T-SQL).");
    }

    // --- Audit Logging ---
    public void logAction(String admin, String type, Integer targetId, String desc) {
        jdbc.update("INSERT INTO AdminLogs (AdminName, ActionType, TargetID, Description) VALUES (?, ?, ?, ?)",
                admin, type, targetId, desc);
    }

    public List<Map<String, Object>> getRecentLogs(int limit) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT TOP (?) * FROM AdminLogs ORDER BY CreatedAt DESC", limit);
        return normalizeKeys(rows);
    }

    public void clearLogs() {
        jdbc.execute("TRUNCATE TABLE AdminLogs");
    }

    // --- Ban Management ---
    public boolean isBanned(int userId, String ip) {
        // --- EMERGENCY FIX UNLOCK DEV ---
        // Chỉ chặn theo UserID để dev có thể test nhiều tài khoản trên cùng 1 máy
        if (userId <= 0) {
            return false;
        }

        // Dùng GETUTCDATE() để chống lệch múi giờ
        String sql = "SELECT COUNT(*) FROM PlayerBans WHERE " +
                     "UserID = ? AND IsActive = 1 AND (ExpiresAt IS NULL OR ExpiresAt > GETUTCDATE())";
        
        try {
            Integer count = jdbc.queryForObject(sql, Integer.class, userId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String getBanReason(int userId, String ip) {
        String sql = "SELECT TOP 1 Reason, ExpiresAt FROM PlayerBans WHERE UserID = ? AND IsActive = 1 AND (ExpiresAt IS NULL OR ExpiresAt > GETUTCDATE()) ORDER BY CreatedAt DESC";
        try {
            Map<String, Object> result = jdbc.queryForMap(sql, userId);
            String reason = (String) result.get("Reason") != null ? (String) result.get("Reason") : "Administrative Action";
            java.sql.Timestamp expires = (java.sql.Timestamp) result.get("ExpiresAt");
            
            if (expires == null) return reason + " (Vĩnh viễn)";
            String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(expires);
            return reason + " (Đến: " + dateStr + ")";
        } catch (Exception e) {
            return "Lý do không xác định";
        }
    }

    public void banPlayer(Integer userId, String ip, String reason, String admin, Integer durationMinutes) {
        // Dùng GETUTCDATE() để tạo thời điểm Ban mẫu mực
        String expiresAtExpr = (durationMinutes == null || durationMinutes <= 0) ? "NULL" : "DATEADD(minute, ?, GETUTCDATE())";
        String sql = "INSERT INTO PlayerBans (UserID, IPAddress, Reason, BannedBy, BanDuration, IsActive, ExpiresAt) VALUES (?, ?, ?, ?, ?, 1, " + expiresAtExpr + ")";
        
        try {
            if (durationMinutes == null || durationMinutes <= 0) {
                jdbc.update(sql, userId, ip, reason, admin, 0); // 0 = Permanent duration log
            } else {
                jdbc.update(sql, userId, ip, reason, admin, durationMinutes, durationMinutes);
            }
        } catch (Exception e) {
            System.err.println("[DB-ERROR] Failed to ban player " + userId + ": " + e.getMessage());
            throw e;
        }
    }

    public void unbanPlayer(int banId) {
        jdbc.update("UPDATE PlayerBans SET IsActive = 0 WHERE BanID = ?", banId);
    }

    public List<Map<String, Object>> getActiveBans() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM PlayerBans WHERE IsActive = 1 AND (ExpiresAt IS NULL OR ExpiresAt > GETDATE()) ORDER BY CreatedAt DESC");
        return normalizeKeys(rows);
    }

    // --- Warning Management ---
    public void warnPlayer(int userId, int level, String reason, String admin) {
        jdbc.update("INSERT INTO PlayerWarnings (UserID, WarningLevel, Reason, WarnedBy) VALUES (?, ?, ?, ?)",
                userId, level, reason, admin);
    }

    public int getWarningCount(int userId) {
        Integer count = jdbc.queryForObject("SELECT SUM(WarningLevel) FROM PlayerWarnings WHERE UserID = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> getRecentWarnings(int limit) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT TOP (?) * FROM PlayerWarnings ORDER BY CreatedAt DESC", limit);
        return normalizeKeys(rows);
    }

    /**
     * Utility method to ensure all keys in the Map are lowercase.
     * This avoids case-sensitivity issues in Thymeleaf templates (e.g. CreatedAt vs createdat).
     */
    private List<Map<String, Object>> normalizeKeys(List<Map<String, Object>> rows) {
        List<Map<String, Object>> normalizedRows = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalizedRow = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                normalizedRow.put(entry.getKey().toLowerCase(), entry.getValue());
            }
            normalizedRows.add(normalizedRow);
        }
        return normalizedRows;
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnType) {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?) " +
                     "BEGIN " +
                     "    ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType + "; " +
                     "END";
        jdbc.update(sql, tableName, columnName);
    }
}
