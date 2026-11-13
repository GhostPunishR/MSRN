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

## Synchronized nickname and preset avatar

Authenticated `GET /profile/me` returns `id`, `username`, `display_name`, and `avatar_id`. Authenticated `PUT /profile/me` accepts JSON such as `{"display_name":"Mon pseudo","avatar_id":3}`. The server validates nonempty display names of at most 80 Unicode characters without control characters, and avatar IDs from 1 through 6. Updates are persisted in PostgreSQL.

Android's Profile screen now loads and saves the server-side nickname and a choice of six built-in emoji avatars. Contacts display the synchronized nickname and selected avatar. Existing accounts receive avatar 1 by default. This is a preset-avatar milestone, not user image upload or image hosting. Avatar selection and nickname updates require a signed-in session. Existing `/auth/me` response fields remain unchanged.

Compilation and integration tests remain deferred until a local development environment is available.

## Personal message and conversation profile

Migration `202611130007_personal_message.sql` adds a personal message to user profiles, defaulting to an empty string. The server accepts up to 160 Unicode characters and rejects control characters. An empty string clears the message.

Authenticated `PUT /profile/message` accepts `{"personal_message":"Disponible pour discuter"}` and returns the updated profile. `GET /profile/me` and `PUT /profile/me` also return `personal_message` while preserving the existing nickname and avatar update contract. Accepted contacts expose their personal message through `GET /contacts`.

Android allows editing the personal message on the Profile screen. The contacts list and conversation header show the selected contact's avatar, nickname, presence, and personal message. The conversation header uses the contact snapshot from the contacts list and does not yet receive live profile-change events. No user-provided HTML or image uploads are accepted.

This milestone has not been compiled or integration-tested.

## Ephemeral typing indicators

The authenticated conversation WebSocket now accepts small JSON frames of the form `{"type":"typing","typing":true}` and `{"type":"typing","typing":false}`. The server verifies membership during the WebSocket handshake, attaches the authenticated sender UUID itself, and forwards events only to other subscribers of that conversation. It ignores malformed and oversized frames and rate-limits typing broadcasts to roughly one event per 700 milliseconds per connection. Typing is ephemeral and is never stored in PostgreSQL.

Android sends a typing event at most once per 1.5 seconds during input, sends a stop event after 2.2 seconds without editing, and hides received indicators after 4 seconds even if a stop event was missed. Typing events are not a guarantee that a message will be sent. This implementation uses a process-local broadcast channel, so typing does not yet work between server replicas. Compilation and integration tests remain deferred.

## Remote Wizz prototype

While connected to a conversation WebSocket, Android can send the exact JSON frame `{"type":"wizz"}`. The server associates the event with the authenticated connection, relays it only to other subscribers of that conversation, and enforces a 10-second cooldown per connection. The receiving Android screen shows a transient status, animates a shake, and vibrates when hardware permits. The sender also sees a local confirmation and animation.

Wizz events are ephemeral and are not stored or delivered to offline contacts. They are process-local and do not propagate across multiple server replicas. Cooldowns are per connection, not per user across devices or reconnects. The feature requires a live WebSocket connection and has not yet been compiled or integration-tested.

## Emoji picker and legacy emoticons

The Android chat screen includes a collapsible horizontal palette of 20 Unicode emoji. Selecting an emoji inserts it at the current text cursor or replaces the selected range. The existing 4,000-character message limit remains enforced. Emoji are sent as normal UTF-8 message text through the existing authenticated HTTP message API, without new database columns or server endpoints.

The chat history renders common legacy text shortcuts such as `:-)`, `:)`, `:D`, `;)`, `:(`, `:P`, `:'(`, and `<3` as Unicode emoji. This substitution is display-only: stored message bodies are not rewritten. This is a lightweight original emoji experience, not a licensed MSN asset pack. Compilation and device testing are still pending.

## Chat bubbles and message timestamps

Android renders message history in a vertical collection of text-only bubbles. The signed-in user ID from the authentication response is compared with each message's `sender_id`: own messages align right in blue, and incoming messages align left in grey. The existing display-only emoji conversion still applies. Each bubble shows a short sender label and the `HH:mm` substring from the server-provided `created_at` field. This is server timestamp display, not a conversion into the device's local timezone.

The existing keyset pagination and WebSocket-triggered history refresh still populate the same cached message collection. The view scrolls to the bottom if it was already near the bottom. No server schema or API change is required. Build and emulator verification remain pending.

## Chat scroll retention and local timestamps

When an older message page is prepended, Android records the previous scroll position and history height, re-renders the cached bubbles, and offsets the scroll position by the resulting height difference. This keeps the reader near the same visible messages instead of jumping to the bottom. On ordinary message refreshes, the chat auto-scrolls only if the reader was already near the bottom.

Message bubble timestamps are parsed as offset-aware PostgreSQL `timestamptz::text` values, including `+HH`, `+HHMM`, and `+HH:MM` offsets. Android converts the instant to the device's current timezone before formatting `HH:mm`. Unknown formats show no time instead of an incorrectly labeled local time. No database or HTTP API changes are required. Device testing is still pending.

## Conversation date separators

Android groups adjacent messages by their calendar day after converting server timestamps into the device's current timezone. Each new day gets a centered separator: `Aujourd'hui`, `Hier`, or a localized long date for earlier days. Unparseable timestamps do not produce a potentially misleading separator. Date separators are reconstructed when history refreshes or older messages are loaded, and the existing scroll retention logic remains in place.

No server endpoint or database migration is required. Android compilation and visual verification are still pending.

## Unread message counts

Migration `202611130008_unread_messages.sql` adds a per-member `last_read_at` timestamp, initially negative infinity. `GET /contacts` now returns an `unread_count` integer for each accepted friend. It counts only messages sent by that friend in the existing direct conversation after the authenticated member's last-read timestamp. Contacts without a direct conversation return zero.

Authenticated `POST /conversations/{id}/read` accepts `{"message_id":"<uuid>"}` and returns HTTP 204. The server checks conversation membership and that the referenced message belongs to that conversation; it advances the read timestamp monotonically and never rewinds it. An invalid message ID for the conversation returns 404.

Android displays nonzero unread counts in bold on the contacts list, capped visually at 99+. After successfully loading the latest history page in a foreground chat, it acknowledges the newest message in that page. The list refreshes on the next contacts load. Counts are not pushed live while the contacts screen remains open, and a read acknowledgement does not indicate the user actually scrolled to or read each message. Messages with identical database timestamps may share the same read boundary. The feature has not yet been compiled or integration-tested.

## Foreground unread badge refresh

The Android contacts screen now refreshes its authenticated `GET /contacts` data every 15 seconds while the app is in the foreground. Quiet refreshes avoid clearing the list or showing a loading message, and overlapping refresh requests are skipped. The refresh callback is cancelled when the user leaves Contacts or the activity stops; it is scheduled again on resume when Contacts is visible. This is bounded foreground polling, not a push-based or instant WebSocket notification. It requires no new endpoint, migration, background service, or notification permission.

The server-side broadcast event enum was also corrected to implement `Clone`, as required by Tokio's broadcast channel. Build and integration tests have not been run.

## Targeted regression checks (manual, no CI)

Run these checks locally on a development computer. Do not enable GitHub Actions or remote build services.

- Rust unit tests: from `rust/`, run `cargo test -p msrn-server`. The WebSocket tests cover the serialized message, typing, and Wizz event contracts; existing pagination tests cover page size bounds. Database-backed endpoints still require separate integration tests.
- Android JVM tests: from `android/`, run `gradle :app:testDebugUnitTest` with Gradle 8.9, the Android SDK, and JDK 17 installed. The repository does not contain a Gradle wrapper. The tests cover emoji conversion, palette uniqueness, PostgreSQL timestamp offsets, timezone conversion, and midnight rollover.
- Database integration: start a disposable PostgreSQL instance, apply all migrations in order, register two test accounts, create a friendship and direct conversation, send messages in both directions, verify `GET /contacts` unread counts, then verify `POST /conversations/{id}/read` advances only the caller's read cursor and rejects foreign messages.
- Android device verification: sign in on two devices/emulators, check chat history, unread badges, typing, Wizz, emoji insertion, local dates, and scroll retention when loading older pages.

Known limitations: no local Rust compilation, Android build, PostgreSQL integration, or device run was performed during this repository-only update. The regression tests are committed but not yet validated by execution.

## Verification record: 2026-11-13

A local Java 17 compiler/runtime was available in the assistant's execution environment, but the Rust toolchain, Gradle/Android SDK, PostgreSQL client, and Docker were not. No GitHub Actions, hosted CI, or remote build was used.

Executed successfully using standalone Java harnesses reproducing the checked production methods:
- `javac` and `java`: four assertions on legacy emoji replacement and palette uniqueness passed.
- `javac` and `java`: five offset-aware timestamp parsing and Europe/Paris conversion cases passed, including UTC `Z`, `+00`, `+01`, `+0000`, and a midnight rollover.

These are smoke checks on reproduced Java logic, not execution of the repository's JUnit test classes or an Android build. No Rust unit tests, SQL integration tests, emulator tests, or end-to-end two-client tests were executed. Do not treat this record as a full quality gate pass. Follow the targeted local commands in the previous section when the required toolchains become available.

## Verification record: 2026-11-13 (prepared development environment)

The development environment now has Rust 1.100.0, Temurin JDK 17.0.21, Gradle 8.9, Android SDK 35, build-tools 34.0.0, and a local PostgreSQL 16 container. All commands below ran locally; no GitHub Actions, hosted CI, or remote build service was used.

The first Android build identified an invalid `android:horizontalScrollBarEnabled` attribute in the emoji palette's `HorizontalScrollView`. Replacing it with `android:scrollbars="none"` preserves the intended hidden scrollbar and allows Android resource linking to complete.

Executed successfully:

- `cargo test --manifest-path rust/Cargo.toml --workspace`: all 10 Rust unit tests passed during environment preparation.
- `gradle -p android --no-daemon --max-workers=2 :app:assembleDebug :app:testDebugUnitTest`: the debug APK was generated and all 5 existing JUnit tests passed through Gradle after the XML correction.
- Server startup against local PostgreSQL: all 8 SQL migrations applied successfully, and `GET http://127.0.0.1:3000/health` returned HTTP 200 with `{"status":"ok","service":"msrn-server"}`.

The APK is generated at `android/app/build/outputs/apk/debug/app-debug.apk`; the Android test report is at `android/app/build/reports/tests/testDebugUnitTest/index.html`.

No emulator/device run or end-to-end two-client messaging test was performed. Successful migrations and the health check do not replace database-backed API integration tests.
