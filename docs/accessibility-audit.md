# Accessibility audit

Covers requirement N3 (WCAG 2.2 AA for contrast and touch targets, and support for the system font scale). See [requirements](requirements.md).

This is a partial audit. The TalkBack, Switch Access and real-device passes in #53 still need a person with a phone; see [Not covered](#not-covered).

## Method

- Date: 2026-11-09.
- Build: debug APK from `main` at the first-run screen merge (`9399518`).
- Device: Android emulator, Pixel 6 profile, API 36 AOSP image (`Android SDK built for x86_64`), 1080 by 2400 at 420 dpi, camera back set to the emulator's virtual scene.
- TalkBack is not available on this image (no Google accessibility suite), so TalkBack itself was not run. In its place each screen was walked on the emulator and dumped with `uiautomator dump`, which reads the same accessibility node tree TalkBack and Switch Access use. For every clickable, checkable or text-entry node the dump was checked for a label (text or content description), a size of at least 48 dp (126 px at this density), and its state (checked, disabled).
- Reading order is the order of nodes in the dump.
- Font scale: every screen was also driven at the system maximum of 2.0 (200%) and the screenshots were inspected for clipped or truncated essential text.
- Contrast is covered by `ThemeContrastTest` (AA for text, 3:1 for control boundaries, light and dark), which already includes the violet accent and the vivid chat colours.
- Robolectric tests with accessibility checks on (`enableAccessibilityChecks()`) already cover each screen on the JVM.

## Results by screen

Status "pass" means no finding at 1x and at 200%.

| Screen | Labels and actions | 48 dp targets | 200% font | Status |
| --- | --- | --- | --- | --- |
| First run | Name field, six colour swatches (radio, checked state), two layout options and Get started all labelled | pass (swatches 52 dp) | Scrolls; Get started stays docked; no clipping | pass |
| Home | New template, Settings, search, template cards, category chips, Type & show, Start chat all labelled | pass | Pinned cards fill most of the screen; list and categories scroll; card previews truncate to two lines (non-essential) | pass |
| Fill, empty and filled | Back, Edit template, placeholder chips (labelled with the placeholder name), Show (disabled until filled) | pass | Wraps cleanly | pass |
| Fill, choices sheet and "Something else" | Each choice labelled; scrim exposes "Close sheet"; drag handle labelled | pass | Typing sheet fits | pass |
| Fill, camera placeholder sheet | Scan the menu, Type it | pass | Not separately checked | pass |
| Show, all three schemes | Close show mode, Change colours (label states the current and next scheme), Continue as chat | pass | Message text auto-sizes; footer fits | pass |
| Type & show | Back, Clear message, Message to show field, Show | pass | Not separately checked beyond Show | pass |
| Editor | Cancel, Save, Name, Category, category chips, Message, Placeholder, Delete placeholder, placeholder fields; discard dialog buttons | pass | Title wraps to two lines; all fields reachable | pass |
| Chat, side by side | End chat, participants, layout switch, text size, composer, Bold, Italic, Highlight, Quote, Insert template, Send, turn control (label includes who is next) | pass | Header participants wrap; toolbar and turn control fit | pass |
| Chat, split screen (removed in #125, replaced by Face to face) | Composer for each half, Send, My turn, End chat, layout switch | pass | Both halves fit | pass, one note (below) |
| Chat, participants sheet | Names, swatches (radio, checked), taken colours disabled, Done | pass | Not separately checked | pass |
| Chat, end dialog | Keep chatting, End chat | pass | Not separately checked | pass |
| Settings | Back, both layout options, name, swatches, three show-mode options | pass | Scrolls; no clipping | pass |
| OCR, rationale, denied and blocked | Back, Allow camera, Type it instead, Open settings | pass | Rationale fits | pass |
| OCR, live view with the virtual scene | Close camera, Freeze camera, Menu item field, Add to message (disabled until text is chosen) | pass | Not separately checked | pass, see note |

### Findings

No defect was found that needs a code change in this branch. Notes:

1. Split chat reading order. The upside-down half comes first in the node order, so a TalkBack user would reach the other person's field and the My turn control before their own. This may be right (it is the other person's half) but it has not been tried with a screen reader. Suggested issue: "Check the TalkBack and Switch Access reading order in split chat on a real device, and decide whether the player's own half should come first."
2. Home category chips. The last chip in the horizontally scrolling row is partly off screen and appears in the dump as a 67 by 126 px node with no label. This is the scroll edge, not a missing label, but it was not confirmed with TalkBack scrolling the row.
3. OCR phrases. The live view's phrase overlay is coded as buttons with a content description, a selected state and a 48 dp minimum target (`PhraseOverlay.kt`), and is covered by `OcrScreenTest`. The emulator's virtual scene contains no text, so selecting a phrase was not exercised on the emulator.
4. The system "Viewing full screen" confirmation appears the first time Show opens. It is a platform dialog and not something the app controls.

### Switch Access

Every interactive control seen in the dumps is clickable or checkable with a label, so each is reachable as a Switch Access target in principle. Disabled controls (Show, Send, Add to message, taken colour swatches) are correctly exposed as disabled. A real Switch Access pass, with scanning and item-by-item focus, was not done.

## Known limitations

- The launcher label ("Deaftly", under the app icon and in the app switcher) is read by TalkBack as "deeftly". Android takes the label from a string resource and gives an app no way to attach a spoken form to it, so it can't be corrected. Inside the app, every place the name appears carries a content description that spells it "Deftly" for speech (#121). Braille displays still show the spelling "Deaftly", which is out of scope because the target audience is sighted deaf people.

## Not covered

- TalkBack with a real user, including focus order, announcements and gestures. TalkBack was not installable on the emulator image.
- Switch Access with hardware or the on-device scanner.
- A real device. All checks were on an emulator with software rendering.
- Dark theme on the emulator (contrast is covered by `ThemeContrastTest`, and Roborazzi records dark screenshots).
- Per-screen 200% checks for the participants sheet, end dialog, Fill camera sheet, OCR live view and OCR denied screens (the 1x results and the Roborazzi large-font screenshots stand in for these).
- Display size (as opposed to font size) scaling, bold text, colour inversion and colour correction.
