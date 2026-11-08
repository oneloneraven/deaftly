# 5. Store templates and settings with DataStore and JSON

## Status

Accepted

## Context

The app stores a few dozen templates and a handful of settings. Chats are
deliberately not stored: they are discarded when they end. There are no
queries, joins or large data sets.

## Decision

Store templates and settings in Jetpack DataStore as versioned JSON,
serialised with kotlinx.serialization. Schema changes are handled by the
serialiser with tested migrations. Chats live in memory only. No Room or
SQLite.

## Screening

DataStore is part of Jetpack (Google), allowed under the free-tool
exception as in [ADR-0003](0003-build-a-native-android-app-with-kotlin-and-compose.md).
kotlinx.serialization is JetBrains' and is not listed.

## Consequences

- One small, atomic store with no SQL, schema files or DAO layer.
- Whole-document reads and writes, which is fine at this size and would
  need revisiting if templates grew into the thousands.
