# Data protection: record of processing

What personal data Deaftly processes, why, where it goes and how long it's
kept. Keep this current: a change that collects new data or adds a processor
updates it in the same PR.

## Processors

One Lone Raven's feedback system receives feedback and issue reports that the user chooses to send from the app (see Processing activities). It's the same system the website's feedback form uses, run by One Lone Raven itself. No other part of the app uses the network. The `INTERNET` permission is on the allowlist for this alone (ADR-0011 (in the vault)).

The user's own Android backup can copy the template and settings store and kept chats off the device (ADR-0012 (in the vault)). That is a service the user chooses and controls in system settings; the app neither configures nor reads it.

## Distribution through Google Play

Google Play is an independent controller for the accounts, installs and payments it handles. The app shares nothing with it (ADR-0014 (in the vault)). Play Console shows the project aggregate install and rating statistics. It also shows Android vitals: crash and freeze reports, with the device model and Android version, from users who chose in their device settings to share usage and diagnostics with app developers. The project uses these only to find and fix faults, and doesn't export them or combine them with anything else. They contain none of the data in the table below.

The APK on GitHub Releases involves no third party beyond GitHub's own download logs.

## Processing activities

All processing happens on the user's device, except a feedback or issue report the user chooses to send. The project receives nothing else.

| Data | Purpose | Lawful basis | Retention | Who can see it |
|---|---|---|---|---|
| Templates and the values typed into them (may include names, health details or routines) | Composing messages to show | Not applicable: processed only on the user's device for their own use | Until the user deletes them or the app | The user, and anyone they show the phone to |
| Participant names and colours, and other settings | Personalising chat and show mode | As above | Until the user changes them or deletes the app | The user |
| The user's first name, last name and full legal name (all optional) | Pre-filling the name gaps in templates, and optionally naming the user in chats | As above: kept on the device only, in the settings store, and never sent anywhere. The Android backup includes the settings with the template store, at the user's choice (ADR-0012 (in the vault)) | Until the user changes them or deletes the app | The user, and anyone they show the phone to once a name is filled into a message |
| Chat messages | Holding a conversation | As above | In memory only; discarded when the chat ends, unless the user keeps it (see Kept chats) | The two people in the conversation |
| Kept chats: messages, both people's names and colours, and when the chat last changed | Reading a chat again later, or carrying it on | As above: kept on the device only when the user chooses Keep, and included in the user's Android backup at their choice (ADR-0012 (in the vault)) | Until the user deletes the chat or the app | The user, and anyone they show the phone to |
| Camera frames and recognised text | Lifting text into a template | As above | Frames: never stored. Text: only if the user inserts it into a template | The user |
| Microphone audio and recognised speech | Letting the hearing person speak their replies in a side by side chat, or speak to the user in Listen for me | As above | Audio: processed in memory only and never stored or sent. Recognised text: in Listen for me, held only on that screen (in memory and its saved state) until the user ends it or continues to side by side; in side by side, stored as a chat message, so it is discarded when the chat ends unless the user keeps it, and a kept chat can reach the user's Android backup at their choice (ADR-0012 (in the vault)) | The two people in the conversation, and anyone the user shows a kept chat to |
| Feedback and issue reports sent from the app: the message and app version, plus an email address, quote consent, credit choice and name if the user gives them. The request's IP address is used for rate limiting | Understanding and fixing problems, improving Deaftly, replying when asked, and quoting people who agreed | Consent: sent only when the user writes a report and taps Send | 12 months after it's dealt with, or after its last update if not. Spam is deleted after 30 days. A quote lasts until the person asks us to stop or the report is deleted | One Lone Raven |

## Gaps to settle

None. Stored conversations are covered by the Kept chats row and ADR-0012 (in the vault). Revisit if any other feature adds network access or a processor.

## Security measures

See the threat model (in the vault) and [privacy rules](privacy.md).
