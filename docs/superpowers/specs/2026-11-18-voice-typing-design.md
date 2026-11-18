# Voice typing in side-by-side chat: design

Issue: #59. Engine decision: [ADR-0013](../../adr/0013-transcribe-speech-with-sherpa-ncnn-bundled-in-the-app.md). Evidence: [#285](../../spikes/285-speech-to-text/results.md), [#287](../../spikes/287-speech-to-text-models/results.md).

Status: agreed with the owner in conversation on 18 November 2026. The builder check (a fresh reviewer reading this doc for gaps) ran the same day; its outcomes are under [Builder check outcomes](#builder-check-outcomes). Plan: [2026-11-18-voice-typing.md](../plans/2026-11-18-voice-typing.md).

## Why

G finds that people they know talk to them much more naturally and expansively when they can use their voice instead of typing. Voice typing lets the hearing person in a side-by-side chat speak their replies, transcribed live into the chat.

It isn't needed for basic communication; typing already covers that. It's there to make conversation flow.

## Who it's for

People the deaf user knows, in longer conversations. It's not aimed at strangers: explaining voice typing to a stranger adds friction. So nothing prompts or advertises it, and there's no "too many choices" moment.

## Goals

- The hearing person can switch on voice typing and talk. Their words appear live, and each pause sends a message.
- Mistakes can be fixed by tapping a message to edit it.
- The deaf user's turn is never interrupted by the mic.

## Non-goals

- Noisy places. Voice typing usually isn't practical there, and typing still works.
- Face-to-face chat, languages other than English, the deaf user's own speech, and text-to-speech (#60).
- Getting strangers to use it.

## Anti-goals

These would make it a failure even if it technically works:

- The mic picking up while the deaf user is typing.
- Audio being stored, or leaving the phone.
- The feature getting in the way of typing.

## Behaviour (owner's decisions)

1. **The mic button** sits next to Send in the composer, in the same accented style (the participant's strong colour, as `SendButton` uses). It shows **only on the hearing person's turn** (participant B). The deaf user never sees it.
2. **Tapping the mic turns voice typing on:**
   - the other composer buttons are disabled
   - the text input is hidden
   - the turn prompt changes to something like "Voice typing is on. Speak clearly towards the phone." (final copy to agree; professional register, no en dashes)
3. **Words appear live** in the message pane as they're spoken, in the hearing person's bubble style.
4. **Voice typing stays on** until it's turned off or the chat ends.
   - It only listens on the hearing person's turn.
   - When the turn switches to the deaf user, listening stops and they get the normal keyboard.
5. **A longer pause sends the message,** so messages don't grow too long. The pause length may need tuning after real use.
6. **No minimum length.** One-word and two-word messages ("Yeah", "Tuesday") are valid turns and must not be dropped.
7. **Tapping a message to edit it:**
   - only the hearing person's own voice-typed messages can be tapped
   - tapping one turns voice typing off and takes the message out of the chat
   - the message goes into the normal text field to edit, and sends again like any other reply
   - in-place editing was rejected, because the chat would then show something other than what was first said

## Builder's calls (made in conversation, open to change)

- **Turning it off:** tap the mic button again.
- **Returning to the hearing person's turn:** if voice typing was on when the turn moved away, it comes back on.
- **Pause length:** start at 2 seconds of trailing silence, using sherpa-ncnn's own endpoint rule (`rule2MinTrailingSilence`).
- **Microphone permission:** Android asks the first time the mic is tapped. If they refuse, typing still works and the mic offers a way to grant it later.

## Builder check outcomes

The owner decided these on 18 November 2026:

- **Words still being spoken are sent, not dropped.** When voice typing stops for any reason (mic off, turn moves to the deaf user, chat ends, app goes to the background), the words recognised so far are sent as the hearing person's message. The message is always stamped with the hearing person as its author, never taken from whoever's turn it is when it lands.
- **Editing:** tapping a voice-typed message edits it. "Edit" is also in the long-press menu, and is a TalkBack action.
- **Editing with a draft already typed:** the message text is added to the end of the draft.
- **Where the model lives:** a GitHub release asset on this repo, separate from app releases, fetched in the build and checked against its SHA-256. It isn't committed to git.
- **Showing that voice typing is on:** a status line that stays up while it's on, separate from the turn prompt. The turn prompt only shows until a participant has sent something, so it can't carry this.

The builder decided these; they're open to change:

- **Saving the flag:** "voice-typed" is a field on the message, saved with the chat. It defaults to false, so existing chats read as before.
- **What counts as a message:** only empty or whitespace results are dropped. Anything else is a message, however short.
- **Coming back on:** when the turn returns to the hearing person, voice typing listens again without a tap, but only if the microphone permission is still granted. The status line and a TalkBack announcement show it's listening.
- **Editing is for the hearing person's turn:** tap to edit and the Edit command work only on their turn. On the deaf user's turn, a tap does nothing, so the deaf user can't start an edit by accident.
- **When the model fails to load:** voice typing turns off, the mic shows that it's unavailable, and typing works as normal. Nothing derived from audio is logged.
- **Denied for good:** if the microphone permission is refused for good, the mic offers to open the app's settings, as the camera does.
- **The sherpa-ncnn source:** fetched as a pinned release tarball with a checksum, then compiled in the app's CMake against the app's ncnn. The model files are converted once by hand; the checkpoint and the SHA-256 of each output are recorded in `third_party/`.
- **Last words when stopping:** stopping sends the text already shown on screen, at once, rather than waiting for the engine to finish decoding. A word cut off at the moment of stopping may be lost; the hearing person can correct it by editing.

## Approaches considered

- **Chosen:** sherpa-ncnn in-process, fed by the app's own `AudioRecord`, with sherpa's endpoint rule for pauses and the model loaded the first time the mic is tapped.
  - The audio stays in memory and is never written anywhere.
  - Loading took about 0.2 s on a OnePlus 13.
- **Rejected:** Android's `SpeechRecognizer` (ADR-0013); a separate voice-activity detector (another model and dependency for no gain); keeping the engine loaded across chats (about 230 MB of memory held for a mid-conversation feature).

## Technical constraints

- **Engine:** `SpeechRecogniser` interface in `:core`, with the sherpa-ncnn implementation in `:app`, as `TextRecogniser` / PaddleOCR are set up (ADR-0010).
- **Native build:** sherpa-ncnn v2.1.15 builds and runs against the app's ncnn 20261020 (verified in #287). Build it from source in the app's CMake alongside the existing OCR code, so there's one shared `libncnn`. Don't use sherpa's prebuilt Android libraries: they bundle a modified ncnn.
- **Model:** `icefall-libri-giga-pruned-transducer-stateless7-streaming-2023-04-04` (k2-fsa, Apache-2.0).
  - Generate the ncnn files from k2-fsa's checkpoint with icefall's published ncnn export script.
  - Record their checksums.
  - Fetch them in the build, as `FetchNcnnTask` does for ncnn.
  - The spike used IconicAI's third-party export from Hugging Face; replace it.
  - Don't attempt int8: it corrupts the encoder (#287).
- **Decoding and threads:** greedy search, 2 threads. Feed 100 ms chunks.
- **Permissions and privacy (ADR-0011):**
  - add `RECORD_AUDIO` to `config/permissions-allowlist.txt`
  - update `docs/threat-model.md`, `docs/data-protection.md`, `docs/privacy.md` and the website privacy page, in the same PR
  - add a `SpeechSendsNothingTest` alongside `OcrSendsNothingTest`
  - no `INTERNET` permission
- **Storage:** a sent voice-typed message is stored like a typed message, so kept chats need no format change. "Voice-typed", which decides whether a message can be tapped to edit, needs a flag on the message. Keep it in memory or in the chat file, as the builder sees fit.
- **Measurements still owed before release** (from #59):
  - a mid-range phone: a Pixel 6a counts as an optimistic proxy; a Galaxy A-series is preferred
  - real recordings of hearing speakers in real places, with their consent. The recordings stay out of git.

## Left to the builder

- **Layout:** the exact layout and states of the mic button (on, off, listening, permission refused) and the live-text styling. Run them through the accessibility checks, including a TalkBack announcement when voice typing turns on.
- **Lifecycle:** how listening pauses when the app goes to the background, and resumes when it comes back.
- **Copy:** for the prompt and the permission explanation. Show it to the owner.
- **Help:** update `help/` (the chat page) to cover voice typing.

## Where the work is

- The spike harness is on branch `spike/285-speech-to-text` (not for merge). It holds a working Kotlin wrapper (`spike-stt/`).
- The tools and scripts are in `tools/stt-spike/`.
