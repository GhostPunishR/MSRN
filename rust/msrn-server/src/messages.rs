// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use uuid::Uuid;

use crate::auth::current_user;

#[derive(Serialize)]
pub struct Conversation {
    pub id: Uuid,
    pub contact_id: Uuid,
}

#[derive(Deserialize)]
pub struct SendMessage {
    pub body: String,
}

#[derive(Serialize)]
pub struct Message {
    pub id: Uuid,
    pub sender_id: Uuid,
    pub body: String,
    pub created_at: String,
}

async fn ensure_member(pool: &PgPool, conversation_id: Uuid, user_id: Uuid) -> Result<(), StatusCode> {
    let allowed: Option<(Uuid,)> = sqlx::query_as(
        "SELECT conversation_id FROM conversation_members WHERE conversation_id = $1 AND user_id = $2",
    )
    .bind(conversation_id)
    .bind(user_id)
    .fetch_optional(pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    allowed.map(|_| ()).ok_or(StatusCode::NOT_FOUND)
}

pub async fn open_direct(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Path(username): Path<String>,
) -> Result<Json<Conversation>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    let other: Option<(Uuid,)> = sqlx::query_as("SELECT id FROM users WHERE username = $1")
        .bind(username)
        .fetch_optional(&pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let (other_id,) = other.ok_or(StatusCode::NOT_FOUND)?;
    if other_id == user.id {
        return Err(StatusCode::BAD_REQUEST);
    }

    let friends: Option<(Uuid,)> = sqlx::query_as(
        "SELECT friend_id FROM friendships WHERE user_id = $1 AND friend_id = $2",
    )
    .bind(user.id)
    .bind(other_id)
    .fetch_optional(&pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if friends.is_none() {
        return Err(StatusCode::FORBIDDEN);
    }

    let (low, high) = if user.id < other_id { (user.id, other_id) } else { (other_id, user.id) };
    let existing: Option<(Uuid,)> = sqlx::query_as(
        "SELECT conversation_id FROM direct_conversations WHERE user_low = $1 AND user_high = $2",
    )
    .bind(low).bind(high)
    .fetch_optional(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if let Some((id,)) = existing {
        return Ok(Json(Conversation { id, contact_id: other_id }));
    }

    let mut tx = pool.begin().await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let conversation_id = Uuid::new_v4();
    sqlx::query("INSERT INTO conversations (id) VALUES ($1)")
        .bind(conversation_id)
        .execute(&mut *tx).await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let result = sqlx::query(
        "INSERT INTO direct_conversations (user_low, user_high, conversation_id) \
         VALUES ($1, $2, $3) ON CONFLICT (user_low, user_high) DO NOTHING",
    )
    .bind(low).bind(high).bind(conversation_id)
    .execute(&mut *tx).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if result.rows_affected() == 0 {
        tx.rollback().await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        let (id,): (Uuid,) = sqlx::query_as(
            "SELECT conversation_id FROM direct_conversations WHERE user_low = $1 AND user_high = $2",
        )
        .bind(low).bind(high)
        .fetch_one(&pool).await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        return Ok(Json(Conversation { id, contact_id: other_id }));
    }
    sqlx::query(
        "INSERT INTO conversation_members (conversation_id, user_id) VALUES ($1, $2), ($1, $3)",
    )
    .bind(conversation_id).bind(user.id).bind(other_id)
    .execute(&mut *tx).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    tx.commit().await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(Conversation { id: conversation_id, contact_id: other_id }))
}

pub async fn history(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Path(conversation_id): Path<Uuid>,
) -> Result<Json<Vec<Message>>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    ensure_member(&pool, conversation_id, user.id).await?;
    let rows: Vec<(Uuid, Uuid, String, String)> = sqlx::query_as(
        "SELECT id, sender_id, body, created_at::text FROM ( \
         SELECT id, sender_id, body, created_at FROM messages \
         WHERE conversation_id = $1 ORDER BY created_at DESC, id DESC LIMIT 100 \
         ) recent ORDER BY created_at ASC, id ASC",
    )
    .bind(conversation_id)
    .fetch_all(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(rows.into_iter().map(|(id, sender_id, body, created_at)| Message {
        id, sender_id, body, created_at
    }).collect()))
}

pub async fn send(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Path(conversation_id): Path<Uuid>,
    Json(input): Json<SendMessage>,
) -> Result<(StatusCode, Json<Message>), StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    if input.body.trim().is_empty() || input.body.chars().count() > 4000 {
        return Err(StatusCode::BAD_REQUEST);
    }
    ensure_member(&pool, conversation_id, user.id).await?;
    let id = Uuid::new_v4();
    let (created_at,): (String,) = sqlx::query_as(
        "INSERT INTO messages (id, conversation_id, sender_id, body) \
         VALUES ($1, $2, $3, $4) RETURNING created_at::text",
    )
    .bind(id).bind(conversation_id).bind(user.id).bind(&input.body)
    .fetch_one(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok((StatusCode::CREATED, Json(Message {
        id, sender_id: user.id, body: input.body, created_at
    })))
}
