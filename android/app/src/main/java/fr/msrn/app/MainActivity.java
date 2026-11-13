// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final StringBuilder demoMessages = new StringBuilder("Camille : Salut !\n");
    private boolean invitationAccepted = false;
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
        FrameLayout container = findViewById(R.id.screen_container);
        container.removeAllViews();
        View screen = LayoutInflater.from(this).inflate(layoutId, container, false);
        container.addView(screen);

        if (layoutId == R.layout.screen_contacts) {
            screen.findViewById(R.id.open_chat).setOnClickListener(v -> showScreen(R.layout.screen_chat));
        } else if (layoutId == R.layout.screen_chat) {
            TextView history = screen.findViewById(R.id.chat_history);
            history.setText(demoMessages.toString());
            EditText input = screen.findViewById(R.id.message_input);
            screen.findViewById(R.id.send_message).setOnClickListener(v -> {
                String message = input.getText().toString().trim();
                if (message.isEmpty()) return;
                demoMessages.append(nickname).append(" : ").append(message).append("\n");
                history.setText(demoMessages.toString());
                input.setText("");
            });
            screen.findViewById(R.id.send_wizz).setOnClickListener(v -> {
                demoMessages.append(nickname).append(" a envoyé un Wizz !\n");
                history.setText(demoMessages.toString());
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                screen.animate().translationX(14f).setDuration(70).withEndAction(
                    () -> screen.animate().translationX(0f).setDuration(70).start()
                ).start();
            });
        } else if (layoutId == R.layout.screen_friends) {
            TextView status = screen.findViewById(R.id.friend_request_status);
            Button accept = screen.findViewById(R.id.accept_friend);
            if (invitationAccepted) {
                status.setText("Sam a été ajouté (démo locale).");
                accept.setEnabled(false);
            }
            accept.setOnClickListener(v -> {
                invitationAccepted = true;
                status.setText("Sam a été ajouté (démo locale).");
                accept.setEnabled(false);
            });
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
            TextView status = screen.findViewById(R.id.server_status);
            screen.findViewById(R.id.check_server).setOnClickListener(v -> checkServer(status));
        }
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
                    byte[] bytes = stream.readNBytes(4096);
                    result = "HTTP " + code + " : " + new String(bytes, StandardCharsets.UTF_8);
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
        io.shutdownNow();
        super.onDestroy();
    }
}
