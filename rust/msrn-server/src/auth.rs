// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

use argon2::{
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use axum::{
    extract::{Json, State},
    http::{header::AUTHORIZATION, HeaderMap, StatusCode},
};
use rand::{rngs::OsRng, RngCore};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Deserialize)]
pub struct Credentials {
    pub username: String,
    pub password: String,
}

#[derive(Deserialize)]
pub struct Registration {
    pub username: String,
    pub display_name: String,
    pub password: String,
}

#[derive(Serialize)]
pub struct PublicUser {
    pub id: Uuid,
    pub username: String,
    pub display_name: String,
}

#[derive(Serialize)]
pub struct AuthResponse {
    pub user: PublicUser,
    pub token: String,
    pub expires_in_seconds: i64,
}

const SESSION_LIFETIME_SECONDS: i64 = 60 * 60 * 24 * 7;
const MAX_PASSWORD_BYTES: usize = 128;

fn valid_username(value: &str) -> bool {
    (3..=40).contains(&value.len())
        && value.bytes().all(|b| b.is_ascii_lowercase() || b.is_ascii_digit() || b == b'_')
}

fn valid_password(value: &str) -> bool {
    (12..=MAX_PASSWORD_BYTES).contains(&value.len())
}

fn valid_display_name(value: &str) -> bool {
    !value.trim().is_empty()
        && value.chars().count() <= 80
        && !value.chars().any(char::is_control)
}

fn hash_password(password: &str) -> Result<String, StatusCode> {
    let salt = SaltString::generate(&mut OsRng);
    Argon2::default()
        .hash_password(password.as_bytes(), &salt)
        .map(|hash| hash.to_string())
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)
}

fn verify_password(password: &str, hash: &str) -> bool {
    PasswordHash::new(hash)
        .ok()
        .and_then(|parsed| Argon2::default().verify_password(password.as_bytes(), &parsed).ok())
        .is_some()
}

fn token_digest(token: &str) -> [u8; 32] {
    Sha256::digest(token.as_bytes()).into()
}

async fn create_session(pool: &PgPool, user: PublicUser) -> Result<Json<AuthResponse>, StatusCode> {
    let mut random = [0_u8; 32];
    OsRng.fill_bytes(&mut random);
    let token = hex::encode(random);
    let digest = token_digest(&token);
    sqlx::query(
        "INSERT INTO user_sessions (id, user_id, token_hash, expires_at) \
         VALUES ($1, $2, $3, NOW() + INTERVAL '7 days')",
    )
    .bind(Uuid::new_v4())
    .bind(user.id)
    .bind(digest.as_slice())
    .execute(pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok(Json(AuthResponse {
        user,
        token,
        expires_in_seconds: SESSION_LIFETIME_SECONDS,
    }))
}

pub async fn register(
    State(pool): State<PgPool>,
    Json(input): Json<Registration>,
) -> Result<(StatusCode, Json<AuthResponse>), StatusCode> {
    if !valid_username(&input.username)
        || !valid_password(&input.password)
        || !valid_display_name(&input.display_name)
    {
        return Err(StatusCode::BAD_REQUEST);
    }

    let password = input.password;
    let password_hash = tokio::task::spawn_blocking(move || hash_password(&password))
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)??;

    let id = Uuid::new_v4();
    let result = sqlx::query(
        "INSERT INTO users (id, username, display_name, password_hash) \
         VALUES ($1, $2, $3, $4) ON CONFLICT (username) DO NOTHING",
    )
    .bind(id)
    .bind(&input.username)
    .bind(input.display_name.trim())
    .bind(password_hash)
    .execute(&pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    if result.rows_affected() == 0 {
        return Err(StatusCode::CONFLICT);
    }

    let user = PublicUser {
        id,
        username: input.username,
        display_name: input.display_name.trim().to_owned(),
    };
    Ok((StatusCode::CREATED, create_session(&pool, user).await?))
}

pub async fn login(
    State(pool): State<PgPool>,
    Json(input): Json<Credentials>,
) -> Result<Json<AuthResponse>, StatusCode> {
    if !valid_username(&input.username) || !valid_password(&input.password) {
        return Err(StatusCode::UNAUTHORIZED);
    }

    let row: Option<(Uuid, String, String, String)> = sqlx::query_as(
        "SELECT id, username, display_name, password_hash FROM users WHERE username = $1",
    )
    .bind(&input.username)
    .fetch_optional(&pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let Some((id, username, display_name, password_hash)) = row else {
        return Err(StatusCode::UNAUTHORIZED);
    };

    let password = input.password;
    let valid = tokio::task::spawn_blocking(move || verify_password(&password, &password_hash))
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if !valid {
        return Err(StatusCode::UNAUTHORIZED);
    }

    create_session(&pool, PublicUser { id, username, display_name }).await
}

async fn current_user(pool: &PgPool, headers: &HeaderMap) -> Result<(Uuid, PublicUser), StatusCode> {
    let value = headers
        .get(AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .ok_or(StatusCode::UNAUTHORIZED)?;
    let token = value.strip_prefix("Bearer ").ok_or(StatusCode::UNAUTHORIZED)?;
    if token.len() != 64 || !token.bytes().all(|b| b.is_ascii_hexdigit()) {
        return Err(StatusCode::UNAUTHORIZED);
    }

    let digest = token_digest(token);
    let row: Option<(Uuid, Uuid, String, String)> = sqlx::query_as(
        "SELECT s.id, u.id, u.username, u.display_name \
         FROM user_sessions s JOIN users u ON u.id = s.user_id \
         WHERE s.token_hash = $1 AND s.revoked_at IS NULL AND s.expires_at > NOW()",
    )
    .bind(digest.as_slice())
    .fetch_optional(pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    row.map(|(session_id, id, username, display_name)| {
        (session_id, PublicUser { id, username, display_name })
    })
    .ok_or(StatusCode::UNAUTHORIZED)
}

pub async fn me(
    State(pool): State<PgPool>,
    headers: HeaderMap,
) -> Result<Json<PublicUser>, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    Ok(Json(user))
}

pub async fn logout(
    State(pool): State<PgPool>,
    headers: HeaderMap,
) -> Result<StatusCode, StatusCode> {
    let (session_id, _) = current_user(&pool, &headers).await?;
    sqlx::query("UPDATE user_sessions SET revoked_at = NOW() WHERE id = $1")
        .bind(session_id)
        .execute(&pool)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    Ok(StatusCode::NO_CONTENT)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validates_usernames() {
        assert!(valid_username("ghost_123"));
        assert!(!valid_username("Ghost"));
        assert!(!valid_username("a"));
        assert!(!valid_username("name with spaces"));
    }

    #[test]
    fn hashes_and_verifies_passwords() {
        let hash = hash_password("a-long-test-password").unwrap();
        assert!(verify_password("a-long-test-password", &hash));
        assert!(!verify_password("wrong-password", &hash));
    }

    #[test]
    fn validates_profile_fields() {
        assert!(valid_display_name("GhostPunishR"));
        assert!(!valid_display_name("  "));
        assert!(!valid_password("short"));
        assert!(valid_password("a-long-test-password"));
    }
}
