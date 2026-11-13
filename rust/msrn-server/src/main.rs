// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

mod auth;

use std::{env, net::SocketAddr, path::Path};

use axum::{extract::{DefaultBodyLimit, State}, http::StatusCode, routing::{get, post}, Json, Router};
use msrn_protocol::HealthResponse;
use sqlx::{postgres::PgPoolOptions, PgPool};
use tracing_subscriber::EnvFilter;

async fn health(State(pool): State<PgPool>) -> Result<Json<HealthResponse>, StatusCode> {
    sqlx::query_scalar::<_, i32>("SELECT 1")
        .fetch_one(&pool)
        .await
        .map_err(|_| StatusCode::SERVICE_UNAVAILABLE)?;

    Ok(Json(HealthResponse {
        status: "ok".into(),
        service: "msrn-server".into(),
    }))
}

fn app(pool: PgPool) -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/auth/register", post(auth::register))
        .route("/auth/login", post(auth::login))
        .route("/auth/me", get(auth::me))
        .route("/auth/logout", post(auth::logout))
        .layer(DefaultBodyLimit::max(4096))
        .with_state(pool)
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()))
        .init();

    let database_url = env::var("DATABASE_URL")
        .map_err(|_| "DATABASE_URL must be set (see docs/SETUP.md)")?;
    let pool = PgPoolOptions::new()
        .max_connections(5)
        .connect(&database_url)
        .await?;

    // Run from the repository root. Migration paths are relative to that root.
    let migrator = sqlx::migrate::Migrator::new(Path::new("database/migrations")).await?;
    migrator.run(&pool).await?;

    let address: SocketAddr = env::var("MSRN_BIND")
        .unwrap_or_else(|_| "127.0.0.1:3000".into())
        .parse()?;
    let listener = tokio::net::TcpListener::bind(address).await?;
    tracing::info!(%address, "MSRN server listening");
    axum::serve(listener, app(pool)).await?;
    Ok(())
}
