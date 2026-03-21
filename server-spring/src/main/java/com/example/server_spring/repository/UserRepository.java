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

/**
 * Quản lý bảng Users — đăng ký, xác thực, truy vấn thông tin tài khoản.
 */
@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Users' AND xtype='U') " +
                        "CREATE TABLE Users (UserID INT PRIMARY KEY IDENTITY(1,1), " +
                        "Username NVARCHAR(50) UNIQUE NOT NULL, PasswordHash NVARCHAR(MAX) NOT NULL, " +
                        "CreatedAt DATETIME DEFAULT GETDATE())");
        System.out.println("[DB] UserRepository: Table ready.");
    }

    public boolean registerUser(String username, String hashedPassword) {
        KeyHolder kh = new GeneratedKeyHolder();
        try {
            int rows = jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Users (Username, PasswordHash) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                return ps;
            }, kh);
            // Tạo row Players tương ứng
            if (rows > 0 && kh.getKey() != null)
                jdbc.update("INSERT INTO Players (UserID) VALUES (?)", kh.getKey().intValue());
            return rows > 0;
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

    public String getUsernameById(int userId) {
        try {
            List<String> names = jdbc.query(
                    "SELECT Username FROM Users WHERE UserID = ?",
                    (rs, n) -> rs.getString("Username"),
                    userId);
            return names.isEmpty() ? null : names.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
