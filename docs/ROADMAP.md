# MSRN development roadmap

This document evolves; the root README remains stable.

1. **Foundation** - Android Java project, Rust workspace, local PostgreSQL, health endpoint.
2. **Android UI** - Contacts, Conversation, Friend Requests, Profile.
3. **Identity** - Registration, authentication, session revocation, user profiles.
4. **Messaging** - Direct conversations, persisted and paginated messages, WebSocket notifications, reconnect, and unread counts are implemented. Database-backed integration and two-client validation remain.
5. **Classic experience** - Presence, Wizz, personal statuses, emoji, and opt-in conversation sounds are implemented. Device validation remains.
6. **Public beta readiness** - Abuse controls, reporting, blocking, deletion, privacy, backup and security review.

## Validation
Run builds and tests locally only. Never configure or trigger remote CI.

## Release
Android-first and France-first. MSRN is a provisional name pending trademark review.

## Real-time prototype
Authenticated conversation-scoped WebSocket notifications are wired to Android with bounded reconnect and HTTP resynchronization. Messages remain persisted and loaded over HTTP. Multi-server delivery and integration tests are still pending.

## Optional conversation sounds

Android Profile now offers separate, persistent switches for incoming message and Wizz sounds. Both are off by default. Native Android tones play only in the visible conversation while the app is in the foreground, with notification volume enabled and silent/vibrate/Do Not Disturb modes off. Message senders are confirmed through HTTP history; own messages, duplicate events, history loads, and reconnect resynchronization stay silent. Sounds share a 1.5-second cooldown to limit bursts.

The initial sound milestone built the debug APK and passed 12 Android JVM tests, including 7 live-message alert regression tests. Audible playback, interactive preference persistence, and device audio/lifecycle behavior still need the manual checks in `docs/SETUP.md`.

## Modern interface direction

The [historical interface study](design/MSN_INTERFACE_STUDY.md) compares major Messenger generations and screen families. GhostPunishR selected a modern interpretation of MSN 7.5 and Windows Live Messenger 2009. The five current Android screens now use a shared light/dark palette, native controls, avatar/presence cards, local contact search, selected bottom navigation, and restyled conversation bubbles. The [interactive browser proposal](design/msrn-modern-preview.html) uses fictional data and is separate from native Android validation.

The current debug APK builds and all 18 Android JVM tests pass. The 50 previous widget ID/type contracts are preserved. Native device checks for layout, large fonts, keyboard, orientation, TalkBack and audio remain. Calls, attachments, group chat, arbitrary avatar uploads, favorites and custom scenes require their own feature work before they can appear as working controls.
