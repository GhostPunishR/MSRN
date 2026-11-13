// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.chat;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MessageSoundQueueTest {
    @Test public void alertsOnlyAfterHttpConfirmsAnIncomingLiveMessage() {
        MessageSoundQueue sounds = new MessageSoundQueue();
        sounds.onMessageCreated("incoming", false);
        assertFalse(sounds.consume("history-only", "friend", "me"));
        assertTrue(sounds.consume("incoming", "friend", "me"));
        assertFalse(sounds.consume("incoming", "friend", "me"));
    }

    @Test public void ignoresOwnMessagesAndAlreadyLoadedNotifications() {
        MessageSoundQueue sounds = new MessageSoundQueue();
        sounds.onMessageCreated("own", false);
        sounds.onMessageCreated("cached", true);
        assertFalse(sounds.consume("own", "me", "me"));
        assertFalse(sounds.consume("cached", "friend", "me"));
    }

    @Test public void coalescesDuplicatesAndKeepsIndependentPendingMessages() {
        MessageSoundQueue sounds = new MessageSoundQueue();
        sounds.onMessageCreated("one", false);
        sounds.onMessageCreated("one", false);
        sounds.onMessageCreated("two", false);
        assertTrue(sounds.consume("two", "friend", "me"));
        assertTrue(sounds.consume("one", "friend", "me"));
        assertFalse(sounds.consume("one", "friend", "me"));
        sounds.onMessageCreated("one", true);
        assertFalse(sounds.consume("one", "friend", "me"));
    }

    @Test public void clearsLiveAlertsForReconnectNavigationAndBackgrounding() {
        MessageSoundQueue sounds = new MessageSoundQueue();
        sounds.onMessageCreated("old-connection", false);
        sounds.clear();
        assertFalse(sounds.consume("old-connection", "friend", "me"));
        sounds.onMessageCreated("new-connection", false);
        assertTrue(sounds.consume("new-connection", "friend", "me"));
    }

    @Test public void rejectsMissingIdentitiesWithoutLeavingAnAlertPending() {
        MessageSoundQueue sounds = new MessageSoundQueue();
        String[][] identities = {{null, "me"}, {"", "me"}, {"friend", null}, {"friend", ""}};
        for (String[] pair : identities) {
            sounds.onMessageCreated("message", false);
            assertFalse(sounds.consume("message", pair[0], pair[1]));
            assertFalse(sounds.consume("message", "friend", "me"));
        }
    }

    @Test public void boundsPendingEventsDuringFailedOrSupersededHistoryRequests() {
        MessageSoundQueue sounds = new MessageSoundQueue();
        for (int i = 0; i <= 100; i++) sounds.onMessageCreated("message-" + i, false);
        assertFalse(sounds.consume("message-0", "friend", "me"));
        assertTrue(sounds.consume("message-1", "friend", "me"));
        assertTrue(sounds.consume("message-100", "friend", "me"));
    }

    @Test public void ignoresMalformedMessageIds() {
        MessageSoundQueue sounds = new MessageSoundQueue();
        sounds.onMessageCreated(null, false);
        sounds.onMessageCreated("", false);
        sounds.onMessageCreated("x".repeat(129), false);
        assertFalse(sounds.consume(null, "friend", "me"));
        assertFalse(sounds.consume("", "friend", "me"));
        assertFalse(sounds.consume("x".repeat(129), "friend", "me"));
    }
}
