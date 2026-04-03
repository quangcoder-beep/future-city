package com.example.server_spring.services;

import com.example.server_spring.dto.PlayerSnapshotDTO;
import com.example.server_spring.managers.ServerStatsManager;
import com.example.server_spring.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service tổng hợp dữ liệu cho Admin Dashboard.
 * Giúp Controller không phải gọi quá nhiều Repository cùng lúc.
 */
@Service
public class DashboardService {

    @Autowired
    private ServerStatsManager stats;

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private PlayerService playerService;

    /**
     * Thu thập toàn bộ dữ liệu cần thiết cho hiển thị Dashboard.
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // 1. Chỉ số hệ thống (Metrics)
        data.put("ccu", stats.getOnlineCount());
        data.put("ramUsage", stats.getRamUsageMB());
        data.put("maxRam", stats.getMaxRamMB());
        data.put("cpuUsage", String.format("%.2f", stats.getCpuUsage()));
        data.put("uptime", stats.getUptime());
        data.put("packetRate", stats.getPacketRate());

        // 2. Logs (In-memory & DB)
        data.put("systemLogs", stats.getSystemLogs() != null ? stats.getSystemLogs() : List.of());
        data.put("auditLogs", adminRepo.getRecentLogs(50));
        data.put("ddosLogs", stats.getDdosLogs() != null ? stats.getDdosLogs() : List.of());

        // 3. Security (Bans & Warnings)
        data.put("activeBans", adminRepo.getActiveBans());
        data.put("recentWarnings", adminRepo.getRecentWarnings(20));

        // 4. Players (Snapshot Isolation)
        List<PlayerSnapshotDTO> playerSnapshots = playerService.getAll().stream()
                .filter(Objects::nonNull)
                .map(PlayerSnapshotDTO::new)
                .collect(Collectors.toList());
        data.put("players", playerSnapshots);

        return data;
    }

    /**
     * Xóa sạch các log hệ thống trong bộ nhớ.
     */
    public void clearSystemLogs() {
        stats.clearSystemLogs();
    }
}
