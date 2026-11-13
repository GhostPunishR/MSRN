// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

use std::sync::OnceLock;

use axum::{
    extract::{ws::{Message as WsMessage, WebSocket, WebSocketUpgrade}, Path, State},
    http::{HeaderMap, StatusCode},
    response::Response,
};
use serde::{Deserialize, Serialize};
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

#[derive(Clone, Serialize)]
struct TypingEvent {
    #[serde(rename = "type")]
    kind: &'static str,
    conversation_id: Uuid,
    sender_id: Uuid,
    typing: bool,
}

#[derive(Clone)]
#[derive(Clone, Serialize)]
struct WizzEvent {
    #[serde(rename = "type")]
    kind: &'static str,
    conversation_id: Uuid,
    sender_id: Uuid,
}

enum LiveEvent { Message(MessageEvent), Typing(TypingEvent), Wizz(WizzEvent) }

#[derive(Deserialize)]
struct TypingInput {
    #[serde(rename = "type")]
    kind: String,
    typing: bool,
}

static EVENTS: OnceLock<broadcast::Sender<LiveEvent>> = OnceLock::new();

fn events() -> &'static broadcast::Sender<LiveEvent> {
    EVENTS.get_or_init(|| broadcast::channel(1024).0)
}

pub fn notify(conversation_id: Uuid, message_id: Uuid) {
    let _ = events().send(LiveEvent::Message(MessageEvent {
        kind: "message_created",
        conversation_id,
        message_id,
    }));
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
    Ok(upgrade.on_upgrade(move |socket| stream(socket, receiver, conversation_id, user.id)))
}

async fn stream(
    mut socket: WebSocket,
    mut receiver: broadcast::Receiver<LiveEvent>,
    conversation_id: Uuid,
    user_id: Uuid,
) {
    let mut last_typing = std::time::Instant::now() - std::time::Duration::from_secs(3);
    let mut last_wizz = std::time::Instant::now() - std::time::Duration::from_secs(15);
    loop {
        tokio::select! {
            event = receiver.recv() => {
                match event {
                    Ok(LiveEvent::Message(event)) if event.conversation_id == conversation_id => {
                        let Ok(json) = serde_json::to_string(&event) else { break; };
                        if socket.send(WsMessage::Text(json.into())).await.is_err() { break; }
                    }
                    Ok(LiveEvent::Typing(event)) if event.conversation_id == conversation_id && event.sender_id != user_id => {
                        let Ok(json) = serde_json::to_string(&event) else { break; };
                        if socket.send(WsMessage::Text(json.into())).await.is_err() { break; }
                    }
                    Ok(LiveEvent::Wizz(event)) if event.conversation_id == conversation_id && event.sender_id != user_id => {
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
                    Some(Ok(WsMessage::Text(text))) => {
                        if text.len() > 128 { continue; }
                        if text.as_str() == r#"{"type":"wizz"}"# {
                            if last_wizz.elapsed() >= std::time::Duration::from_secs(10) {
                                last_wizz = std::time::Instant::now();
                                let _ = events().send(LiveEvent::Wizz(WizzEvent {
                                    kind: "wizz", conversation_id, sender_id: user_id,
                                }));
                            }
                            continue;
                        }
                        if let Ok(input) = serde_json::from_str::<TypingInput>(&text) {
                            if input.kind == "typing" && last_typing.elapsed().as_millis() >= 700 {
                                last_typing = std::time::Instant::now();
                                let _ = events().send(LiveEvent::Typing(TypingEvent {
                                    kind: "typing", conversation_id, sender_id: user_id, typing: input.typing,
                                }));
                            }
                        }
                    }
                    _ => {} // Message sending stays on authenticated HTTP.
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
