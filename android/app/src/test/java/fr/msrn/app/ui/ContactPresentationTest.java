// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.ui;

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ContactPresentationTest {
    @Test public void searchesNameUsernameAndPersonalMessageIgnoringAccentsAndCase() {
        assertTrue(ContactPresentation.matches("  elODIE ", "Élodie Martin", "elo", "À bientôt"));
        assertTrue(ContactPresentation.matches("@ELO", "Élodie", "elo", ""));
        assertTrue(ContactPresentation.matches("bientot", "Élodie", "elo", "À bientôt"));
        assertFalse(ContactPresentation.matches("hugo", "Élodie", "elo", "À bientôt"));
    }

    @Test public void acceptsEmptyQueryAndMissingOptionalFields() {
        assertTrue(ContactPresentation.matches(null, null, null, null));
        assertTrue(ContactPresentation.matches("  ", null, null, null));
        assertFalse(ContactPresentation.matches("alice", null, null, null));
    }

    @Test public void handlesDecomposedUnicodeAndEmoji() {
        assertTrue(ContactPresentation.matches("Élodie", "E\u0301lodie", "elo", ""));
        assertTrue(ContactPresentation.matches("🎮", "Alex", "alex", "Soirée 🎮"));
    }

    @Test public void searchDoesNotDependOnDeviceLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertTrue(ContactPresentation.matches("IRIS", "Iris", "iris", ""));
        } finally { Locale.setDefault(previous); }
    }

    @Test public void groupsOnlyKnownConnectedStatesAndKeepsInvisibleOffline() {
        for (String state : new String[]{"online", "away", "busy"}) assertTrue(ContactPresentation.connected(state));
        for (String state : new String[]{null, "", "invisible", "offline", "unknown"}) {
            assertFalse(ContactPresentation.connected(state));
            assertEquals("Hors ligne", ContactPresentation.presenceLabel(state));
        }
        assertEquals("En ligne", ContactPresentation.presenceLabel("online"));
        assertEquals("Absent", ContactPresentation.presenceLabel("away"));
        assertEquals("Occupé", ContactPresentation.presenceLabel("busy"));
    }

    @Test public void capsVisualUnreadCountWithoutInventingNegativeCounts() {
        assertEquals("", ContactPresentation.unreadLabel(-1));
        assertEquals("", ContactPresentation.unreadLabel(0));
        assertEquals("1", ContactPresentation.unreadLabel(1));
        assertEquals("99", ContactPresentation.unreadLabel(99));
        assertEquals("99+", ContactPresentation.unreadLabel(100));
        assertEquals("99+", ContactPresentation.unreadLabel(Integer.MAX_VALUE));
    }
}
