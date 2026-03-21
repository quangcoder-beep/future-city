package com.futurecity.game.managers;

import com.badlogic.gdx.Gdx;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Send REST API authentication (Login/Register) via HTTP before connecting to
 * KryoNet.
 * Runs on a separate thread to avoid blocking the UI.
 */
public class HttpAuthClientManager {

    private String baseUrl;

    public HttpAuthClientManager(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
    }

    public interface AuthCallback {
        void onSuccess(int userId, String accessToken, String refreshToken);

        void onError(String message);
    }

    /**
     * Send POST /auth/login on background thread.
     */
    public void login(String username, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
                String response = post(baseUrl + "/auth/login", json);

                // Parse response simply (no JSON library needed)
                if (response.contains("\"status\":\"SUCCESS\"")) {
                    String token = extractField(response, "token");
                    String refreshToken = extractField(response, "refreshToken");
                    int newId = extractInt(response, "newId");
                    Gdx.app.postRunnable(() -> callback.onSuccess(newId, token, refreshToken));
                } else {
                    String message = extractField(response, "message");
                    Gdx.app.postRunnable(
                            () -> callback.onError(message != null ? message : "Login failed"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Gdx.app.postRunnable(() -> callback.onError("Failed to connect to HTTP: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Send POST /auth/register on background thread.
     */
    public void register(String username, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
                String response = post(baseUrl + "/auth/register", json);

                if (response.contains("\"status\":\"SUCCESS\"")) {
                    Gdx.app.postRunnable(() -> callback.onSuccess(0, null, null));
                } else {
                    String message = extractField(response, "message");
                    Gdx.app.postRunnable(() -> callback.onError(message != null ? message : "Register failed"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Gdx.app.postRunnable(() -> callback.onError("Failed to connect to HTTP: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Send POST /auth/refresh to get new Access Token using Refresh Token.
     * No need to re-enter password.
     */
    public void refresh(String refreshToken, AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"token\":\"" + refreshToken + "\"}";
                String response = post(baseUrl + "/auth/refresh", json);

                if (response.contains("\"status\":\"SUCCESS\"")) {
                    String newAccessToken = extractField(response, "token");
                    int userId = extractInt(response, "newId");
                    Gdx.app.postRunnable(() -> callback.onSuccess(userId, newAccessToken, null));
                } else {
                    Gdx.app.postRunnable(() -> callback.onError("Refresh Token expired"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Gdx.app.postRunnable(() -> callback.onError("Failed to connect to HTTP: " + e.getMessage()));
            }
        }).start();
    }

    private String post(String urlStr, String jsonBody) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /** Extract String value from simple JSON (no library needed). */
    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1)
            return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;
        return json.substring(start, end);
    }

    /** Extract int value from simple JSON. */
    private int extractInt(String json, String field) {
        String key = "\"" + field + "\":";
        int start = json.indexOf(key);
        if (start == -1)
            return -1;
        start += key.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
