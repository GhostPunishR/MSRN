// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.ui;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Display-only contact search and presence labels, independent of server identity. */
public final class ContactPresentation {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private ContactPresentation() { }

    public static boolean matches(String query, String name, String username, String personalMessage) {
        String needle = normalize(query).trim();
        return needle.isEmpty() || normalize(safe(name) + " @" + safe(username) + " " + safe(personalMessage)).contains(needle);
    }

    public static boolean connected(String presence) {
        return "online".equals(presence) || "away".equals(presence) || "busy".equals(presence);
    }

    public static String presenceLabel(String presence) {
        if ("online".equals(presence)) return "En ligne";
        if ("away".equals(presence)) return "Absent";
        if ("busy".equals(presence)) return "Occupé";
        return "Hors ligne";
    }

    public static String unreadLabel(int count) {
        return count <= 0 ? "" : count > 99 ? "99+" : String.valueOf(count);
    }

    private static String normalize(String value) {
        return MARKS.matcher(Normalizer.normalize(safe(value), Normalizer.Form.NFD))
            .replaceAll("").toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
