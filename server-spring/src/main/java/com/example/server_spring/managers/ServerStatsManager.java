package com.example.server_spring.managers;

import com.example.server_spring.services.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Component
public class ServerStatsManager {

    @Autowired
    private PlayerService playerService;

    private final long startTime = System.currentTimeMillis();
    private final java.util.concurrent.atomic.AtomicInteger totalPackets = new java.util.concurrent.atomic.AtomicInteger(0);
    private int lastPacketRate = 0;

    // Log Entry Structure
    public static class LogEntry {
        public long timestamp;
        public String level; // INFO, WARN, CRITICAL
        public String source;
        public String message;
        public Integer playerId;

        public LogEntry(String level, String source, String message, Integer playerId) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.source = source;
            this.message = message;
            this.playerId = playerId;
        }
    }

    // Thread-safe Capacity-limited Lists for UI
    private final List<LogEntry> systemLogs = Collections.synchronizedList(new java.util.ArrayList<LogEntry>() {
        @Override
        public boolean add(LogEntry e) {
            if (size() >= 100) remove(0);
            return super.add(e);
        }
    });

    private final List<String> ddosLogs = Collections.synchronizedList(new java.util.ArrayList<String>() {
        @Override
        public boolean add(String e) {
            if (size() >= 50) remove(0);
            return super.add(e);
        }
    });

    public void logError(String level, String source, String message, Integer playerId) {
        systemLogs.add(new LogEntry(level, source, message, playerId));
    }

    public void logDdos(String message) {
        ddosLogs.add(message);
    }

    // Called by a scheduled task or KryoNetworkHandler every second
    public void updatePacketRate() {
        lastPacketRate = totalPackets.getAndSet(0);
    }

    public void incrementPacketCount() {
        totalPackets.incrementAndGet();
    }

    // Getters for UI
    public List<LogEntry> getSystemLogs() {
        return new java.util.ArrayList<>(systemLogs);
    }

    public List<String> getDdosLogs() {
        return new java.util.ArrayList<>(ddosLogs);
    }

    public int getOnlineCount() {
        return playerService != null ? playerService.getAll().size() : 0;
    }

    public int getPacketRate() {
        return lastPacketRate;
    }

    public String getUptime() {
        long diff = System.currentTimeMillis() - startTime;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
    }

    public long getRamUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    public long getMaxRamMB() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    public double getCpuUsage() {
        try {
            // Sử dụng casting tới com.sun.management.OperatingSystemMXBean để lấy CPU Load thực tế trên Windows
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double load = ((com.sun.management.OperatingSystemMXBean) osBean).getCpuLoad();
                if (load >= 0) return load * 100.0;
            }
            // Fallback nếu không cast được
            double loadAvg = osBean.getSystemLoadAverage();
            return loadAvg >= 0 ? loadAvg : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public void clearSystemLogs() {
        systemLogs.clear();
    }
}
