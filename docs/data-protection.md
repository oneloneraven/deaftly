# Data protection: record of processing

What personal data Deaftly processes, why, where it goes and how long it's
kept. Keep this current: a change that collects new data or adds a processor
updates it in the same PR.

## Processors

None. The app requests no network permission today, so no third party receives data from it. Any feature that would change that adds the permission to the allowlist and updates this record in the same PR ([ADR-0011](adr/0011-review-permissions-against-an-allowlist.md)).

The user's own Android backup can copy the template and settings store and kept chats off the device ([ADR-0012](adr/0012-back-up-kept-chats.md)). That is a service the user chooses and controls in system settings; the app neither configures nor reads it.

## Processing activities

All processing happens on the user's device. The project receives none of it.

| Data | Purpose | Lawful basis | Retention | Who can see it |
|---|---|---|---|---|
| Templates and the values typed into them (may include names, health details or routines) | Composing messages to show | Not applicable: processed only on the user's device for their own use | Until the user deletes them or the app | The user, and anyone they show the phone to |
| Participant names and colours, and other settings | Personalising chat and show mode | As above | Until the user changes them or deletes the app | The user |
| The user's first name, last name and full legal name (all optional) | Pre-filling the name gaps in templates, and optionally naming the user in chats | As above: kept on the device only, in the settings store, and never sent anywhere. The Android backup includes the settings with the template store, at the user's choice ([ADR-0012](adr/0012-back-up-kept-chats.md)) | Until the user changes them or deletes the app | The user, and anyone they show the phone to once a name is filled into a message |
| Chat messages | Holding a conversation | As above | In memory only; discarded when the chat ends | The two people in the conversation |
| Kept chats: messages, both people's names and colours, and when the chat last changed | Reading a chat again later, or carrying it on | As above: kept on the device only when the user chooses Keep, and included in the user's Android backup at their choice ([ADR-0012](adr/0012-back-up-kept-chats.md)) | Until the user deletes the chat or the app | The user, and anyone they show the phone to |
| Camera frames and recognised text | Lifting text into a template | As above | Frames: never stored. Text: only if the user inserts it into a template | The user |
| Microphone audio and recognised speech | Letting the hearing person speak their replies in a side by side chat | As above | Audio: processed in memory only and never stored or sent. Recognised text: stored as a chat message, so it is discarded when the chat ends unless the user keeps it, and a kept chat can reach the user's Android backup at their choice ([ADR-0012](adr/0012-back-up-kept-chats.md)) | The two people in the conversation, and anyone the user shows a kept chat to |

## Gaps to settle

None. Revisit if any feature adds network access, a processor or stored conversations.

## Security measures

See the [threat model](threat-model.md) and [privacy rules](privacy.md).
