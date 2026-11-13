// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.network;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class AuthApi {
    private static final String BASE_URL = "http://10.0.2.2:3000";
    private AuthApi() {}

    public static JSONObject login(String username, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        return request("POST", "/auth/login", body, null);
    }

    public static JSONObject register(String username, String displayName, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("display_name", displayName);
        body.put("password", password);
        return request("POST", "/auth/register", body, null);
    }

    public static JSONObject me(String token) throws Exception {
        return request("GET", "/auth/me", null, token);
    }

    public static void logout(String token) throws Exception {
        request("POST", "/auth/logout", null, token);
    }

    private static JSONObject request(String method, String path, JSONObject body, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }

            int code = connection.getResponseCode();
            if (code == 204) return new JSONObject();
            InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = "";
            if (stream != null) {
                try (InputStream input = stream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[1024];
                    int read;
                    while (buffer.size() < 8192 && (read = input.read(chunk, 0, Math.min(chunk.length, 8192 - buffer.size()))) != -1) {
                        buffer.write(chunk, 0, read);
                    }
                    text = buffer.toString("UTF-8");
                }
            }
            if (code >= 400) throw new ApiException(code);
            return new JSONObject(text);
        } finally {
            connection.disconnect();
        }
    }

    public static final class ApiException extends Exception {
        public final int status;
        public ApiException(int status) {
            super("HTTP " + status);
            this.status = status;
        }
    }
}
