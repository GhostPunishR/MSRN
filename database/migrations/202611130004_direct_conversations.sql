-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

CREATE TABLE direct_conversations (
    user_low UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_high UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL UNIQUE REFERENCES conversations(id) ON DELETE CASCADE,
    PRIMARY KEY (user_low, user_high),
    CONSTRAINT ordered_direct_users CHECK (user_low < user_high)
);

CREATE INDEX messages_sender_idx ON messages(sender_id);
