package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;

/**
 * Quáº£n lĂ½ báº£ng TransactionLog â€” ghi láº¡i má»i giao dá»‹ch tiá»n tá»‡.
 */
@Repository
@DependsOn("userRepository")
public class TransactionRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='TransactionLog' AND xtype='U') " +
                        "CREATE TABLE TransactionLog (LogID INT PRIMARY KEY IDENTITY(1,1), " +
                        "UserID INT FOREIGN KEY REFERENCES Users(UserID), Type NVARCHAR(20) NOT NULL, " +
                        "Amount INT NOT NULL, BalanceAfter INT NOT NULL, Reason NVARCHAR(255), " +
                        "CreatedAt DATETIME DEFAULT GETDATE())");
        System.out.println("[DB] TransactionRepository: Table ready.");
    }

    public void logTransaction(int userId, String type, int amount, int balanceAfter, String reason) {
        try {
            jdbc.update("INSERT INTO TransactionLog (UserID,Type,Amount,BalanceAfter,Reason) VALUES (?,?,?,?,?)",
                    userId, type, amount, balanceAfter, reason);
        } catch (Exception e) {
            /* non-critical */
        }
    }
}
