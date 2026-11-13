// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.chat;

import org.junit.Test;
import static org.junit.Assert.*;

public final class EmojiSupportTest {
    @Test public void preservesPlainTextAndEmptyValues() {
        assertEquals("", EmojiSupport.display(null));
        assertEquals("", EmojiSupport.display(""));
        assertEquals("Bonjour MSRN", EmojiSupport.display("Bonjour MSRN"));
    }

    @Test public void rendersLegacySmileyShortcutsWithoutChangingOtherText() {
        assertEquals("Salut 🙂 😄 😉 ❤️", EmojiSupport.display("Salut :) :D ;) <3"));
        assertEquals("😢", EmojiSupport.display(":'("));
        assertEquals("😄", EmojiSupport.display(":-D"));
    }

    @Test public void paletteHasUniqueNonEmptyEntries() {
        java.util.HashSet<String> entries = new java.util.HashSet<>();
        for (String emoji : EmojiSupport.EMOJI) {
            assertFalse(emoji.isEmpty());
            assertTrue("Duplicate emoji: " + emoji, entries.add(emoji));
        }
        assertEquals(20, entries.size());
    }
}
