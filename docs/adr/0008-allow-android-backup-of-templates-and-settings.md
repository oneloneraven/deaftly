# 8. Allow Android backup of templates and settings

## Status

Accepted

## Context

Users build their own templates over time. Without backup, changing or
resetting a phone loses them. The app itself has no network access
([ADR-0004](0004-the-app-has-no-network-access.md)) and runs no sync.

## Decision

Enable Android Auto Backup and device-to-device transfer for the template
and settings store only, with explicit backup and data-extraction rules.
Nothing else is backed up. The backup is the user's own Android backup:
they control it in system settings, and Android encrypts it.

## Consequences

- Templates survive a phone change for users with backup turned on.
- The record of processing lists the user's Android backup as the one way
  data leaves the device, under the user's control.
- The rules must be updated whenever the store's location changes.
