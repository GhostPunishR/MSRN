// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Receives change notifications. Message bodies are always fetched through AuthApi.
 * This development-only URL must become WSS before production.
 */
public final class ConversationEvents {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build();

    public interface Listener {
        void onMessageChanged();
        void onDisconnected();
    }

    private WebSocket socket;

    public void connect(String token, String conversationId, Listener listener) {
        close();
        if (!conversationId.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid conversation ID");
        }
        Request request = new Request.Builder()
            .url("ws://10.0.2.2:3000/conversations/" + conversationId + "/events")
            .header("Authorization", "Bearer " + token)
            .build();
        socket = CLIENT.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    String type = new JSONObject(text).optString("type");
                    if ("message_created".equals(type) || "resync_required".equals(type)) {
                        listener.onMessageChanged();
                    }
                } catch (Exception ignored) {
                    // Ignore malformed notifications. HTTP remains the source of truth.
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable error, Response response) {
                listener.onDisconnected();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                listener.onDisconnected();
            }
        });
    }

    public void close() {
        WebSocket previous = socket;
        socket = null;
        if (previous != null) previous.close(1000, "Leaving conversation");
    }
}
