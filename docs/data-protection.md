# Data protection: record of processing

What personal data Deaftly processes, why, where it goes and how long it's
kept. Keep this current: a change that collects new data or adds a processor
updates it in the same PR.

## Processors

None. The app has no network access ([ADR-0004](adr/0004-the-app-has-no-network-access.md)), so no third party receives data from it.

The user's own Android backup can copy the template and settings store off the device ([ADR-0008](adr/0008-allow-android-backup-of-templates-and-settings.md)). That is a service the user chooses and controls in system settings; the app neither configures nor reads it.

## Processing activities

All processing happens on the user's device. The project receives none of it.

| Data | Purpose | Lawful basis | Retention | Who can see it |
|---|---|---|---|---|
| Templates and the values typed into them (may include names, health details or routines) | Composing messages to show | Not applicable: processed only on the user's device for their own use | Until the user deletes them or the app | The user, and anyone they show the phone to |
| Participant names and colours, and other settings | Personalising chat and show mode | As above | Until the user changes them or deletes the app | The user |
| Chat messages | Holding a conversation | As above | In memory only; discarded when the chat ends | The two people in the conversation |
| Camera frames and recognised text | Lifting text into a template | As above | Frames: never stored. Text: only if the user inserts it into a template | The user |

## Gaps to settle

None. Revisit if any feature adds network access, a processor or stored conversations.

## Security measures

See the [threat model](threat-model.md) and [privacy rules](privacy.md).
