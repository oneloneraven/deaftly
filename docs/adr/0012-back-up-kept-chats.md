# 12. Back up kept chats

## Status

Accepted. Supersedes [ADR-0008](0008-allow-android-backup-of-templates-and-settings.md).

## Context

Side by side chats used to be discarded when they ended. People want to keep
some: to look back at what someone told them, and to carry on a chat with a
friend across sessions (#62). A kept chat is only useful over time if it
survives a phone change, the same reason templates are backed up.

Kept chats can hold health conversations, so including them in backup is a
bigger step than templates and settings.

## Decision

The user chooses which chats to keep when a chat ends. Each kept chat is one
file in the app's private `chats/` folder. Android Auto Backup and
device-to-device transfer include the template and settings store and the
`chats/` folder, through explicit backup and data-extraction rules. Nothing
else is backed up. The backup is the user's own: they control it in system
settings, and Android encrypts it.

## Consequences

- Kept chats survive a phone change for users with backup turned on.
- The user's Android backup can hold conversations about health, names and
  routines. The record of processing, the threat model and the privacy page
  say so.
- Chats the user doesn't keep are never written to storage.
- Deleting a kept chat removes its file at once; it leaves the backup at
  Android's next backup.
- The rules must be updated whenever the store's or the chats' location
  changes.
