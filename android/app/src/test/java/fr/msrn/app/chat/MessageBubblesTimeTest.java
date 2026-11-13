// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.chat;

import org.junit.Test;
import java.time.ZoneId;
import java.util.TimeZone;
import static org.junit.Assert.*;

public final class MessageBubblesTimeTest {
    @Test public void convertsUtcAndPostgresOffsetsToDeviceTime() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Paris")));
            assertEquals("13:30", MessageBubbles.localTime("2026-01-10 12:30:00+00"));
            assertEquals("13:30", MessageBubbles.localTime("2026-01-10 13:30:00+01"));
            assertEquals("13:30", MessageBubbles.localTime("2026-01-10T12:30:00Z"));
            assertEquals("2026-01-10", MessageBubbles.localDateKey("2026-01-10 12:30:00+0000"));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test public void handlesMidnightAndInvalidTimestamps() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
            assertEquals("2026-01-11", MessageBubbles.localDateKey("2026-01-10 23:30:00+00"));
            assertEquals("", MessageBubbles.localTime("not-a-date"));
            assertEquals("", MessageBubbles.dateLabel("not-a-date"));
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
