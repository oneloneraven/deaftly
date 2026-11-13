# Deaftly – High-Level Requirements (MVP)

Status: Approved (2026-11-07)
Tech stack: [tech-stack.md](tech-stack.md).
Source: [PRD.md](PRD.md). Research: [research.md](research.md) (Appendix A). Deferred items: the [Post-MVP epic](https://github.com/oneloneraven/deaftly/issues/13). Delivery: the [project board](https://github.com/users/ravendarque/projects/7).

Requirements added or amended in step 2 are marked with *(A: F*n*)*, where F*n* is the supporting finding in Appendix A.

## 1. Purpose

Deaftly is an Android app that helps deaf people communicate quickly and with little friction with hearing people, using text on the phone screen.

## 2. Guiding principles

- **KISS.** The MVP covers the three PRD features and nothing else.
- **Speed under pressure.** Every core task works one-handed in a few taps, for example at a till with a queue behind you.
- **Readable at a glance.** Text shown to a hearing person must be legible at arm's length.
- **Private by default.** All data stays on the device. There are no accounts, no backend and no network dependency.
- **Minimal, beautiful, with personality.** The design is clean and calm, with room for the user's own expression.

## 3. Scope and constraints

| ID | Constraint |
|----|------------|
| C1 | Android only. |
| C2 | English only, localised for en-GB (the default), en-US, en-AU, en-CA, en-NZ and en-IE: spelling, vocabulary and the name of the local official sign language (BSL, ASL, Auslan, NZSL, ISL). Other English locales use en-GB. OCR reads English text (#217). |
| C3 | No accounts, app-run sync or backend in the MVP. Network access is decided per feature, through a reviewed change ([ADR-0011](adr/0011-review-permissions-against-an-allowlist.md)). The user's own Android backup of templates and settings is allowed (decision D7). |
| C4 | All processing, including OCR, runs on the device. |
| C5 | Free, with no paid third-party services. |
| C6 | Distributed as a signed APK on GitHub Releases; not on Google Play for now (decision D8, see [#69](https://github.com/oneloneraven/deaftly/issues/69)). |
| C7 | Every dependency and vendor is screened against the BDS boycott lists before adoption (project-bootstrap ADR-0002). |

## 4. Functional requirements

### 4.1 Quick-access templates

| ID | Requirement |
|----|-------------|
| T1 | The app ships with a starter set of built-in templates for common situations (check-in, ordering, asking for help, explaining deafness). |
| T2 | Users can create, edit, duplicate and delete their own templates. Built-in templates can be edited and restored to default. |
| T3 | Templates can contain placeholders, each of which has a label and a list of predefined options. |
| T4 | Tapping a placeholder shows its options and lets the user type a custom value. |
| T5 | A placeholder can be marked repeatable, so the user can add further values ("+ add more"). |
| T6 | A placeholder can be of type OCR, which opens the camera capture (see 4.3). |
| T7 | Templates are reachable within one tap of app launch, with a way to pin favourites. |
| T8 | Users can organise templates simply, for example by pinning or with basic categories. |
| T9 | Composing favours choosing over typing. Placeholder options are presented first, and custom typing comes second. *(A: F1, F2)* |
| T10 | Built-in template copy uses plain English: short sentences, common words, one idea per sentence. *(A: F1)* |
| T11 | Built-in templates include a short, polite line telling the hearing person how to respond (e.g. "Please type your reply on my phone"). *(A: F3, F6)* |

### 4.2 Show mode

| ID | Requirement |
|----|-------------|
| S1 | A completed template (or any message) is previewed, then shown full screen in large, high-contrast text for a hearing person to read. *(A: F2)* |
| S2 | The text scales to fill the screen down to a minimum legible size. Beyond that it wraps and scrolls; it never shrinks to unreadable. *(A: F4)* |
| S3 | Show mode keeps the screen awake. |
| S4 | The user can leave show mode and return to editing in one gesture. |
| S5 | From show mode, the user can go straight into a chat with the shown message as the first turn. |
| S6 | Show mode has a small choice of simple, high-contrast colour schemes (default: black on white, or white on black in the dark theme). *(A: F4)* |
| S7 | A free-text "type and show" entry is reachable in one tap from launch, with a one-tap clear for typing the next message. *(A: F4, F7)* |

### 4.3 Live OCR capture

| ID | Requirement |
|----|-------------|
| O1 | The camera preview recognises text live, on the device. |
| O2 | Recognised word groups (lines or blocks, e.g. "Pasta arrabbiata") are highlighted and can be selected with one tap. |
| O3 | The user can drag across recognised text to select a custom range. |
| O4 | Selected text can be edited before it is inserted into the placeholder. |
| O5 | The camera can be frozen on a frame so the user can select text without holding the phone steady. |
| O6 | The torch can be toggled for dim environments such as restaurants. |

### 4.4 Two-person chat

| ID | Requirement |
|----|-------------|
| H1 | A single screen holds a conversation between two people, each with their own side of the screen for message bubbles. |
| H2 | Each person has a colour (with a sensible default) and an optional name and avatar. |
| H3 | Turns are switched with a swipe. The active side is always visually obvious. |
| H9 | **Face to face** replaces the split-screen chat layout: the top half, rotated 180°, shows the deaf user's sent messages and the text being typed, live, in medium text for the person opposite; the bottom half is the deaf user's field and Send button. The stranger can scroll back without disturbing the keyboard. The user can continue side by side, which opens the chat with each sent message from the deaf user and the turn passed on. *(Decisions D38–D41, issue #125; supersedes D5)* |
| H4 | Messages support simple formatting (bold, italic), highlighting, and quoting an earlier message. |
| H5 | Bubbles are easy to read for both people, with an adjustable text size. |
| H6 | Inserting a template into a chat turn is supported. |
| H7 | Chats are ephemeral. Closing a chat discards it, after a confirmation. |
| H8 | When the turn switches to the hearing person, a short plain prompt tells them what to do (e.g. "Your turn – type your reply"). *(A: F3, F5)* |

## 5. Non-functional requirements

| ID | Requirement |
|----|-------------|
| N1 | The app starts in under 1 second on a mid-range device. |
| N2 | OCR recognition updates in under 500 ms on a mid-range device. |
| N3 | The app meets WCAG 2.2 AA for contrast and touch targets and supports the system font scale. |
| N4 | The app works one-handed. Primary actions sit within thumb reach. |
| N5 | The app has light and dark themes, following the system setting by default. |
| N6 | The app requests no permissions beyond the camera, and asks for it only when OCR is first used. |
| N7 | There is no analytics or telemetry in the MVP. |

## 6. Out of scope (MVP)

Everything in the [Post-MVP epic](https://github.com/oneloneraven/deaftly/issues/13), plus iOS.

## 7. Open points

- **Minimum Android version.** Decided: Android 8.0 (`minSdk` 26), see [tech stack](tech-stack.md).
- **Mockups.** Deaftly MVP mockups (a private design canvas).
- **Content of the starter templates.** To be written to T10 and T11, and validated with deaf users.
- **User testing with deaf users.** Decided: moderated sessions with deaf test users (#56). Their feedback is also the MVP's device verification.
