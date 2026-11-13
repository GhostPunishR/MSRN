// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.chat;

/**
 * Small, offline emoji palette and legacy chat shortcut rendering.
 * Original message text remains unchanged on the server.
 */
public final class EmojiSupport {
    public static final String[] EMOJI = {
        "🙂", "😀", "😄", "😉", "😍", "😎", "😂", "😢",
        "😮", "😡", "😴", "❤️", "💔", "👍", "👋", "🎉",
        "⭐", "🐱", "💜", "🎮"
    };

    private EmojiSupport() { }

    public static String display(String text) {
        if (text == null || text.isEmpty()) return "";
        // Replace longer tokens first to avoid partial matches.
        return text.replace(":'(", "😢")
            .replace(":-D", "😄")
            .replace(":-)", "🙂")
            .replace(";-)", "😉")
            .replace(":-(", "🙁")
            .replace(":-P", "😛")
            .replace(":D", "😄")
            .replace(":)", "🙂")
            .replace(";)", "😉")
            .replace(":(", "🙁")
            .replace(":P", "😛")
            .replace("<3", "❤️");
    }
}
