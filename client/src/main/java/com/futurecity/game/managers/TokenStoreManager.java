package com.futurecity.game.managers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lưu và đọc Access Token + Refresh Token từ file trên ổ cứng.
 * Vị trí: %LOCALAPPDATA%\FutureCity\tokens.dat (Windows)
 *
 * Cho phép user bật game mà không cần nhập lại password,
 * miễn là Refresh Token còn hạn (30 ngày).
 */
public class TokenStoreManager {

    private static final String FILE_NAME = "tokens.dat";

    private static Path getFilePath() {
        String appData = System.getenv("LOCALAPPDATA");
        if (appData == null) {
            // Fallback cho non-Windows (Linux, Mac)
            appData = System.getProperty("user.home") + "/.local/share";
        }
        return Paths.get(appData, "FutureCity", FILE_NAME);
    }

    /**
     * Lưu cả 2 token xuống file.
     */
    public static void save(String accessToken, String refreshToken) {
        try {
            Path filePath = getFilePath();
            Files.createDirectories(filePath.getParent());
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
                writer.println(accessToken);
                writer.println(refreshToken);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load token từ file. Return null nếu file không tồn tại.
     */
    public static String[] load() {
        try {
            Path filePath = getFilePath();
            if (!Files.exists(filePath))
                return null;

            try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
                String accessToken = reader.readLine();
                String refreshToken = reader.readLine();
                if (accessToken != null && refreshToken != null
                        && !accessToken.isEmpty() && !refreshToken.isEmpty()) {
                    return new String[] { accessToken, refreshToken };
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Xóa file token (dùng khi logout hoặc token hết hạn).
     */
    public static void clear() {
        try {
            Files.deleteIfExists(getFilePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
