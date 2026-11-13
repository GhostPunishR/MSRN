// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.chat;

import java.util.LinkedHashSet;

/** Matches live notifications with senders confirmed by HTTP history. Main thread only. */
public final class MessageSoundQueue {
    private static final int MAX_PENDING = 100;
    private final LinkedHashSet<String> pending = new LinkedHashSet<>();

    public void onMessageCreated(String messageId, boolean alreadyLoaded) {
        if (messageId == null || messageId.isEmpty() || messageId.length() > 128 || alreadyLoaded) return;
        pending.add(messageId);
        if (pending.size() > MAX_PENDING) pending.remove(pending.iterator().next());
    }

    public boolean consume(String messageId, String senderId, String ownUserId) {
        return pending.remove(messageId)
            && senderId != null && !senderId.isEmpty()
            && ownUserId != null && !ownUserId.isEmpty()
            && !senderId.equals(ownUserId);
    }

    public void clear() {
        pending.clear();
    }
}
