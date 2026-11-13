// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.chat;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;

/** Optional foreground tones using Android's notification stream. Main thread only. */
public final class ChatSounds {
    public static final String MESSAGE_PREF = "message_sounds_enabled";
    public static final String WIZZ_PREF = "wizz_sounds_enabled";
    private final SharedPreferences preferences;
    private final AudioManager audio;
    private final NotificationManager notifications;
    private ToneGenerator generator;
    private boolean active;
    private long lastPlayed = -1500L;

    public ChatSounds(Context context, SharedPreferences preferences) {
        this.preferences = preferences;
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        notifications = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void start() {
        active = true;
    }

    public void messageReceived() {
        if (preferences.getBoolean(MESSAGE_PREF, false)) play(ToneGenerator.TONE_PROP_BEEP, 180);
    }

    public void wizzReceived() {
        if (preferences.getBoolean(WIZZ_PREF, false)) play(ToneGenerator.TONE_PROP_ACK, 350);
    }

    private void play(int tone, int duration) {
        if (!active || audio == null || notifications == null) return;
        try {
            if (audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL
                || audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0
                || notifications.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) return;
            long now = SystemClock.elapsedRealtime();
            if (now - lastPlayed < 1500L) return;
            if (generator == null) generator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60);
            if (generator.startTone(tone, duration)) lastPlayed = now;
        } catch (RuntimeException ignored) {
            // Optional audio must not interrupt messaging on devices without working audio.
            release();
        }
    }

    public void stop() {
        active = false;
        release();
    }

    private void release() {
        ToneGenerator previous = generator;
        generator = null;
        if (previous != null) {
            try {
                previous.release();
            } catch (RuntimeException ignored) {
                // Release remains best effort when the device audio service fails.
            }
        }
    }
}
