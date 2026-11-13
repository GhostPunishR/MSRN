// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

use axum::{extract::State, http::{HeaderMap, StatusCode}, Json};
use msrn_protocol::Presence;
use serde::{Deserialize, Serialize};
use sqlx::PgPool;

use crate::auth::current_user;

#[derive(Deserialize)]
pub struct SetPresence {
    pub presence: Presence,
}

#[derive(Serialize)]
pub struct OwnPresence {
    pub presence: Presence,
}

fn visible_presence(preferred: &str, active: bool) -> &'static str {
    if !active || preferred == "invisible" || preferred == "offline" {
        "offline"
    } else {
        match preferred {
            "away" => "away",
            "busy" => "busy",
            _ => "online",
        }
    }
}

pub async fn mine(
    State(pool): State<PgPool>,
    headers: HeaderMap,
) -> Result<Json<OwnPresence>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    let (presence,): (String,) = sqlx::query_as(
        "SELECT preferred_presence FROM users WHERE id = $1",
    )
    .bind(user.id).fetch_one(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let presence = serde_json::from_value(serde_json::Value::String(presence))
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(OwnPresence { presence }))
}

pub async fn set(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Json(input): Json<SetPresence>,
) -> Result<Json<OwnPresence>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    let value = serde_json::to_value(&input.presence)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let value = value.as_str().ok_or(StatusCode::INTERNAL_SERVER_ERROR)?;
    sqlx::query("UPDATE users SET preferred_presence = $1 WHERE id = $2")
        .bind(value).bind(user.id).execute(&pool).await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(OwnPresence { presence: input.presence }))
}

pub async fn heartbeat(
    State(pool): State<PgPool>,
    headers: HeaderMap,
) -> Result<StatusCode, StatusCode> {
    let (session_id, _) = current_user(&pool, &headers).await?;
    sqlx::query("UPDATE user_sessions SET last_seen_at = NOW() WHERE id = $1 AND revoked_at IS NULL")
        .bind(session_id).execute(&pool).await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}

pub(crate) fn contact_presence(preferred: &str, active: bool) -> &'static str {
    visible_presence(preferred, active)
}

#[cfg(test)]
mod tests {
    use super::visible_presence;

    #[test]
    fn invisible_never_leaks() {
        assert_eq!(visible_presence("invisible", true), "offline");
        assert_eq!(visible_presence("busy", false), "offline");
        assert_eq!(visible_presence("away", true), "away");
    }
}
