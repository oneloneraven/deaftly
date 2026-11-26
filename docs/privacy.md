# Privacy

Rules for handling personal data in this project. The record of what is
processed, and by whom, is in [data protection](data-protection.md).

- **Collect the minimum.** Every piece of personal data has a stated
  purpose in the record of processing before it's collected.
- **No personal data in logs**, URLs or query strings, analytics events,
  error reports, test fixtures or the repository.
- **No email addresses published** in the repository, the site or DNS
  records. Scrapers harvest them.
- **No provider or supplier names, and no detailed architecture, in
  user-facing copy.** Name what a feature does, not what it runs on.
- **One person's raw data is never exposed to another.** Features that
  combine data across people use anonymised aggregates only.
- **Test setup never reaches real external services.** Local secrets files
  can hold real keys, so bulk test setup uses fakes or test modes.
- **Telemetry is aggregate and minimal.** Prefer approaches that need no
  consent banner because they collect nothing personal.
- **A new processor** (any third party that receives personal data) is
  added to the record of processing and the threat model in the same PR.

## Microphone

- Microphone audio is processed in memory on the phone to turn speech into
  text. It is never written to a file, a log, saved state or a chat, and
  never sent anywhere.
- The microphone is used only while voice typing is on and the app is in the
  foreground: in Side by side, on the hearing person's turn; in Listen for
  me, until the user pauses it or leaves the screen. The screen shows when it
  is listening.
- Only the recognised text is kept. In Side by side it is a chat message the
  user can edit or delete. It is stored only if the user keeps the chat, and a
  kept chat can reach the user's own Android backup (ADR-0012 (in the vault)).
  In Listen for me it stays on that screen until the user ends it, and is not
  stored; continuing to Side by side carries it into that chat.
- The app works without the permission: the user can refuse it and type.

## Feedback from the app

- The app uses the network only to send a feedback or issue report the user
  writes, when they tap Send. Nothing is sent in the background, and no other
  feature uses the network.
- It sends the message, the app version, and only if the user gives them, an
  email address, their quote consent and credit choice, and a name. It goes to
  the system One Lone Raven uses to receive feedback for all its products,
  under the same retention as the website's form (see the website's privacy
  page).
- The screen asks people not to include health or other personal details, and
  says plainly that without an email we can't reply.

