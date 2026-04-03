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
import org.springframework.dao.DataIntegrityViolationException;
import com.example.server_spring.exception.UserAlreadyExistsException;

/**
 * Quản lý bảng Users — đăng ký, xác thực, truy vấn thông tin tài khoản.
 */
@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void initTables() {
        // 1. Tạo bảng Users nếu chưa có
        // 1. Khởi tạo bảng cơ bản (Không dùng UNIQUE trực tiếp ở đây để tránh lỗi NULL Collision cho Username)
        jdbc.execute("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users') " +
                "CREATE TABLE Users (UserID INT PRIMARY KEY IDENTITY, Email NVARCHAR(100) UNIQUE, " +
                "Username NVARCHAR(50) NULL, PasswordHash NVARCHAR(MAX) NULL, " +
                "Role NVARCHAR(20) DEFAULT 'PLAYER', " +
                "CreatedAt DATETIME DEFAULT GETDATE())");

        // 2. Migration: Thêm hoặc Cập nhật các cột mới
        addColumnIfMissing("Users", "Role", "NVARCHAR(20) DEFAULT 'PLAYER'");
        addColumnIfMissing("Users", "Nickname", "NVARCHAR(50) NULL");
        addColumnIfMissing("Users", "AvatarURL", "NVARCHAR(MAX) NULL");
        addColumnIfMissing("Users", "GoogleID", "NVARCHAR(100) NULL");
        addColumnIfMissing("Users", "FacebookID", "NVARCHAR(100) NULL");

        // Đảm bảo AvatarURL luôn là NVARCHAR(MAX) kể cả khi cột đã tồn tại từ trước với kích thước nhỏ hơn
        jdbc.execute("ALTER TABLE Users ALTER COLUMN AvatarURL NVARCHAR(MAX) NULL");

        // 3. Gỡ bỏ các UNIQUE constraint cũ (bao gồm cả Username)
        dropUniqueConstraintOnColumn("Users", "Username");
        dropUniqueConstraintOnColumn("Users", "Nickname");
        dropUniqueConstraintOnColumn("Users", "GoogleID");
        dropUniqueConstraintOnColumn("Users", "FacebookID");

        // 4. Tạo Filtered Unique Index (Chỉ ràng buộc UNIQUE với các giá trị KHÁC NULL)
        createFilteredUniqueIndex("Users", "Username");
        createFilteredUniqueIndex("Users", "Nickname");
        createFilteredUniqueIndex("Users", "GoogleID");
        createFilteredUniqueIndex("Users", "FacebookID");

        System.out.println("[DB] UserRepository: Table ready with Social & Nickname support.");
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnType) {
        String sql = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?) " +
                     "BEGIN " +
                     "    ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType + "; " +
                     "END";
        jdbc.update(sql, tableName, columnName);
    }

    private void dropUniqueConstraintOnColumn(String tableName, String columnName) {
        // Tìm tên của UNIQUE constraint gắn với cột này
        String sqlFind = "SELECT name FROM sys.objects WHERE type = 'UQ' AND parent_object_id = OBJECT_ID(?) " +
                         "AND name IN (SELECT i.name FROM sys.indexes i " +
                         "JOIN sys.index_columns ic ON i.index_id = ic.index_id AND i.object_id = ic.object_id " +
                         "JOIN sys.columns c ON ic.column_id = c.column_id AND ic.object_id = c.object_id " +
                         "WHERE i.is_unique_constraint = 1 AND c.name = ?)";
        try {
            java.util.List<String> constraints = jdbc.queryForList(sqlFind, String.class, tableName, columnName);
            for (String constraintName : constraints) {
                jdbc.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
                System.out.println("[DB] Dropped legacy constraint: " + constraintName + " on " + columnName);
            }
        } catch (Exception e) {
            // Có thể không có constraint nào, bỏ qua
        }
    }

    private void createFilteredUniqueIndex(String tableName, String columnName) {
        String indexName = "UQ_Filtered_" + tableName + "_" + columnName;
        String sqlCheck = "SELECT COUNT(*) FROM sys.indexes WHERE name = ? AND object_id = OBJECT_ID(?)";
        Integer count = jdbc.queryForObject(sqlCheck, Integer.class, indexName, tableName);
        
        if (count == null || count == 0) {
            String sqlCreate = "CREATE UNIQUE INDEX " + indexName + " ON " + tableName + "(" + columnName + ") WHERE " + columnName + " IS NOT NULL";
            jdbc.execute(sqlCreate);
            System.out.println("[DB] Created Filtered Index: " + indexName);
        }
    }

    public boolean isNicknameTaken(String nickname) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM Users WHERE Nickname = ?", Integer.class, nickname);
        return count != null && count > 0;
    }

    public void updateNickname(int userId, String nickname) {
        jdbc.update("UPDATE Users SET Nickname = ? WHERE UserID = ?", nickname, userId);
    }

    public void updateAvatar(int userId, String avatarUrl) {
        jdbc.update("UPDATE Users SET AvatarURL = ? WHERE UserID = ?", avatarUrl, userId);
    }

    public boolean registerUser(String username, String hashedPassword, String nickname) {
        KeyHolder kh = new GeneratedKeyHolder();
        try {
            int rows = jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Users (Username, PasswordHash, Nickname, Role) VALUES (?, ?, ?, 'PLAYER')",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, nickname);
                return ps;
            }, kh);
            // Tạo row Players tương ứng
            Number key = kh.getKey();
            if (rows > 0 && key != null)
                jdbc.update("INSERT INTO Players (UserID) VALUES (?)", key.intValue());
            return rows > 0;
        } catch (DataIntegrityViolationException e) {
            System.err.println("[DB] Registration failed: Username/Email already exists. (" + e.getMessage() + ")");
            throw new UserAlreadyExistsException("Tên đăng nhập hoặc Email đã tồn tại!");
        } catch (Exception e) {
            System.err.println("[DB] Registration error: " + e.getMessage());
            return false;
        }
    }

    public String getPasswordHash(String username) {
        try {
            List<String> hashes = jdbc.query(
                    "SELECT PasswordHash FROM Users WHERE Username = ?",
                    (rs, n) -> rs.getString("PasswordHash"),
                    username);
            return hashes.isEmpty() ? null : hashes.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getUserIdByUsername(String username) {
        try {
            List<Integer> ids = jdbc.query(
                    "SELECT UserID FROM Users WHERE Username = ?",
                    (rs, n) -> rs.getInt("UserID"),
                    username);
            return ids.isEmpty() ? -1 : ids.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getUserIdByGoogleId(String googleId) {
        List<Integer> ids = jdbc.query("SELECT UserID FROM Users WHERE GoogleID = ?", (rs, n) -> rs.getInt("UserID"), googleId);
        return ids.isEmpty() ? -1 : ids.get(0);
    }

    public int getUserIdByFacebookId(String facebookId) {
        List<Integer> ids = jdbc.query("SELECT UserID FROM Users WHERE FacebookID = ?", (rs, n) -> rs.getInt("UserID"), facebookId);
        return ids.isEmpty() ? -1 : ids.get(0);
    }

    public int createSocialUser(String googleId, String facebookId, String avatarUrl) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Users (GoogleID, FacebookID, AvatarURL, Role) VALUES (?, ?, ?, 'PLAYER')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, googleId);
            ps.setString(2, facebookId);
            ps.setString(3, avatarUrl);
            return ps;
        }, kh);

        Number key = kh.getKey();
        int userId = (key != null) ? key.intValue() : -1;
        if (userId != -1) {
            jdbc.update("INSERT INTO Players (UserID) VALUES (?)", userId);
        }
        return userId;
    }

    public String getUsernameById(int userId) {
        List<String> names = jdbc.query("SELECT Username FROM Users WHERE UserID = ?", (rs, n) -> rs.getString("Username"), userId);
        return names.isEmpty() ? null : names.get(0);
    }

    public String getNickname(int userId) {
        List<String> nicknames = jdbc.query("SELECT Nickname FROM Users WHERE UserID = ?", (rs, n) -> rs.getString("Nickname"), userId);
        return nicknames.isEmpty() ? null : nicknames.get(0);
    }

    public String getAvatarUrl(int userId) {
        List<String> urls = jdbc.query("SELECT AvatarURL FROM Users WHERE UserID = ?", (rs, n) -> rs.getString("AvatarURL"), userId);
        return urls.isEmpty() ? null : urls.get(0);
    }

    public String getRoleByUsername(String username) {
        List<String> roles = jdbc.query("SELECT Role FROM Users WHERE Username = ?", (rs, n) -> rs.getString("Role"), username);
        return roles.isEmpty() ? "PLAYER" : roles.get(0);
    }

    public void updateRole(String username, String role) {
        jdbc.update("UPDATE Users SET Role = ? WHERE Username = ?", role, username);
    }
}
