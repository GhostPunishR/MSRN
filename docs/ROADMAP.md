# MSRN development roadmap

This document evolves; the root README remains stable.

1. **Foundation** - Android Java project, Rust workspace, local PostgreSQL, health endpoint.
2. **Android UI** - Contacts, Conversation, Friend Requests, Profile.
3. **Identity** - Registration, authentication, session revocation, user profiles.
4. **Messaging** - Direct conversation API and persisted message prototype added. WebSocket delivery, reconnect, and local validation remain.
5. **Classic experience** - Presence, Wizz, personal statuses, optional sounds.
6. **Public beta readiness** - Abuse controls, reporting, blocking, deletion, privacy, backup and security review.

## Validation
Run builds and tests locally only. Never configure or trigger remote CI.

## Release
Android-first and France-first. MSRN is a provisional name pending trademark review.
