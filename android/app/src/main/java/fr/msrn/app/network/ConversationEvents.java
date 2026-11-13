// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.network;

import android.os.Handler;
import android.os.Looper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

/**
 * Conversation-scoped WebSocket with bounded retry and HTTP resynchronization.
 * Uses the emulator development endpoint. Production requires WSS.
 */
public final class ConversationEvents {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build();
    private static final long MAX_RETRY_MS = 30000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebSocket socket;
    private String token;
    private String conversationId;
    private Listener listener;
    private int generation;
    private int failures;
    private boolean active;

    public interface Listener {
        void onMessageChanged();
        void onConnected();
        void onDisconnected();
        void onTyping(boolean typing);
    }

    public void connect(String token, String conversationId, Listener listener) {
        if (!conversationId.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid conversation ID");
        }
        close();
        this.token = token;
        this.conversationId = conversationId;
        this.listener = listener;
        active = true;
        failures = 0;
        open(generation);
    }

    private void open(int expectedGeneration) {
        if (!active || generation != expectedGeneration) return;
        Request request = new Request.Builder()
            .url("ws://10.0.2.2:3000/conversations/" + conversationId + "/events")
            .header("Authorization", "Bearer " + token)
            .build();
        socket = CLIENT.newWebSocket(request, new WebSocketListener() {
            private boolean completed;

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                handler.post(() -> {
                    if (!valid(expectedGeneration) || completed) return;
                    failures = 0;
                    listener.onConnected();
                    // Close the gap while offline and during the handshake.
                    listener.onMessageChanged();
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    String type = new JSONObject(text).optString("type");
                    if ("typing".equals(type)) {
                        boolean typing = new JSONObject(text).optBoolean("typing");
                        handler.post(() -> {
                            if (valid(expectedGeneration) && !completed) listener.onTyping(typing);
                        });
                    } else if ("message_created".equals(type) || "resync_required".equals(type)) {
                        handler.post(() -> {
                            if (valid(expectedGeneration) && !completed) listener.onMessageChanged();
                        });
                    }
                } catch (Exception ignored) {
                    // HTTP history remains authoritative.
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable error, Response response) {
                finish(expectedGeneration, webSocket, response != null &&
                    (response.code() == 401 || response.code() == 403 || response.code() == 404));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                finish(expectedGeneration, webSocket, code == 1008);
            }

            private void finish(int expected, WebSocket webSocket, boolean permanent) {
                handler.post(() -> {
                    if (!valid(expected) || completed || socket != webSocket) return;
                    completed = true;
                    socket = null;
                    listener.onDisconnected();
                    if (permanent) {
                        active = false;
                        return;
                    }
                    long delay = Math.min(MAX_RETRY_MS, 1000L << Math.min(failures++, 5));
                    handler.postDelayed(() -> {
                        if (valid(expected) && socket == null) open(expected);
                    }, delay);
                });
            }
        });
    }

    private boolean valid(int expectedGeneration) {
        return active && generation == expectedGeneration;
    }

    public void sendTyping(boolean typing) {
        if (active && socket != null) {
            socket.send("{\"type\":\"typing\",\"typing\":" + typing + "}");
        }
    }

    public void close() {
        generation++;
        active = false;
        handler.removeCallbacksAndMessages(null);
        WebSocket previous = socket;
        socket = null;
        listener = null;
        token = null;
        conversationId = null;
        if (previous != null) previous.close(1000, "Leaving conversation");
    }
}
