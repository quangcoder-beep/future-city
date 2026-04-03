package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;

/**
 * Quáº£n lĂ½ báº£ng Items â€” Ä‘á»‹nh nghÄ©a váº­t pháº©m trong game.
 */
@Repository
public class ItemRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Items' AND xtype='U') " +
                        "CREATE TABLE Items (ItemID INT PRIMARY KEY IDENTITY(1,1), " +
                        "ItemName NVARCHAR(100) NOT NULL, ItemType NVARCHAR(20) NOT NULL, " +
                        "Description NVARCHAR(255), Rarity NVARCHAR(20) DEFAULT 'COMMON')");
        System.out.println("[DB] ItemRepository: Table ready.");
    }
}
