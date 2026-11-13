# Architecture and module ownership

## Android
`android/` contains Java Android screens, XML layouts, navigation, platform notifications, and API adapters. UI does not access PostgreSQL directly.

## Rust
`rust/msrn-server/` owns HTTP/WebSocket transport, authentication integration, authorization, and application services.
`rust/msrn-protocol/` owns stable serialized event and message contracts.

## Persistence
`database/migrations/` contains ordered SQL migrations. PostgreSQL is the durable store. Redis is not required for the first milestone.

## Communication
Android -> HTTPS API for commands/history and authenticated WebSocket for live events -> Rust server -> PostgreSQL.

## Rust on Android
JNI is deliberately deferred until a shared native algorithm or engine provides real value. Avoid premature FFI complexity.

## Security boundary
The server verifies identity, permissions, limits, and data validation. Never trust client-declared user IDs or presence.
