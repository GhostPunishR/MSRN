// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

use std::sync::OnceLock;

use axum::{
    extract::{ws::{Message as WsMessage, WebSocket, WebSocketUpgrade}, Path, State},
    http::{HeaderMap, StatusCode},
    response::Response,
};
use serde::Serialize;
use sqlx::PgPool;
use tokio::sync::broadcast;
use uuid::Uuid;

use crate::auth::current_user;

#[derive(Clone, Serialize)]
pub struct MessageEvent {
    #[serde(rename = "type")]
    kind: &'static str,
    pub conversation_id: Uuid,
    pub message_id: Uuid,
}

static EVENTS: OnceLock<broadcast::Sender<MessageEvent>> = OnceLock::new();

fn events() -> &'static broadcast::Sender<MessageEvent> {
    EVENTS.get_or_init(|| broadcast::channel(1024).0)
}

pub fn notify(conversation_id: Uuid, message_id: Uuid) {
    let _ = events().send(MessageEvent {
        kind: "message_created",
        conversation_id,
        message_id,
    });
}

pub async fn subscribe(
    State(pool): State<PgPool>,
    headers: HeaderMap,
    Path(conversation_id): Path<Uuid>,
    upgrade: WebSocketUpgrade,
) -> Result<Response, StatusCode> {
    let (_, user) = current_user(&pool, &headers).await?;
    let allowed: Option<(Uuid,)> = sqlx::query_as(
        "SELECT conversation_id FROM conversation_members WHERE conversation_id = $1 AND user_id = $2",
    )
    .bind(conversation_id)
    .bind(user.id)
    .fetch_optional(&pool)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    if allowed.is_none() {
        return Err(StatusCode::NOT_FOUND);
    }

    // Subscribe before completing the handshake to minimize missed notifications.
    let receiver = events().subscribe();
    Ok(upgrade.on_upgrade(move |socket| stream(socket, receiver, conversation_id)))
}

async fn stream(
    mut socket: WebSocket,
    mut receiver: broadcast::Receiver<MessageEvent>,
    conversation_id: Uuid,
) {
    loop {
        tokio::select! {
            event = receiver.recv() => {
                match event {
                    Ok(event) if event.conversation_id == conversation_id => {
                        let Ok(json) = serde_json::to_string(&event) else { break; };
                        if socket.send(WsMessage::Text(json.into())).await.is_err() { break; }
                    }
                    Ok(_) => {}
                    Err(broadcast::error::RecvError::Lagged(_)) => {
                        // Client must reload history after losing events.
                        if socket.send(WsMessage::Text(r#"{"type":"resync_required"}"#.into())).await.is_err() {
                            break;
                        }
                    }
                    Err(broadcast::error::RecvError::Closed) => break,
                }
            }
            incoming = socket.recv() => {
                match incoming {
                    Some(Ok(WsMessage::Close(_))) | None | Some(Err(_)) => break,
                    Some(Ok(WsMessage::Ping(data))) => {
                        if socket.send(WsMessage::Pong(data)).await.is_err() { break; }
                    }
                    _ => {} // Read-only notifications. Message sending stays on authenticated HTTP.
                }
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn serializes_message_notification() {
        let event = MessageEvent {
            kind: "message_created",
            conversation_id: Uuid::nil(),
            message_id: Uuid::nil(),
        };
        let json = serde_json::to_string(&event).unwrap();
        assert!(json.contains("\"type\":\"message_created\""));
    }
}
