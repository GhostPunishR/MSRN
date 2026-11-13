-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

ALTER TABLE users ADD COLUMN preferred_presence TEXT NOT NULL DEFAULT 'online'
    CHECK (preferred_presence IN ('online', 'away', 'busy', 'invisible', 'offline'));

ALTER TABLE user_sessions ADD COLUMN last_seen_at TIMESTAMPTZ;
CREATE INDEX user_sessions_presence_idx ON user_sessions(user_id, last_seen_at)
    WHERE revoked_at IS NULL;
