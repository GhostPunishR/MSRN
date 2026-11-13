# Local development setup

Copyright (C) 2026 GhostPunishR and contributors.
SPDX-License-Identifier: AGPL-3.0-only

No remote CI, GitHub Actions, or issues are required or permitted for this workflow.

## Prerequisites

- Android Studio with Android SDK 35, Android Gradle Plugin compatible JDK 17, and an Android emulator.
- Gradle 8.9 installed locally (or generate a Gradle wrapper locally and commit only after review).
- Rust stable with Cargo.
- Docker with Docker Compose.
- A local terminal with Git.

## 1. PostgreSQL

From the repository root:

```sh
export POSTGRES_PASSWORD='choose-a-local-development-password'
docker compose -f infrastructure/compose.yaml up -d postgres
```

On Windows PowerShell use `$env:POSTGRES_PASSWORD = 'choose-a-local-development-password'`.

The database listens only on localhost:5432. Do not reuse this development password in production.

## 2. Rust API

Run from the repository root so SQL migrations are found:

```sh
export DATABASE_URL='postgres://msrn:choose-a-local-development-password@127.0.0.1:5432/msrn'
cargo run --manifest-path rust/Cargo.toml -p msrn-server
```

PowerShell: `$env:DATABASE_URL = 'postgres://msrn:choose-a-local-development-password@127.0.0.1:5432/msrn'`.

Test in another local terminal:

```sh
curl http://127.0.0.1:3000/health
```

Expected after the database is healthy: `{"status":"ok","service":"msrn-server"}`.

The server runs migrations automatically at startup.

## 3. Android

Open `android/` as a project in Android Studio. Sync Gradle with JDK 17 and Android SDK 35, then run the `app` configuration in an Android emulator.

This repository intentionally does not contain a generated Gradle wrapper yet. Install Gradle 8.9 locally or use Android Studio's Gradle setup to generate a wrapper after checking its distribution source.

The four screens contain local demonstration data. Chat messages and friend invitations are not sent to the server yet.

In Profile, tap `Tester le serveur Rust`. Android emulator address `10.0.2.2` maps to the host machine. The Rust service listens on `127.0.0.1:3000` on that host. HTTP cleartext is allowed in debug builds only. Production must use HTTPS.

## Local verification

```sh
cargo fmt --manifest-path rust/Cargo.toml --all -- --check
cargo test --manifest-path rust/Cargo.toml --workspace
```

Android build (from `android/` with a locally available Gradle):

```sh
gradle :app:assembleDebug
```

No builds or tests should be run on GitHub-hosted runners.

## Scope

This is a foundation milestone, not a production messenger. Authentication, real messaging, authorization, rate limiting, encryption design, and account deletion still need implementation before public release.

## Authentication milestone

Local-only API routes:

- `POST /auth/register` with JSON `{"username":"ghost_123","display_name":"Ghost","password":"a-strong-password"}`.
- `POST /auth/login` with JSON `{"username":"ghost_123","password":"a-strong-password"}`.
- `GET /auth/me` with header `Authorization: Bearer <token>`.
- `POST /auth/logout` with the same bearer header. This revokes the current session.

Usernames must be 3 to 40 lowercase ASCII letters, digits, or underscores. Passwords must contain 12 to 128 UTF-8 bytes. Tokens are random, hashed in PostgreSQL, and expire after seven days.

In Android open **Profil**, then **Mon compte MSRN** to register or sign in against the local Rust server. Sessions currently remain only in application memory and are lost when the app process ends. No production login, HTTPS transport, password recovery, email verification, rate limiting, or account deletion is implemented yet. Never expose this prototype directly to the internet.

## Local API example

```sh
curl -i -X POST http://127.0.0.1:3000/auth/register \\
  -H 'Content-Type: application/json' \\
  -d '{"username":"ghost_123","display_name":"Ghost","password":"a-strong-password"}'
```

## Friends and contacts prototype

With two locally registered accounts, authenticate each user and use the returned bearer token:

- `GET /contacts`: list accepted contacts for the authenticated user.
- `GET /friends/requests`: list incoming invitations.
- `POST /friends/requests`: JSON `{"username":"other_account"}` to invite an existing account.
- `POST /friends/requests/{id}/accept`: accept an incoming invitation.
- `POST /friends/requests/{id}/decline`: decline an incoming invitation.

All routes require `Authorization: Bearer <token>`. Acceptance inserts reciprocal contact relationships in one database transaction. The server applies a maximum of 200 contacts returned and 100 incoming invitations returned per request. Pagination, rate limits, blocking, and public deployment protections are not yet implemented.

Android Contacts and Amis now load real server data when a session is active. Messages and Wizz remain local demonstrations. The Android token remains in process memory only.

## Direct messages milestone

After two users accept a friend request:

1. On Android, open Contacts and tap the contact.
2. The app calls `POST /conversations/with/{username}` to find or create a private direct conversation.
3. `GET /conversations/{id}/messages` loads up to 100 recent persisted messages.
4. `POST /conversations/{id}/messages` with `{"body":"Bonjour !"}` saves a new message.

All endpoints require a bearer token. The server checks that users are friends before opening a direct conversation and checks conversation membership before reading or sending messages. Conversation creation uses a unique unordered pair to prevent duplicate direct conversations. The Android chat displays sender IDs for now, and refresh is manual.

The Wizz button is still a local-only demonstration and does not send a network event. There is no WebSocket delivery, push notification, pagination, message deletion, block enforcement, or end-to-end encryption yet.

This milestone has not been compiled or tested in this session. Run local Rust and Android builds before relying on it. Never deploy this prototype to the public internet.

## Real-time notifications prototype

`GET /conversations/{id}/events` upgrades to an authenticated WebSocket. Clients send the same `Authorization: Bearer <token>` header used for HTTP. Only members of the requested conversation can subscribe.

When `POST /conversations/{id}/messages` successfully stores a message, the Rust server broadcasts a `{"type":"message_created","conversation_id":"...","message_id":"..."}` notification to connected subscribers of that conversation. Android uses the notification to reload the history over HTTP. A `{"type":"resync_required"}` event means the receiver fell behind and must reload history.

The notification bus is process-local and is suitable only for one Rust server process. It does not guarantee delivery while offline, and there is no automatic reconnect yet. Android keeps manual refresh as a fallback. Message content is not included in WebSocket notifications. A production multi-instance deployment needs a durable or cross-instance notification mechanism, session revocation handling for active sockets, WSS, reconnection, and stronger security controls.

Android debug builds permit HTTP and WS to the local emulator host only. Release builds must use HTTPS and WSS with an appropriately configured server.

## Reconnect and resynchronization milestone

The Android WebSocket client now retries transient failures with exponential delays of 1, 2, 4, 8, 16, and at most 30 seconds. It stops retrying when the conversation is closed or the Activity is stopped, and does not retry authorization failures (HTTP 401/403) or unknown conversations (HTTP 404). A successful handshake triggers a full HTTP reload of the most recent 100 messages to cover missed notifications. Lagged WebSocket notifications also trigger a reload. Android resumes the connection when returning to a visible chat.

This is not a durable message queue. If more than 100 messages arrive while offline, older messages are not available through the current history API. A later milestone must add cursor-based pagination, background delivery where appropriate, and reconnection integration tests. Socket authorization is checked at handshake; active sockets are not yet terminated immediately on token revocation.

## Paginated message history

`GET /conversations/{id}/messages/page?limit=50` returns:

```json
{
  "messages": [],
  "next_before": null,
  "has_more": false
}
```

The response is ordered from oldest to newest within each page. A page contains 1 to 100 messages (default 50). When `has_more` is true, pass the returned `next_before` UUID as the `before` query parameter to retrieve older messages. An unknown or cross-conversation cursor returns HTTP 400. Membership is checked before accessing history.

The existing `GET /conversations/{id}/messages` endpoint remains available for compatibility. Android now uses the paginated endpoint and a "Charger les anciens messages" button. Incoming WebSocket notifications reload the latest page and merge it with locally loaded older pages.

Limitations: the Android cache is in memory, not persisted across process restarts. There is no scroll-position preservation, search, retention policy, or full offline history. Messages created with identical timestamps are consistently ordered by UUID as a tie-breaker. The paginated API and Android client still require local compilation and integration testing.

## Presence prototype

The server exposes authenticated `GET /presence/me`, `PUT /presence/me` with JSON `{"presence":"away"}`, and `POST /presence/heartbeat`. Accepted values are `online`, `away`, `busy`, `invisible`, and `offline`. The preference is stored on the user record; each authenticated session has an independent last-seen timestamp.

Android sends a heartbeat every 30 seconds while the Activity is in the foreground and stops when it leaves the foreground. `GET /contacts` returns the effective `presence` of accepted contacts. A contact is considered active if any unrevoked, unexpired session sent a heartbeat within the last 90 seconds. Otherwise the contact appears offline. Invisible and explicitly offline users always appear offline to contacts, even with an active session.

This is a polling-based presence prototype, not push presence updates. Contacts need to refresh the list to see changes. It does not yet include idle detection, a per-device status preference, push notifications, or an administrative presence service. The schema migration is append-only. Compilation and integration tests remain deferred.
