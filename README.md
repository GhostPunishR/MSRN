# MSRN

**MSRN** is an independent, open-source instant messaging project inspired by the social experience of classic desktop messengers, designed first for Android.

> Project name is provisional pending trademark clearance. MSRN is not affiliated with or endorsed by Microsoft.

## Vision

Bring back contact lists, presence indicators, personal status messages, expressive profiles, and playful nudges in a clean, modern messaging experience.

## Technology

- **Android:** Java and Android XML layouts.
- **Backend:** Rust, Axum, and Tokio.
- **Database:** PostgreSQL.
- **Shared protocol:** Rust types and documented wire formats.

The architecture favors clear module boundaries, local development, privacy, and incremental delivery. A Rust library embedded in Android is optional and will only be introduced when justified.

## Repository layout

- `android/` - Android application and platform integrations.
- `rust/msrn-server/` - Rust API and realtime service.
- `rust/msrn-protocol/` - Shared protocol contracts.
- `database/migrations/` - Versioned SQL schema changes.
- `infrastructure/` - Local development infrastructure.
- `docs/` - Evolving technical documentation, decisions, and setup instructions.
- `AGENTS.md` - Repository contribution and automation rules.

## Project principles

- Build a safe, usable messaging foundation before expanding features.
- Keep each module responsible for one clearly defined job.
- Treat authentication, authorization, abuse prevention, and user privacy as core requirements.
- Keep development and verification local; **no GitHub Actions or CI workflows** are configured.
- Do not commit credentials, private keys, or production data.
- Keep this README stable; put changing implementation details in `docs/`.

## Status

Early development. No production-ready messaging service is available yet. See `docs/ROADMAP.md` for the evolving implementation plan.

## License and attribution

Copyright (C) 2026 **GhostPunishR** and contributors.

Licensed under the **GNU Affero General Public License v3.0 only (AGPL-3.0-only)**. See [LICENSE](LICENSE).

Source files should carry an SPDX license identifier and appropriate copyright attribution. Cryptographic commit signing is separate and requires a signing key configured by the contributor.

## Disclaimer

This project is independent and is not an official continuation of MSN Messenger. Third-party names, logos, artwork, and sounds are not licensed for reuse by this repository.
