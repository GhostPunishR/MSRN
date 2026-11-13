# AGENTS.md — MSRN repository rules

## Ownership and scope
- Project owner: **GhostPunishR**.
- Project: **MSRN**, Android-first independent messenger.
- Main stack: Android Java/XML, Rust (Axum/Tokio), PostgreSQL.
- Keep each directory focused on its stated responsibility; do not create empty abstraction layers.

## Non-negotiable: no CI
- **Never create, modify, enable, dispatch, or trigger GitHub Actions workflows or other remote CI jobs.**
- Do not add files under `.github/workflows/`, including placeholders.
- Do not use remote build or test services.
- Run tests/builds locally only when tooling is available; report explicitly when not run.
- Add `[skip ci]` to commit messages as a supplementary precaution, not as a guarantee that external integrations will not run.
- Do not enable automation without explicit owner approval.

## README is frozen
- Treat root `README.md` as an immutable project charter.
- **Do not modify README.md** unless GhostPunishR explicitly asks for that file to change.
- Place setup instructions, implementation progress, decisions, and changing information under `docs/`.

## License and attribution
- License: **AGPL-3.0-only**. Preserve the full root `LICENSE`.
- New original source files must begin with an appropriate comment header:
  `Copyright (C) 2026 GhostPunishR and contributors`
  `SPDX-License-Identifier: AGPL-3.0-only`
- Preserve third-party licenses and provenance. Do not claim ownership of third-party code.
- Do not pretend that a copyright header cryptographically signs a file or commit.
- Use Git signed commits/tags only if the owner has configured an appropriate GPG/SSH signing key. Never create or expose private keys.

## Directory boundaries
- `android/`: Android Java UI, XML resources, Android platform code and network adapters.
- `rust/msrn-server/`: server HTTP/WebSocket endpoints and application orchestration.
- `rust/msrn-protocol/`: shared wire-level contracts and serializable message types.
- `database/migrations/`: append-only SQL migrations; never rewrite deployed migrations.
- `infrastructure/`: local development deployment/configuration; no remote CI.
- `docs/`: evolving guides, roadmap, architecture decisions.

## Security and quality
- No secrets or personal data in Git; use local environment files excluded by gitignore.
- Never log credentials, access tokens, or private messages.
- Validate untrusted input and enforce server-side authorization for every user-scoped operation.
- Keep changes minimal, test locally when possible, and report the exact verification performed.
- Do not imply encryption is end-to-end unless a vetted, reviewed protocol is implemented.
- Prefer small, reviewable changes; avoid unrelated refactors.

## Development workflow
1. Inspect existing code and relevant docs before editing.
2. Define the affected contract and directory ownership.
3. Implement the smallest coherent change.
4. Verify locally (no CI).
5. Summarize changes, tests run, tests not run, and outstanding risks.
