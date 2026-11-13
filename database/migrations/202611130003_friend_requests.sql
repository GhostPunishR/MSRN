-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

CREATE TABLE friend_requests (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT no_self_request CHECK (sender_id <> recipient_id),
    CONSTRAINT unique_directed_request UNIQUE (sender_id, recipient_id)
);
CREATE INDEX friend_requests_recipient_idx ON friend_requests(recipient_id, created_at DESC);
CREATE INDEX friendships_friend_id_idx ON friendships(friend_id);
