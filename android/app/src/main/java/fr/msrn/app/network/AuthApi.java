// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.network;

import org.json.JSONObject;
import org.json.JSONArray;
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

    public static JSONArray contacts(String token) throws Exception {
        return new JSONArray(requestText("GET", "/contacts", null, token));
    }

    public static JSONArray incomingRequests(String token) throws Exception {
        return new JSONArray(requestText("GET", "/friends/requests", null, token));
    }

    public static void sendFriendRequest(String token, String username) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        requestText("POST", "/friends/requests", body, token);
    }

    public static void respondToRequest(String token, String id, boolean accept) throws Exception {
        if (!id.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid ID");
        requestText("POST", "/friends/requests/" + id + (accept ? "/accept" : "/decline"), null, token);
    }

    public static JSONObject openConversation(String token, String username) throws Exception {
        if (!username.matches("[a-z0-9_]{3,40}")) throw new IllegalArgumentException("Invalid username");
        return request("POST", "/conversations/with/" + username, null, token);
    }

    public static JSONArray messages(String token, String conversationId) throws Exception {
        if (!conversationId.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid conversation ID");
        return new JSONArray(requestText("GET", "/conversations/" + conversationId + "/messages", null, token));
    }

    public static JSONObject messagePage(String token, String conversationId, String before) throws Exception {
        if (!conversationId.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid conversation ID");
        String path = "/conversations/" + conversationId + "/messages/page?limit=50";
        if (before != null) {
            if (!before.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid cursor");
            path += "&before=" + before;
        }
        return request("GET", path, null, token);
    }

    public static void markRead(String token, String conversationId, String messageId) throws Exception {
        if (!conversationId.matches("[0-9a-fA-F-]{36}") ||
            !messageId.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid conversation or message ID");
        }
        JSONObject body = new JSONObject();
        body.put("message_id", messageId);
        requestText("POST", "/conversations/" + conversationId + "/read", body, token);
    }

    public static JSONObject sendMessage(String token, String conversationId, String message) throws Exception {
        if (!conversationId.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid conversation ID");
        JSONObject body = new JSONObject();
        body.put("body", message);
        return request("POST", "/conversations/" + conversationId + "/messages", body, token);
    }

    public static JSONObject setPersonalMessage(String token, String message) throws Exception {
        JSONObject body = new JSONObject();
        body.put("personal_message", message);
        return request("PUT", "/profile/message", body, token);
    }

    public static JSONObject myProfile(String token) throws Exception {
        return request("GET", "/profile/me", null, token);
    }

    public static JSONObject updateProfile(String token, String displayName, int avatarId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("display_name", displayName);
        body.put("avatar_id", avatarId);
        return request("PUT", "/profile/me", body, token);
    }

    public static JSONObject myPresence(String token) throws Exception {
        return request("GET", "/presence/me", null, token);
    }

    public static JSONObject setPresence(String token, String presence) throws Exception {
        JSONObject body = new JSONObject();
        body.put("presence", presence);
        return request("PUT", "/presence/me", body, token);
    }

    public static void heartbeat(String token) throws Exception {
        request("POST", "/presence/heartbeat", null, token);
    }

    public static JSONObject me(String token) throws Exception {
        return request("GET", "/auth/me", null, token);
    }

    public static void logout(String token) throws Exception {
        request("POST", "/auth/logout", null, token);
    }

    private static JSONObject request(String method, String path, JSONObject body, String token) throws Exception {
        String result = requestText(method, path, body, token);
        return result.isEmpty() ? new JSONObject() : new JSONObject(result);
    }

    private static String requestText(String method, String path, JSONObject body, String token) throws Exception {
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
            if (code == 204 || code == 201 && body != null && path.equals("/friends/requests")) return "";
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
            return text;
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
