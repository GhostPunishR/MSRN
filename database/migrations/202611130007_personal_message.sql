-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

ALTER TABLE users ADD COLUMN personal_message TEXT NOT NULL DEFAULT ''
    CONSTRAINT users_personal_message_length CHECK (char_length(personal_message) <= 160);
