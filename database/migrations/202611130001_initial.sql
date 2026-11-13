-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE friendships (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, friend_id),
    CONSTRAINT no_self_friendship CHECK (user_id <> friend_id)
);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT message_body_length CHECK (char_length(body) BETWEEN 1 AND 4000),
    FOREIGN KEY (conversation_id, sender_id)
        REFERENCES conversation_members(conversation_id, user_id)
);
CREATE INDEX messages_conversation_created_idx
    ON messages(conversation_id, created_at DESC);
