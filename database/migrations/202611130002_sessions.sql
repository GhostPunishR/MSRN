-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash BYTEA NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT session_expiry_after_creation CHECK (expires_at > created_at),
    CONSTRAINT token_hash_sha256_length CHECK (octet_length(token_hash) = 32)
);

CREATE INDEX user_sessions_user_id_idx ON user_sessions(user_id);
CREATE INDEX user_sessions_expires_at_idx ON user_sessions(expires_at);
