// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app;

import android.app.Activity;
import fr.msrn.app.network.AuthApi;
import fr.msrn.app.network.ConversationEvents;
import org.json.JSONObject;
import org.json.JSONArray;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ConversationEvents events = new ConversationEvents();
    private String activeConversationId;
    private String activeContactName;
    private String sessionToken;
    private String nickname = "Moi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nickname = getPreferences(MODE_PRIVATE).getString("nickname", "Moi");

        findViewById(R.id.tab_contacts).setOnClickListener(v -> showScreen(R.layout.screen_contacts));
        findViewById(R.id.tab_chat).setOnClickListener(v -> showScreen(R.layout.screen_chat));
        findViewById(R.id.tab_friends).setOnClickListener(v -> showScreen(R.layout.screen_friends));
        findViewById(R.id.tab_profile).setOnClickListener(v -> showScreen(R.layout.screen_profile));
        showScreen(R.layout.screen_contacts);
    }

    private void showScreen(int layoutId) {
        events.close();
        FrameLayout container = findViewById(R.id.screen_container);
        container.removeAllViews();
        View screen = LayoutInflater.from(this).inflate(layoutId, container, false);
        container.addView(screen);

        if (layoutId == R.layout.screen_contacts) {
            screen.findViewById(R.id.refresh_contacts).setOnClickListener(v -> loadContacts(screen));
            loadContacts(screen);
        } else if (layoutId == R.layout.screen_chat) {
            setupChat(screen);
        } else if (layoutId == R.layout.screen_friends) {
            screen.findViewById(R.id.refresh_requests).setOnClickListener(v -> loadRequests(screen));
            screen.findViewById(R.id.send_friend_request).setOnClickListener(v -> sendInvitation(screen));
            loadRequests(screen);
        } else if (layoutId == R.layout.screen_profile) {
            EditText name = screen.findViewById(R.id.profile_name);
            name.setText(nickname);
            screen.findViewById(R.id.save_profile).setOnClickListener(v -> {
                String value = name.getText().toString().trim();
                if (value.isEmpty()) {
                    name.setError("Pseudo requis");
                    return;
                }
                nickname = value;
                getPreferences(MODE_PRIVATE).edit().putString("nickname", nickname).apply();
                Toast.makeText(this, "Pseudo enregistré sur cet appareil", Toast.LENGTH_SHORT).show();
            });
            screen.findViewById(R.id.open_auth).setOnClickListener(v -> showScreen(R.layout.screen_auth));
            TextView status = screen.findViewById(R.id.server_status);
            screen.findViewById(R.id.check_server).setOnClickListener(v -> checkServer(status));
        } else if (layoutId == R.layout.screen_auth) {
            setupAuthScreen(screen);
        }
    }

    private void openConversation(String username) {
        if (sessionToken == null) {
            showScreen(R.layout.screen_auth);
            return;
        }
        String token = sessionToken;
        activeConversationId = null;
        activeContactName = username;
        showScreen(R.layout.screen_chat);
        io.execute(() -> {
            try {
                JSONObject conversation = AuthApi.openConversation(token, username);
                String id = conversation.getString("id");
                runOnUiThread(() -> {
                    if (!token.equals(sessionToken) || !username.equals(activeContactName)) return;
                    activeConversationId = id;
                    FrameLayout container = findViewById(R.id.screen_container);
                    if (container.findViewById(R.id.chat_history) != null) {
                        setupChat(container.getChildAt(0));
                        connectEvents(container.getChildAt(0), token, id);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    FrameLayout container = findViewById(R.id.screen_container);
                    TextView status = container.findViewById(R.id.chat_status);
                    if (status != null) status.setText("Conversation inaccessible. Vérifiez votre liste de contacts.");
                });
            }
        });
    }

    private void setupChat(View screen) {
        TextView title = screen.findViewById(R.id.chat_title);
        TextView status = screen.findViewById(R.id.chat_status);
        title.setText(activeContactName == null ? "Choisissez un contact" : "@" + activeContactName);
        Button send = screen.findViewById(R.id.send_message);
        Button refresh = screen.findViewById(R.id.refresh_messages);
        EditText input = screen.findViewById(R.id.message_input);
        send.setEnabled(sessionToken != null && activeConversationId != null);
        refresh.setEnabled(sessionToken != null && activeConversationId != null);
        status.setText(activeConversationId == null ? "Choisissez un contact dans Contacts." : "Chargement...");
        if (activeConversationId != null) {
            loadMessages(screen);
            if (sessionToken != null) connectEvents(screen, sessionToken, activeConversationId);
        }
        refresh.setOnClickListener(v -> loadMessages(screen));
        send.setOnClickListener(v -> {
            if (sessionToken == null || activeConversationId == null) return;
            String body = input.getText().toString();
            if (body.trim().isEmpty() || body.length() > 4000) return;
            String token = sessionToken;
            String conversationId = activeConversationId;
            send.setEnabled(false);
            status.setText("Envoi...");
            io.execute(() -> {
                try {
                    AuthApi.sendMessage(token, conversationId, body);
                    runOnUiThread(() -> {
                        if (screen.getParent() == null) return;
                        input.setText("");
                        send.setEnabled(true);
                        loadMessages(screen);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (screen.getParent() == null) return;
                        send.setEnabled(true);
                        status.setText("Message non envoyé. Réessayez.");
                    });
                }
            });
        });
        screen.findViewById(R.id.send_wizz).setOnClickListener(v -> {
            status.setText("Wizz local uniquement, non transmis.");
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            screen.animate().translationX(14f).setDuration(70).withEndAction(
                () -> screen.animate().translationX(0f).setDuration(70).start()
            ).start();
        });
    }

    private void connectEvents(View screen, String token, String conversationId) {
        events.connect(token, conversationId, new ConversationEvents.Listener() {
            @Override
            public void onMessageChanged() {
                runOnUiThread(() -> {
                    if (screen.getParent() != null && token.equals(sessionToken)
                        && conversationId.equals(activeConversationId)) {
                        loadMessages(screen);
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    if (screen.getParent() != null && token.equals(sessionToken)
                        && conversationId.equals(activeConversationId)) {
                        TextView status = screen.findViewById(R.id.chat_status);
                        status.setText("Temps réel déconnecté. Actualisez manuellement.");
                    }
                });
            }
        });
    }

    private void loadMessages(View screen) {
        if (sessionToken == null || activeConversationId == null) return;
        String token = sessionToken;
        String conversationId = activeConversationId;
        TextView history = screen.findViewById(R.id.chat_history);
        TextView status = screen.findViewById(R.id.chat_status);
        Button send = screen.findViewById(R.id.send_message);
        Button refresh = screen.findViewById(R.id.refresh_messages);
        send.setEnabled(true);
        refresh.setEnabled(true);
        status.setText("Chargement des messages...");
        io.execute(() -> {
            try {
                JSONArray messages = AuthApi.messages(token, conversationId);
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject message = messages.optJSONObject(i);
                    if (message == null) continue;
                    text.append(message.optString("sender_id")).append(" : ")
                        .append(message.optString("body")).append("\n");
                }
                runOnUiThread(() -> {
                    if (screen.getParent() == null || !token.equals(sessionToken)
                        || !conversationId.equals(activeConversationId)) return;
                    history.setText(text.length() == 0 ? "Aucun message." : text.toString());
                    status.setText("100 derniers messages maximum. Actualisation manuelle.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (screen.getParent() != null) status.setText("Impossible de charger les messages.");
                });
            }
        });
    }

    private void loadContacts(View screen) {
        TextView status = screen.findViewById(R.id.contacts_status);
        LinearLayout list = screen.findViewById(R.id.contacts_list);
        list.removeAllViews();
        if (sessionToken == null) {
            status.setText("Connectez-vous dans Profil, Mon compte MSRN.");
            return;
        }
        String token = sessionToken;
        status.setText("Chargement des contacts...");
        io.execute(() -> {
            try {
                JSONArray contacts = AuthApi.contacts(token);
                runOnUiThread(() -> {
                    if (sessionToken == null || !sessionToken.equals(token)) return;
                    if (screen.getParent() == null) return;
                    status.setText(contacts.length() == 0 ? "Aucun contact pour le moment." : contacts.length() + " contact(s)");
                    list.removeAllViews();
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject user = contacts.optJSONObject(i);
                        if (user == null) continue;
                        TextView item = new TextView(this);
                        item.setText(user.optString("display_name") + " (@" + user.optString("username") + ")");
                        item.setTextSize(18f);
                        item.setPadding(0, 16, 0, 16);
                        String username = user.optString("username");
                        item.setOnClickListener(v -> openConversation(username));
                        list.addView(item);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (screen.getParent() != null) status.setText("Impossible de charger les contacts.");
                });
            }
        });
    }

    private void loadRequests(View screen) {
        TextView status = screen.findViewById(R.id.friend_request_status);
        LinearLayout list = screen.findViewById(R.id.requests_list);
        list.removeAllViews();
        if (sessionToken == null) {
            status.setText("Connectez-vous dans Profil, Mon compte MSRN.");
            return;
        }
        String token = sessionToken;
        status.setText("Chargement des invitations...");
        io.execute(() -> {
            try {
                JSONArray requests = AuthApi.incomingRequests(token);
                runOnUiThread(() -> {
                    if (sessionToken == null || !sessionToken.equals(token)) return;
                    if (screen.getParent() == null) return;
                    list.removeAllViews();
                    status.setText(requests.length() == 0 ? "Aucune invitation reçue." : requests.length() + " invitation(s)");
                    for (int i = 0; i < requests.length(); i++) {
                        JSONObject request = requests.optJSONObject(i);
                        if (request == null) continue;
                        String id = request.optString("id");
                        JSONObject sender = request.optJSONObject("sender");
                        if (sender == null) continue;
                        TextView name = new TextView(this);
                        name.setText(sender.optString("display_name") + " (@" + sender.optString("username") + ")");
                        name.setTextSize(18f);
                        list.addView(name);
                        Button accept = new Button(this);
                        accept.setText("Accepter");
                        accept.setOnClickListener(v -> respondToInvitation(screen, id, true));
                        list.addView(accept);
                        Button decline = new Button(this);
                        decline.setText("Refuser");
                        decline.setOnClickListener(v -> respondToInvitation(screen, id, false));
                        list.addView(decline);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (screen.getParent() != null) status.setText("Impossible de charger les invitations.");
                });
            }
        });
    }

    private void sendInvitation(View screen) {
        TextView status = screen.findViewById(R.id.friend_request_status);
        EditText username = screen.findViewById(R.id.friend_username);
        String target = username.getText().toString().trim();
        if (sessionToken == null) {
            status.setText("Connectez-vous d'abord.");
            return;
        }
        if (!target.matches("[a-z0-9_]{3,40}")) {
            status.setText("Identifiant invalide.");
            return;
        }
        String token = sessionToken;
        status.setText("Envoi de l'invitation...");
        io.execute(() -> {
            try {
                AuthApi.sendFriendRequest(token, target);
                runOnUiThread(() -> {
                    if (screen.getParent() != null) status.setText("Invitation envoyée.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (screen.getParent() != null) status.setText("Envoi impossible. Vérifier le compte ou une invitation existante.");
                });
            }
        });
    }

    private void respondToInvitation(View screen, String id, boolean accept) {
        if (sessionToken == null) return;
        String token = sessionToken;
        TextView status = screen.findViewById(R.id.friend_request_status);
        status.setText("Traitement...");
        io.execute(() -> {
            try {
                AuthApi.respondToRequest(token, id, accept);
                runOnUiThread(() -> {
                    if (screen.getParent() != null) loadRequests(screen);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (screen.getParent() != null) status.setText("Action impossible. Actualisez les invitations.");
                });
            }
        });
    }

    private void setupAuthScreen(View screen) {
        EditText username = screen.findViewById(R.id.auth_username);
        EditText displayName = screen.findViewById(R.id.auth_display_name);
        EditText password = screen.findViewById(R.id.auth_password);
        TextView status = screen.findViewById(R.id.auth_status);
        TextView identity = screen.findViewById(R.id.auth_identity);
        Button login = screen.findViewById(R.id.auth_login);
        Button register = screen.findViewById(R.id.auth_register);
        Button logout = screen.findViewById(R.id.auth_logout);
        identity.setText(sessionToken == null ? "Non connecté" : "Session active (en mémoire)");
        logout.setEnabled(sessionToken != null);

        login.setOnClickListener(v -> authenticate(screen, false));
        register.setOnClickListener(v -> authenticate(screen, true));
        logout.setOnClickListener(v -> {
            if (sessionToken == null) return;
            String token = sessionToken;
            login.setEnabled(false);
            register.setEnabled(false);
            logout.setEnabled(false);
            status.setText("Déconnexion...");
            io.execute(() -> {
                try {
                    AuthApi.logout(token);
                    sessionToken = null;
                    runOnUiThread(() -> {
                        identity.setText("Non connecté");
                        activeConversationId = null;
                        activeContactName = null;
                        status.setText("Déconnecté");
                        login.setEnabled(true);
                        register.setEnabled(true);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        status.setText("Déconnexion impossible. Réessayer.");
                        login.setEnabled(true);
                        register.setEnabled(true);
                        logout.setEnabled(true);
                    });
                }
            });
        });
    }

    private void authenticate(View screen, boolean createAccount) {
        EditText username = screen.findViewById(R.id.auth_username);
        EditText displayName = screen.findViewById(R.id.auth_display_name);
        EditText password = screen.findViewById(R.id.auth_password);
        TextView status = screen.findViewById(R.id.auth_status);
        TextView identity = screen.findViewById(R.id.auth_identity);
        Button login = screen.findViewById(R.id.auth_login);
        Button register = screen.findViewById(R.id.auth_register);
        Button logout = screen.findViewById(R.id.auth_logout);

        String user = username.getText().toString().trim();
        String name = displayName.getText().toString().trim();
        String secret = password.getText().toString();
        if (!user.matches("[a-z0-9_]{3,40}") || secret.length() < 12 || secret.getBytes(StandardCharsets.UTF_8).length > 128
                || (createAccount && (name.isEmpty() || name.length() > 80))) {
            status.setText("Vérifie l'identifiant, le pseudo et le mot de passe.");
            return;
        }
        login.setEnabled(false);
        register.setEnabled(false);
        logout.setEnabled(false);
        status.setText("Connexion en cours...");
        io.execute(() -> {
            try {
                JSONObject response = createAccount
                    ? AuthApi.register(user, name, secret)
                    : AuthApi.login(user, secret);
                String token = response.getString("token");
                String actualName = response.getJSONObject("user").getString("display_name");
                sessionToken = token;
                runOnUiThread(() -> {
                    identity.setText("Connecté : " + actualName);
                    password.setText("");
                    status.setText("Compte connecté au serveur local.");
                    login.setEnabled(true);
                    register.setEnabled(true);
                    logout.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText(e instanceof AuthApi.ApiException
                        ? "Erreur HTTP " + ((AuthApi.ApiException) e).status
                        : "Serveur inaccessible ou réponse invalide.");
                    login.setEnabled(true);
                    register.setEnabled(true);
                    logout.setEnabled(sessionToken != null);
                });
            }
        });
    }

    private void checkServer(TextView status) {
        status.setText("Connexion au serveur...");
        io.execute(() -> {
            HttpURLConnection connection = null;
            String result;
            try {
                // Emulator loopback to the host machine, for local debug builds only.
                URL url = new URL("http://10.0.2.2:3000/health");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                int code = connection.getResponseCode();
                try (InputStream stream = code < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[1024];
                    int read;
                    while (buffer.size() < 4096 && (read = stream.read(chunk, 0, Math.min(chunk.length, 4096 - buffer.size()))) != -1) {
                        buffer.write(chunk, 0, read);
                    }
                    result = "HTTP " + code + " : " + buffer.toString(StandardCharsets.UTF_8.name());
                }
            } catch (Exception e) {
                result = "Serveur inaccessible. Vérifier le démarrage Rust et PostgreSQL.";
            } finally {
                if (connection != null) connection.disconnect();
            }
            String output = result;
            runOnUiThread(() -> status.setText(output));
        });
    }

    @Override
    protected void onDestroy() {
        events.close();
        io.shutdownNow();
        super.onDestroy();
    }
}
