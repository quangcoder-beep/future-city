package com.futurecity.game.managers;

import com.badlogic.gdx.Gdx;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Send REST API authentication (Login/Register) via HTTP before connecting to KryoNet.
 */
public class HttpAuthClientManager {

    private String baseUrl;

    public HttpAuthClientManager(String host, int port, boolean useHttps) {
        String scheme = useHttps ? "https" : "http";
        if ((useHttps && port == 443) || (!useHttps && port == 80)) {
            this.baseUrl = scheme + "://" + host;
        } else {
            this.baseUrl = scheme + "://" + host + ":" + port;
        }
    }

    public HttpAuthClientManager(String host, int port) {
        this(host, port, false);
    }

    public interface AuthCallback {
        void onSuccess(int userId, String accessToken, String refreshToken, String nickname, String avatarUrl, boolean needsNickname);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void login(String username, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
                String response = request("POST", baseUrl + "/auth/login", json, null);
                handleAuthResponse(response, callback);
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> callback.onError("Connect error: " + e.getMessage()));
            }
        }).start();
    }

    public void register(String username, String password, String nickname, AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"nickname\":\"" + nickname + "\"}";
                String response = request("POST", baseUrl + "/auth/register", json, null);
                if (response.contains("\"status\":\"SUCCESS\"")) {
                    Gdx.app.postRunnable(() -> callback.onSuccess(0, null, null, null, null, false));
                } else {
                    Gdx.app.postRunnable(() -> callback.onError(extractField(response, "message")));
                }
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> callback.onError("Connect error: " + e.getMessage()));
            }
        }).start();
    }

    public void refresh(String refreshToken, AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"token\":\"" + refreshToken + "\"}";
                String response = request("POST", baseUrl + "/auth/refresh", json, null);
                handleAuthResponse(response, callback);
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> callback.onError("Connect error: " + e.getMessage()));
            }
        }).start();
    }

    public void pollStatus(String sessionId, AuthCallback callback) {
        new Thread(() -> {
            try {
                String response = request("GET", baseUrl + "/api/auth/poll-status?sessionId=" + sessionId, null, null);
                if (response != null && response.contains("\"status\":\"SUCCESS\"")) {
                    handleAuthResponse(response, callback);
                } else if (response != null && response.contains("PENDING")) {
                    // Still pending
                } else {
                    Gdx.app.postRunnable(() -> callback.onError("Polling failed or session expired"));
                }
            } catch (Exception e) {
                // Ignore silent errors during polling
            }
        }).start();
    }

    public void checkNickname(String name, SimpleCallback callback) {
        new Thread(() -> {
            try {
                String response = request("GET", baseUrl + "/api/auth/check-nickname?name=" + name, null, null);
                if (response != null && response.contains("\"available\":true")) {
                    Gdx.app.postRunnable(() -> callback.onSuccess("AVAILABLE"));
                } else {
                    Gdx.app.postRunnable(() -> callback.onSuccess("TAKEN"));
                }
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void setNickname(String token, String nickname, SimpleCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"nickname\":\"" + nickname + "\"}";
                String response = request("POST", baseUrl + "/api/auth/set-nickname", json, token);
                if (response != null && response.contains("\"status\":\"SUCCESS\"")) {
                    Gdx.app.postRunnable(() -> callback.onSuccess("SUCCESS"));
                } else {
                    Gdx.app.postRunnable(() -> callback.onError("Set nickname failed"));
                }
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    private void handleAuthResponse(String response, AuthCallback callback) {
        if (response != null && response.contains("\"status\":\"SUCCESS\"")) {
            String token = extractField(response, "token");
            String refresh = extractField(response, "refreshToken");
            String nickname = extractField(response, "nickname");
            String avatar = extractField(response, "avatarUrl");
            boolean needs = response.contains("\"needsNickname\":true");
            int userId = extractInt(response, "newId");
            Gdx.app.postRunnable(() -> callback.onSuccess(userId, token, refresh, nickname, avatar, needs));
        } else {
            String msg = extractField(response, "message");
            Gdx.app.postRunnable(() -> callback.onError(msg != null ? msg : "Auth failed"));
        }
    }

    private String request(String method, String urlStr, String jsonBody, String token) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (jsonBody != null) {
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
        }
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (jsonBody != null) {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
            }
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return null;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return null;
        
        // Tìm dấu hai chấm sau key
        int colonIndex = json.indexOf(":", keyIndex + key.length());
        if (colonIndex == -1) return null;
        
        // Tìm dấu ngoặc kép mở sau dấu hai chấm
        int quoteStart = json.indexOf("\"", colonIndex);
        if (quoteStart == -1) return null;
        
        int start = quoteStart + 1;
        int end = json.indexOf("\"", start);
        return (end == -1) ? null : json.substring(start, end);
    }

    private int extractInt(String json, String field) {
        String key = "\"" + field + "\":";
        int start = json.indexOf(key);
        if (start == -1) return -1;
        start += key.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return -1; }
    }
}
