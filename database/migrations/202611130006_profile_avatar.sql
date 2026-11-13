-- Copyright (C) 2026 GhostPunishR and contributors
-- SPDX-License-Identifier: AGPL-3.0-only

ALTER TABLE users ADD COLUMN avatar_id SMALLINT NOT NULL DEFAULT 1
    CONSTRAINT users_avatar_id_range CHECK (avatar_id BETWEEN 1 AND 6);
