-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

ALTER TABLE conversation_members
    ADD COLUMN last_read_at TIMESTAMPTZ NOT NULL DEFAULT '-infinity'::timestamptz;

CREATE INDEX messages_unread_lookup_idx
    ON messages (conversation_id, created_at) INCLUDE (sender_id);
