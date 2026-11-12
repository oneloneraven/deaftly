# 4. The app has no network access

## Status

Superseded by [ADR-0011](0011-review-permissions-against-an-allowlist.md)

## Context

Deaftly has no backend, no accounts and no sync. Everything it does
(templates, show mode, chat, text recognition) works on the device. The
requirements rule out analytics and telemetry, and some libraries send
usage data by default (for example, Google's ML Kit uploads diagnostics).

## Decision

The app does not request the `INTERNET` permission. The release build's
merged manifest is checked in CI, and the build fails if any dependency
adds `INTERNET` or any permission other than `CAMERA`. A library that
merges `INTERNET` in has it removed with `tools:node="remove"`.

## Consequences

- No personal data can leave the device through the app, whatever its
  libraries try to do, so the record of processing has no processors.
- Features that need the network (speech-to-text services, sync, model
  downloads) are ruled out until this decision is superseded.
- Libraries that misbehave without network access must be caught in
  testing, not assumed safe.
