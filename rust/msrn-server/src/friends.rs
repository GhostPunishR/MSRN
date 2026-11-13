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

#[derive(Deserialize)]
pub struct Invite {
    username: String,
}

#[derive(Serialize)]
pub struct Contact {
    id: Uuid,
    username: String,
    display_name: String,
}

#[derive(Serialize)]
pub struct PendingRequest {
    id: Uuid,
    sender: Contact,
}

pub async fn contacts(
    State(pool): State<PgPool>,
    headers: HeaderMap,
) -> Result<Json<Vec<Contact>>, StatusCode> {
    let (_, current) = current_user(&pool, &headers).await?;
    let rows: Vec<(Uuid, String, String)> = sqlx::query_as(
        "SELECT u.id, u.username, u.display_name FROM friendships f \
         JOIN users u ON u.id = f.friend_id WHERE f.user_id = $1 ORDER BY u.username LIMIT 200",
    )
    .bind(current.id)
    .fetch_all(&pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(rows.into_iter().map(|(id, username, display_name)| Contact {
        id, username, display_name
    }).collect()))
}

pub async fn incoming(
    State(pool): State<PgPool>,
    headers: HeaderMap,
) -> Result<Json<Vec<PendingRequest>>, StatusCode> {
    let (_, current) = current_user(&pool, &headers).await?;
    let rows: Vec<(Uuid, Uuid, String, String)> = sqlx::query_as(
        "SELECT r.id, u.id, u.username, u.display_name FROM friend_requests r \
         JOIN users u ON u.id = r.sender_id WHERE r.recipient_id = $1 \
         ORDER BY r.created_at DESC LIMIT 100",
    )
    .bind(current.id)
    .fetch_all(&pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(rows.into_iter().map(|(id, user_id, username, display_name)| PendingRequest {
        id,
        sender: Contact { id: user_id, username, display_name },
    }).collect()))
}

pub async fn send_request(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Json(input): Json<Invite>,
) -> Result<StatusCode, StatusCode> {
    let (_, current) = current_user(&pool, &headers).await?;
    if input.username.len() < 3 || input.username.len() > 40 ||
        !input.username.bytes().all(|b| b.is_ascii_lowercase() || b.is_ascii_digit() || b == b'_')
    {
        return Err(StatusCode::BAD_REQUEST);
    }
    let recipient: Option<(Uuid,)> = sqlx::query_as("SELECT id FROM users WHERE username = $1")
        .bind(&input.username)
        .fetch_optional(&pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let (recipient_id,) = recipient.ok_or(StatusCode::NOT_FOUND)?;
    if recipient_id == current.id {
        return Err(StatusCode::BAD_REQUEST);
    }
    let existing: Option<(Uuid,)> = sqlx::query_as(
        "SELECT friend_id FROM friendships WHERE user_id = $1 AND friend_id = $2",
    )
    .bind(current.id).bind(recipient_id)
    .fetch_optional(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if existing.is_some() {
        return Err(StatusCode::CONFLICT);
    }
    let result = sqlx::query(
        "INSERT INTO friend_requests (id, sender_id, recipient_id) \
         VALUES ($1, $2, $3) ON CONFLICT (sender_id, recipient_id) DO NOTHING",
    )
    .bind(Uuid::new_v4()).bind(current.id).bind(recipient_id)
    .execute(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if result.rows_affected() == 0 {
        return Err(StatusCode::CONFLICT);
    }
    Ok(StatusCode::CREATED)
}

pub async fn accept_request(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Path(request_id): Path<Uuid>,
) -> Result<StatusCode, StatusCode> {
    let (_, current) = current_user(&pool, &headers).await?;
    let mut tx = pool.begin().await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let request: Option<(Uuid,)> = sqlx::query_as(
        "SELECT sender_id FROM friend_requests WHERE id = $1 AND recipient_id = $2 FOR UPDATE",
    )
    .bind(request_id).bind(current.id)
    .fetch_optional(&mut *tx).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let (sender_id,) = request.ok_or(StatusCode::NOT_FOUND)?;

    sqlx::query(
        "INSERT INTO friendships (user_id, friend_id) VALUES ($1, $2), ($2, $1) \
         ON CONFLICT (user_id, friend_id) DO NOTHING",
    )
    .bind(current.id).bind(sender_id)
    .execute(&mut *tx).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    sqlx::query(
        "DELETE FROM friend_requests WHERE (sender_id = $1 AND recipient_id = $2) \
         OR (sender_id = $2 AND recipient_id = $1)",
    )
    .bind(current.id).bind(sender_id)
    .execute(&mut *tx).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    tx.commit().await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}

pub async fn decline_request(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Path(request_id): Path<Uuid>,
) -> Result<StatusCode, StatusCode> {
    let (_, current) = current_user(&pool, &headers).await?;
    let result = sqlx::query(
        "DELETE FROM friend_requests WHERE id = $1 AND recipient_id = $2",
    )
    .bind(request_id).bind(current.id)
    .execute(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if result.rows_affected() == 0 {
        return Err(StatusCode::NOT_FOUND);
    }
    Ok(StatusCode::NO_CONTENT)
}
