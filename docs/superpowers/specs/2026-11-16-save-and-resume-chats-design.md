# Save and resume chats: design

Issue: #62 (Saved and exportable chat history). This design covers saving
and resuming. Exporting and sharing stay in #183 and #188.

## Intent

Side by side chats are thrown away when they end (requirement H7). The owner
wants two things:

1. **Look back.** After a chat with, say, a GP receptionist, reopen it later
   to check what was said: times, instructions, a reference number.
2. **Carry on.** A side by side chat with a friend continues across sessions,
   like a thread, instead of starting blank every time.

## Goals

- When a side by side chat ends, the user can choose to keep it.
- Kept chats are listed, can be reopened, and carry on exactly where they
  left off: messages, both people's names and colours, and whose turn it is.
- Kept chats survive app restarts and phone changes through the user's own
  Android backup.
- A kept chat can be deleted, and then it is really gone from the device.

## Non-goals

- Face to face. It has its own message list and fits neither reason.
- Export, share, or sending a chat to another person (#183, #184, #188).
- Search across chats.
- A separate app lock (PIN or fingerprint) for kept chats. The phone's own
  lock is the guard. This could be a later feature.
- Syncing between devices beyond Android backup.

## Anti-goals

- Keeping a chat the user didn't choose to keep. A chat with a stranger at a
  pharmacy must not be stored unless the user says so.
- Data loss in a kept chat: once a chat is kept, a crash or flat battery
  must not lose messages sent after it was reopened.
- Anything leaving the phone other than the user's own Android backup.

## Decisions (made with the owner)

- **Which chats are saved (D3):** only those the user chooses to keep.
- **Backup (D4):** kept chats are included in Android backup and
  device-to-device transfer, like templates and settings.
- **Storage (D5):** one file per kept chat in app-private storage.

## Approaches considered (storage)

1. **One file per kept chat (chosen).** `filesDir/chats/<id>.json`, written
   atomically (`AtomicFile`) in the same format the chat already uses for
   saved state (`ChatCodec`). Deleting a chat deletes its file. A long friend
   thread rewrites only its own file. The backup rules add one folder. No new
   dependency. Cost: a little code to list chats by reading each file's
   summary.
2. **One DataStore document holding every kept chat**, beside templates and
   settings (ADR-0005). Consistent with the app, but every save rewrites
   every chat, which gets slow once a thread is long.
3. **Room database.** Scales best and allows search later, but is a new
   dependency to screen, plus a schema and more code than listing, opening
   and deleting needs.

## Behaviour

### Ending a chat

- Today: "End this chat?" / "The messages will be deleted." / End chat /
  Keep chatting. Ending with no messages skips the dialog.
- New, for a chat that isn't kept and has messages: the dialog offers
  **Keep** (saves, then ends), **Delete** (discards, as today) and
  **Keep chatting** (cancel). *(Builder settles exact copy; no en or em
  dashes in copy.)*
- A chat with no messages ends with no dialog, as today, and isn't kept.
- For a chat that is already kept, ending saves and leaves with no dialog,
  even if every message has been deleted. A kept chat only goes away when
  the user deletes it from the list. *(Claude's call: the user chose to keep
  it, so it shouldn't vanish without asking.)*
  *(Claude's call: asking every time would nag in a friend thread.)*

### Saving while a kept chat is open

- A kept chat is saved after every change to the conversation (each sent
  message, deleted message, participant change, turn change), not only on
  end. *(Claude's call, to meet the no-data-loss anti-goal.)*
- Drafts are not saved to the file; they stay in saved state as today.
  *(Claude's call.)*

### Saved chats list

- Home's Chat group gains a **Saved chats** card when at least one chat is
  kept. *(Claude's call.)*
- The list shows kept chats newest first (by last change). Each row shows
  the other person's name (or "Them"), the date of the last change, and the
  last message's text, cut to one line. A kept chat with no messages shows
  a short "No messages" line in place of the last message.
- Tapping a row opens it in side by side with everything restored.
- Each row can be deleted, after a confirmation. *(Builder picks the
  gesture or menu, matching the app's existing delete patterns, such as
  template delete in #162 and message delete in #161.)*
- Empty state: the card isn't shown, so the list is only reachable with at
  least one chat. If the last chat is deleted while on the list, show a
  short empty message.

### Identity

- A kept chat has a stable random ID. It's stored in the chat's saved
  state, so a reopened chat writes back to the same file, including after
  process death.

## Storage

- `filesDir/chats/<id>.json`, one per kept chat, written with `AtomicFile`.
- Content: the conversation in the existing `ChatCodec` format, plus a
  version number and the last-changed time. *(Builder decides whether to
  wrap the existing DTO or extend it; no migrations are needed before
  launch.)*
- The list reads only what it needs for each row. With tens of chats,
  reading each file is fine; no index file. *(Claude's call; revisit if
  it's slow.)*
- File work is on a background dispatcher, never on the main thread.

## Privacy and documents

This turns the most sensitive content in the app into stored personal data.
All of these change in the same piece of work:

- **Requirement H7** (`docs/requirements.md`): chats are discarded when they
  end unless the user keeps them; kept chats are stored on the device and
  in the user's Android backup.
- **New ADR** superseding ADR-0008: Android backup covers templates,
  settings and kept chats. `backup_rules.xml` and
  `data_extraction_rules.xml` include the `chats/` folder for cloud backup
  and device transfer.
- **Record of processing** (`docs/data-protection.md`): the chat messages
  row changes from "in memory only" to kept chats on the device and in the
  user's Android backup, kept until the user deletes them or the app.
- **Threat model** (`docs/threat-model.md`): kept chats are an asset at
  rest; anyone with the unlocked phone can read them; the user's Android
  backup holds them, encrypted by Android.
- **Help:** side by side help explains keeping, reopening and deleting.
- **Website privacy page** (`oneloneraven/deaftly-website`,
  `views/privacy/index.md`): kept chats, and that they go into Android
  backup if it's on. That's a separate PR in the website repo, released
  alongside.

## Testing

- Store: round trip, atomic write, delete removes the file, listing order.
- View model: keeping, ending a kept chat without a dialog, saving after
  each change, reopening restores the conversation exactly.
- UI: end dialog choices, list rows and empty state, delete confirmation.
- Screenshots: end dialog and saved chats list in light, dark and 2x font
  scale.
- Backup rules: the `chats/` folder is included (test or lint the XML).

## Left to the builder

- Exact copy for the dialog, card, list and empty state, within the
  project's copy rules.
- Delete gesture or menu, matching existing patterns.
- How the file's DTO is wrapped or versioned.
- Splitting into issues and PRs (suggested: store and backup; keep on end
  and save while open; list and reopen; docs and help; website).
