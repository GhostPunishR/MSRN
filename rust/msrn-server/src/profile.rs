// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

use axum::{extract::State, http::{HeaderMap, StatusCode}, Json};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use uuid::Uuid;

use crate::auth::current_user;

#[derive(Serialize)]
pub struct Profile {
    id: Uuid,
    username: String,
    display_name: String,
    avatar_id: i16,
    personal_message: String,
}

#[derive(Deserialize)]
pub struct ProfileUpdate {
    display_name: String,
    avatar_id: i16,
}

fn valid_name(value: &str) -> bool {
    !value.trim().is_empty()
        && value.chars().count() <= 80
        && !value.chars().any(char::is_control)
}

pub async fn mine(
    State(pool): State<PgPool>,
    headers: HeaderMap,
) -> Result<Json<Profile>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    let (avatar_id, personal_message): (i16, String) = sqlx::query_as("SELECT avatar_id, personal_message FROM users WHERE id = $1")
        .bind(user.id).fetch_one(&pool).await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(Profile {
        id: user.id, username: user.username,
        display_name: user.display_name, avatar_id, personal_message,
    }))
}

pub async fn update(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Json(input): Json<ProfileUpdate>,
) -> Result<Json<Profile>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    if !valid_name(&input.display_name) || !(1..=6).contains(&input.avatar_id) {
        return Err(StatusCode::BAD_REQUEST);
    }
    let name = input.display_name.trim().to_owned();
    sqlx::query("UPDATE users SET display_name = $1, avatar_id = $2 WHERE id = $3")
        .bind(&name).bind(input.avatar_id).bind(user.id)
        .execute(&pool).await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let (personal_message,): (String,) = sqlx::query_as("SELECT personal_message FROM users WHERE id = $1")
        .bind(user.id).fetch_one(&pool).await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(Profile {
        id: user.id, username: user.username,
        display_name: name, avatar_id: input.avatar_id, personal_message,
    }))
}


#[derive(Deserialize)]
pub struct PersonalMessageUpdate {
    personal_message: String,
}

pub async fn update_message(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Json(input): Json<PersonalMessageUpdate>,
) -> Result<Json<Profile>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    if input.personal_message.chars().count() > 160
        || input.personal_message.chars().any(char::is_control)
    {
        return Err(StatusCode::BAD_REQUEST);
    }
    let message = input.personal_message.trim().to_owned();
    let (display_name, avatar_id): (String, i16) = sqlx::query_as(
        "UPDATE users SET personal_message = $1 WHERE id = $2 RETURNING display_name, avatar_id",
    )
    .bind(&message).bind(user.id)
    .fetch_one(&pool).await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(Json(Profile {
        id: user.id, username: user.username,
        display_name, avatar_id, personal_message: message,
    }))
}

#[cfg(test)]
mod tests {
    use super::valid_name;

    #[test]
    fn validates_display_names() {
        assert!(valid_name("Nouveau pseudo"));
        assert!(!valid_name("   "));
        assert!(!valid_name("A\nB"));
        assert!(!valid_name(&"x".repeat(81)));
    }
}
