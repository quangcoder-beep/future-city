package com.example.server_spring.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Quáº£n lĂ½ báº£ng Npcs + NpcDialogues â€” thĂ´ng tin vĂ  há»™i thoáº¡i NPC.
 */
@Repository
public class NpcRepository {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void initTables() {
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Npcs' AND xtype='U') " +
                        "CREATE TABLE Npcs (NpcID NVARCHAR(50) PRIMARY KEY, " +
                        "NpcName NVARCHAR(100) NOT NULL, LocationX FLOAT, LocationY FLOAT, LocationZ FLOAT)");
        jdbc.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='NpcDialogues' AND xtype='U') " +
                        "CREATE TABLE NpcDialogues (DialogueID INT PRIMARY KEY IDENTITY(1,1), " +
                        "NpcID NVARCHAR(50) NOT NULL REFERENCES Npcs(NpcID), LineOrder INT NOT NULL, " +
                        "DialogueText NVARCHAR(500) NOT NULL)");
        System.out.println("[DB] NpcRepository: Tables ready.");
    }

    public void upsertNpc(String npcId, String name, float x, float y, float z) {
        jdbc.update(
                "MERGE Npcs AS t USING (SELECT ? AS id, ? AS n, ? AS x, ? AS y, ? AS z) AS s " +
                        "ON t.NpcID = s.id " +
                        "WHEN MATCHED THEN UPDATE SET NpcName=s.n, LocationX=s.x, LocationY=s.y, LocationZ=s.z " +
                        "WHEN NOT MATCHED THEN INSERT (NpcID, NpcName, LocationX, LocationY, LocationZ) " +
                        "VALUES(s.id, s.n, s.x, s.y, s.z);",
                npcId, name, x, y, z);
    }

    public String[] getNpcDialogues(String npcId) {
        List<String> lines = jdbc.query(
                "SELECT DialogueText FROM NpcDialogues WHERE NpcID=? ORDER BY LineOrder",
                (rs, n) -> rs.getString("DialogueText"), npcId);
        return lines.toArray(new String[0]);
    }
}
