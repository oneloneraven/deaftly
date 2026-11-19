# Voice Typing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the hearing person in a side by side chat switch on voice typing and speak their replies, transcribed on the phone, with each pause sending a message.

**Architecture:** `:core` gains a synchronous, streaming `SpeechRecogniser` interface, an `AudioSource` seam and fakes for both. `:app` implements the recogniser with sherpa-ncnn compiled from source against the app's own ncnn, and the audio source with `AudioRecord`. A `VoiceTyping` helper owned by `ChatViewModel` (like `ChatDrafts` and `ChatKeeper`) runs listening on a background dispatcher and hands each utterance to the view model, which appends it as participant B with a `voiceTyped` flag. The composer gains a mic button and a status line; tapping a voice-typed message moves it back into the draft.

**Tech Stack:** Kotlin, Jetpack Compose, coroutines, kotlinx.serialization, CMake and the NDK, sherpa-ncnn v2.1.15, ncnn 20261020, Gradle tasks in `buildSrc`, Robolectric, Roborazzi, instrumented tests.

**Spec:** `docs/superpowers/specs/2026-11-18-voice-typing-design.md` (read its "Builder check outcomes" section; it settles every behaviour question below).

## Global Constraints

- Only participant B (the hearing person) ever sees the mic. Participant A's composer is unchanged.
- Listening happens only on B's turn, with the app in the foreground and voice typing on.
- Audio stays in memory: never written to a file, a log, saved state or the chat. No `INTERNET` permission.
- Pause rule: sherpa-ncnn endpointing with `rule2MinTrailingSilence = 2.0f`, in one named constant.
- Engine: greedy search, 2 threads, 16 kHz mono float samples fed in 100 ms chunks (1,600 samples).
- No minimum length: drop only empty or whitespace results.
- A voice-typed message is always authored by `ParticipantId.B`, never by `conversation.turn`.
- Stopping for any reason sends the live text already shown, if it isn't blank.
- Model: `icefall-libri-giga-pruned-transducer-stateless7-streaming-2023-04-04`, exported from k2-fsa's checkpoint with icefall's ncnn export script, fp16 as exported, no int8. Never IconicAI's upload.
- Don't use sherpa-ncnn's prebuilt Android libraries. One shared `libncnn`.
- The model is a GitHub release asset on this repo with a pinned SHA-256, fetched into a generated assets directory. Never committed to git.
- Load the engine the first time voice typing turns on; close it when voice typing turns off or the chat ends.
- Copy: British English, professional register, no en or em dashes, no supplier names in user-facing text. Show the owner all new copy before merging.
- No comments by default; one line for a "why" the code can't express.
- No migrations or backwards compatibility before launch; additive defaulted fields only.
- Every change goes through an issue (a sub-issue of #59), a branch and a PR (`docs/workflow.md`). Suggested PRs: Tasks 1 and 2; Task 3; Task 4; Tasks 5 and 6; Task 7 (website repo).
- `./gradlew check` passes before every push. Heavy work (model export, native builds) stays on disk, not `/tmp` (tmpfs, 14 GiB RAM).
- Never ask G to record speech. Test audio comes from a public, licensed corpus clip; real recordings of hearing speakers are made with consent and kept out of git.

## Review Focus

1. **The turn moves to the deaf user mid-sentence:** the words so far land as B's message, and nothing after the switch is heard. Test in Task 5 (`turnAwayMidUtteranceCommitsAsB`).
2. **Ending the chat while words are live:** the live text is committed before the keep dialog reads the messages, so a chat whose only content is live speech still asks to keep. Test in Task 5 (`stopBeforeEndCommitsLiveText`) and Task 6 (`endingWhileListeningShowsKeepDialog`).
3. **Microphone unavailable** (`AudioRecord` fails to start, another app holds it, model fails to load): voice typing turns off, the mic shows it's unavailable, typing works and the app doesn't crash. Test in Task 5 (`failedStartTurnsOffAndFlagsFailure`).
4. **Process death while voice typing is on:** the chat comes back with voice typing on but nothing listening until the screen is resumed and permission is checked. Test in Task 5 (`restoredOnStateWaitsForResume`).
5. **Editing a voice message that another message quotes, or with a draft already typed:** the quote loses its link but keeps its text, and the message text is added to the end of the draft. Test in Task 5 (`editAppendsToDraftAndUnlinksQuotes`).

---

### Task 1: Voice-typed messages in the chat model

**Files:**
- Modify: `core/src/main/kotlin/com/ravendarque/deaftly/core/chat/Message.kt`, `core/src/main/kotlin/com/ravendarque/deaftly/core/chat/Conversation.kt`
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/ChatCodec.kt` (`MessageDto`)
- Test: `core/src/test/kotlin/com/ravendarque/deaftly/core/chat/ConversationTest.kt`, `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatCodecTest.kt`

**Interfaces:**
- Produces:
  - `Message(..., val voiceTyped: Boolean = false)`
  - `Conversation.appendVoiceTyped(text: String): Conversation`: appends a message authored by `ParticipantId.B` with `RichText.spans(text.trim())`, `voiceTyped = true`, no quote. Requires non-blank text.
  - `deleteMessage` keeps `voiceTyped` on the messages it re-indexes.
  - `MessageDto.voiceTyped: Boolean = false`.

- [ ] **Step 1: Write the failing tests.**
  - `ConversationTest.appendVoiceTypedIsAuthoredByBWhateverTheTurn`: on a conversation with `turn = A`, `appendVoiceTyped("  Tuesday ")` gives one message with `author == B`, `plainText == "Tuesday"`, `voiceTyped == true`, and the turn is still A.
  - `ConversationTest.appendVoiceTypedRefusesBlankText`: `assertFailsWith<IllegalArgumentException> { appendVoiceTyped("  ") }`.
  - `ConversationTest.deletingKeepsVoiceTypedFlags`: append typed, voice, voice; delete id 0; the remaining two have `voiceTyped == true` and ids 0 and 1.
  - `ChatCodecTest.voiceTypedSurvivesARoundTrip`: `decode(encode(c)) == c` for a conversation with a voice-typed message.
  - `ChatCodecTest.messagesWithoutTheFieldReadAsTyped`: decoding a JSON message with no `voiceTyped` key gives `voiceTyped == false`.
- [ ] **Step 2: Run** `./gradlew :core:test :app:testDebugUnitTest --tests '*ConversationTest' --tests '*ChatCodecTest'`. Expected: FAIL (unresolved `voiceTyped`, `appendVoiceTyped`).
- [ ] **Step 3: Implement** the field, `appendVoiceTyped`, and the DTO mapping in both directions.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit.**

### Task 2: Speech interfaces and fakes in `:core`

**Files:**
- Create: `core/src/main/kotlin/com/ravendarque/deaftly/core/speech/SpeechRecogniser.kt`, `core/src/main/kotlin/com/ravendarque/deaftly/core/speech/AudioSource.kt`, `core/src/main/kotlin/com/ravendarque/deaftly/core/speech/FakeSpeechRecogniser.kt`, `core/src/main/kotlin/com/ravendarque/deaftly/core/speech/FakeAudioSource.kt`
- Test: `core/src/test/kotlin/com/ravendarque/deaftly/core/speech/FakeSpeechRecogniserTest.kt`

**Interfaces:**
- Produces:
  - `public const val SPEECH_SAMPLE_RATE: Int = 16_000` and `public const val SPEECH_CHUNK_SAMPLES: Int = 1_600`
  - `public sealed interface SpeechEvent { data class Partial(val text: String); data class Utterance(val text: String) }`. `Utterance` means an endpoint was reached; the recogniser has reset itself for the next one.
  - `public interface SpeechRecogniser : AutoCloseable { fun accept(samples: FloatArray): SpeechEvent?; override fun close() }`. Blocking; callers run it off the main thread. Returns `null` when the text hasn't changed. Text is trimmed.
  - `public fun interface AudioSource { fun chunks(): Flow<FloatArray> }`. Cold: collecting opens the microphone, cancelling releases it. Each chunk is `SPEECH_CHUNK_SAMPLES` long. Failure to open throws `AudioUnavailableException : Exception`.
  - `public class FakeSpeechRecogniser(script: List<SpeechEvent?>) : SpeechRecogniser` returns the scripted event for each `accept` call in order, then `null`; exposes `accepted: Int` and `isClosed: Boolean`; `accept` after `close` throws `IllegalStateException`.
  - `public class FakeAudioSource(chunks: Int = Int.MAX_VALUE, failWith: Exception? = null) : AudioSource` emits zeroed chunks (yielding between them) and exposes `opened: Int` and `isOpen: Boolean`.

- [ ] **Step 1: Write the failing tests** in `FakeSpeechRecogniserTest`: `returnsScriptedEventsInOrderThenNull`, `refusesSamplesAfterClose`, and `FakeAudioSourceTest`-style cases in the same file: `cancellingCollectionClosesTheSource` (`isOpen` true while collecting, false after `cancelAndJoin`), `failingSourceThrowsAudioUnavailable`.
- [ ] **Step 2: Run** `./gradlew :core:test --tests '*speech*'`. Expected: FAIL.
- [ ] **Step 3: Implement** the four files.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit**, then open the PR for Tasks 1 and 2.

### Task 3: sherpa-ncnn engine and bundled model

This task has the build's biggest unknown. Step 1 proves it; if sherpa-ncnn won't compile against the app's ncnn within the app's CMake, stop and report rather than reach for the prebuilt libraries.

**Files:**
- Create: `third_party/sherpa-ncnn/README.md` (source tarballs and checksums, model checkpoint, export commands, per-file SHA-256, licences, ADR-0002 screening)
- Create: `buildSrc/src/main/kotlin/com/ravendarque/deaftly/nativelibs/FetchArchiveTask.kt`, replacing `FetchNcnnTask.kt` (same checksum behaviour; adds `includes: ListProperty<String>` and `stripFirstDir: Property<Boolean>`; handles `.zip` and `.tar.gz`)
- Modify: `app/build.gradle.kts` (ncnn uses `FetchArchiveTask`; new `fetchSherpaSources` and `fetchSpeechModel` tasks; CMake arguments; generated assets directory via `androidComponents.onVariants { it.sources.assets?.addGeneratedSourceDirectory(...) }`)
- Modify: `app/src/main/cpp/CMakeLists.txt` (OCR and speech built into one `deaftly_native` library, so ncnn is linked once), `app/src/main/kotlin/com/ravendarque/deaftly/ocr/paddle/NativePaddleOcr.kt` (loads `deaftly_native`)
- Create: `app/src/main/cpp/speech_jni.cpp`
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/speech/NativeSherpa.kt`, `app/src/main/kotlin/com/ravendarque/deaftly/speech/SherpaSpeechRecogniser.kt`
- Modify: `.github/workflows/release-artefacts.yml` (skip releases whose tag doesn't start with `v`)
- Modify: `docs/threat-model.md` (model provenance row and the "never downloaded" wording)
- Create: `app/src/androidTest/assets/speech-fixtures/` (one LibriSpeech test-clean utterance as 16 kHz mono WAV with its transcript; CC-BY 4.0, credited in `third_party/sherpa-ncnn/README.md`)
- Test: `app/src/androidTest/kotlin/com/ravendarque/deaftly/speech/SherpaSpeechRecogniserTest.kt`, `app/src/androidTest/kotlin/com/ravendarque/deaftly/speech/SpeechSendsNothingTest.kt`, `app/src/androidTest/kotlin/com/ravendarque/deaftly/speech/SpeechNativeInputValidationTest.kt`, `buildSrc` unit test for `FetchArchiveTask` if `buildSrc` has tests, otherwise the existing `fetchNcnn` run proves it

**Interfaces:**
- Consumes: `SpeechRecogniser`, `SpeechEvent`, `SPEECH_SAMPLE_RATE` (Task 2).
- Produces:
  - `class SherpaSpeechRecogniser(context: Context, config: SherpaConfig = SherpaConfig()) : SpeechRecogniser`
  - `data class SherpaConfig(val threads: Int = 2, val trailingSilenceSeconds: Float = PAUSE_SECONDS)` with `const val PAUSE_SECONDS = 2.0f` in the same file
  - Model assets under `speech/` in the APK: `encoder.ncnn.param`, `encoder.ncnn.bin`, `decoder.ncnn.param`, `decoder.ncnn.bin`, `joiner.ncnn.param`, `joiner.ncnn.bin`, `tokens.txt`
  - JNI (`NativeSherpa`): `create(assets: AssetManager, dir: String, threads: Int, trailingSilence: Float): Long` (0 on failure), `accept(handle: Long, samples: FloatArray): Int` (0 no change, 1 partial, 2 endpoint), `text(handle: Long): String`, `release(handle: Long)`

- [ ] **Step 1: Prove the native build.** Fetch sherpa-ncnn v2.1.15, and the kaldi-native-fbank and kissfft versions its CMake pins, as source tarballs. In `CMakeLists.txt`, compile the recogniser sources sherpa needs (an explicit list from `sherpa-ncnn/csrc`, plus kaldi-native-fbank's `csrc` and kissfft's `kiss_fft.c` and `kiss_fftr.c`) with `speech_jni.cpp` into the same library as the OCR code, renamed `deaftly_native`. ncnn is linked statically, so two libraries would each carry a copy. Don't `add_subdirectory` sherpa's top level: it fetches its own ncnn. ncnn is built without exceptions or RTTI, and so is everything linked against it. Run `./gradlew :app:assembleDebug`. Expected: builds for all three ABIs, the APK holds one `libdeaftly_native.so` per ABI, and it's under 1 MB bigger than the OCR-only library.
- [ ] **Step 2: Export the model by hand**, on disk under `~/` not `/tmp`: k2-fsa's `icefall-libri-giga-pruned-transducer-stateless7-streaming-2023-04-04` checkpoint, icefall's `export-for-ncnn.py` then pnnx, as icefall's sherpa-ncnn docs describe. Record the checkpoint commit, commands, tool versions and each output file's SHA-256 in `third_party/sherpa-ncnn/README.md`. Check the export against the spike: transcribe the fixture WAV with the spike harness and confirm the same text.
- [ ] **Step 3: Publish the model.** Zip the seven files as `speech-model-libri-giga-2023-04-04.zip`. Change `release-artefacts.yml` so `attach` runs only `if: startsWith(github.event.release.tag_name, 'v')`, merge that first, then **ask the owner** before creating the public release `speech-model-libri-giga-2023-04-04` (a prerelease, not marked latest) with the zip attached. Record the zip's SHA-256.
- [ ] **Step 4: Write the failing instrumented tests.**
  - `SherpaSpeechRecogniserTest.transcribesTheFixture`: feed the fixture in `SPEECH_CHUNK_SAMPLES` chunks, then 2.5 s of zeros; exactly one `Utterance` arrives, and its word error rate against the transcript is at most 15%.
  - `SherpaSpeechRecogniserTest.silenceGivesNoUtterance`: 5 s of zeros gives no `Utterance` and no non-blank `Partial`.
  - `SpeechSendsNothingTest.transcribingSendsAndReceivesNothing`: the `OcrSendsNothingTest` TrafficStats pattern around loading the recogniser and transcribing the fixture.
  - `SpeechNativeInputValidationTest`: `accept` with an empty array, an array over 10 s of samples, and a released handle each return without crashing (empty and released return 0; oversized is refused with 0).
- [ ] **Step 5: Implement** `fetchSpeechModel` (`FetchArchiveTask` with the release URL and SHA-256, output into the generated assets `speech/` directory, wired before asset merging), the JNI with bounds checks (sample cap, null handle), `NativeSherpa` and `SherpaSpeechRecogniser`. `create` returning 0 throws `IllegalStateException` from the constructor. Native code can't throw (no exceptions), and sherpa may abort the process on a bad model, so before calling `create` the Kotlin side checks that each of the seven assets exists and that its size matches the recorded one, and throws `IllegalStateException` if not. Add `SherpaSpeechRecogniserTest.missingModelThrowsInsteadOfCrashing` using a config that names a model directory that doesn't exist. The model's tokens are upper case: convert the text to sentence case (lower case, first letter and a standalone "i" upper case) and change nothing else.
- [ ] **Step 6: Run** `./gradlew check` and `./gradlew :app:connectedDebugAndroidTest --tests 'com.ravendarque.deaftly.speech.*'` on the emulator. Expected: PASS. Record the debug APK size against `main` in the PR description.
- [ ] **Step 7: Commit** and open the PR.

### Task 4: Microphone access, permission and privacy docs

**Files:**
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/speech/MicrophoneAudioSource.kt`
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/ui/permissions/RuntimePermission.kt` (the state logic from `CameraPermission.kt`, parameterised by permission name)
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/ui/ocr/CameraPermission.kt` (becomes a thin wrapper over `RuntimePermission`; `CameraAccess` and `rememberCameraPermission()` keep their names so OCR callers don't change)
- Modify: `app/src/main/AndroidManifest.xml` (`RECORD_AUDIO`, and `<uses-feature android:name="android.hardware.microphone" android:required="false" />`)
- Modify: `config/permissions-allowlist.txt` (`android.permission.RECORD_AUDIO` with a "Voice typing in side by side chat." comment)
- Modify: `docs/threat-model.md`, `docs/data-protection.md` (a row for microphone audio: processed in memory only, never stored; recognised text is stored as a message and so can reach Android backup, ADR-0012), `docs/privacy.md`
- Test: `app/src/test/kotlin/com/ravendarque/deaftly/ui/ocr/OcrScreenTest.kt` (existing permission cases must still pass), `app/src/androidTest/kotlin/com/ravendarque/deaftly/speech/MicrophoneAudioSourceTest.kt`

**Interfaces:**
- Consumes: `AudioSource`, `AudioUnavailableException`, `SPEECH_SAMPLE_RATE`, `SPEECH_CHUNK_SAMPLES` (Task 2).
- Produces:
  - `class MicrophoneAudioSource(io: CoroutineDispatcher) : AudioSource`: `AudioRecord` with `MediaRecorder.AudioSource.VOICE_RECOGNITION`, mono, `ENCODING_PCM_FLOAT` at 16 kHz, reads `SPEECH_CHUNK_SAMPLES` per emit on `io`, releases in `finally`. Throws `AudioUnavailableException` if `state != STATE_INITIALIZED` or `startRecording` fails.
  - `enum class PermissionAccess { NotAsked, Denied, DeniedForGood, Granted }`, `class RuntimePermission(val access: PermissionAccess, val request: () -> Unit, val openSettings: () -> Unit)`, `@Composable fun rememberRuntimePermission(permission: String): RuntimePermission`
  - `@Composable fun rememberMicrophonePermission(): RuntimePermission = rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)`

- [ ] **Step 1: Write the failing test** `MicrophoneAudioSourceTest.emitsFullChunksAndReleases` (instrumented, with `GrantPermissionRule.grant(RECORD_AUDIO)`): take 3 chunks; each has `SPEECH_CHUNK_SAMPLES` samples; collection completes without throwing.
- [ ] **Step 2: Run** it on the emulator. Expected: FAIL (class missing).
- [ ] **Step 3: Implement** `MicrophoneAudioSource`, `RuntimePermission`, the camera wrapper, manifest and allowlist changes.
- [ ] **Step 4: Run** `./gradlew check` (includes `verifyReleaseManifest` and the OCR permission tests) and the instrumented test. Expected: PASS.
- [ ] **Step 5: Update the privacy docs** listed above. The website privacy page is Task 7.
- [ ] **Step 6: Commit** and open the PR.

### Task 5: Voice typing in the chat view model

**Files:**
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/VoiceTyping.kt`
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/ChatViewModel.kt`, `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/ChatDrafts.kt`, `app/src/main/kotlin/com/ravendarque/deaftly/AppContainer.kt`
- Test: `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/VoiceTypingTest.kt`, `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatViewModelTest.kt` (and every `ChatViewModel` construction site in tests, updated to pass fakes)

**Interfaces:**
- Consumes: `Conversation.appendVoiceTyped` (Task 1); `SpeechRecogniser`, `SpeechEvent`, `AudioSource`, `AudioUnavailableException`, fakes (Task 2).
- Produces:
  - `data class VoiceTypingState(val on: Boolean = false, val listening: Boolean = false, val live: String = "", val unavailable: Boolean = false)`
  - `class VoiceTyping(savedState: SavedStateHandle, scope: CoroutineScope, work: CoroutineDispatcher, createRecogniser: () -> SpeechRecogniser, audio: AudioSource, onUtterance: (String) -> Unit)` with `val state: StateFlow<VoiceTypingState>`, `fun turnOn()`, `fun turnOff()`, `fun update(canListen: Boolean)`, `fun stop()`, `fun close()`. `on` is saved under the key `voice_typing`; `canListen` is never saved.
  - `ChatUiState.Ready.voice: VoiceTypingState`
  - `ChatViewModel`: `val voice: VoiceTyping`; `fun setListeningAllowed(foreground: Boolean, permitted: Boolean)`; `fun editVoiceMessage(messageId: Int)`; `onCleared()` closes `voice`.
  - `ChatDrafts.appendText(owner: ParticipantId, text: String)`: adds `text` to the end of `owner`'s composition, with one space between when the existing text doesn't end in whitespace.
  - `ChatViewModel` constructor gains `createSpeechRecogniser: () -> SpeechRecogniser` and `audio: AudioSource` (AppContainer passes `SherpaSpeechRecogniser(context.applicationContext)` and `MicrophoneAudioSource(Dispatchers.IO)`), and `speechWork: CoroutineDispatcher = Dispatchers.Default`.

Behaviour `VoiceTyping` owns:
- Listening runs only while `on && canListen`. The view model computes `canListen = turn == B && foreground && permitted` and calls `update` whenever any of those changes.
- When listening starts, create the recogniser if there isn't one (on `work`), then collect `audio.chunks()` on `work`, passing each chunk to `accept`. `Partial` sets `live`; `Utterance` calls `onUtterance(text)` on the main dispatcher if it isn't blank and clears `live`.
- When listening stops (`update(false)`, `turnOff`, `stop`), cancel collection, then commit `live` through `onUtterance` if it isn't blank, and clear it. Synchronous on the caller's thread: no waiting on the engine.
- `turnOff` and `close` also close the recogniser. `stop` commits but leaves `on` as it is (used before ending the chat and on background).
- `AudioUnavailableException` or a recogniser that throws on creation: `on = false`, `unavailable = true`, recogniser closed. `turnOn` clears `unavailable`.

`editVoiceMessage(id)` does nothing unless the turn is B and the message exists, is by B and is voice-typed. Otherwise: `voice.turnOff()`, then delete the message (through the existing `deleteMessage`, so drafts' quotes are re-indexed), then `drafts.appendText(B, message.plainText)`; if B's draft has no quote and the message had one whose message still exists, set it.

- [ ] **Step 1: Write the failing `VoiceTypingTest` cases** (with `StandardTestDispatcher`, `FakeSpeechRecogniser`, `FakeAudioSource`, a list collecting utterances):
  - `pauseSendsAnUtterance`: script `[Partial("yeah"), Utterance("Yeah")]`; turn on, `update(true)`, advance; utterances are `["Yeah"]` and `live == ""`.
  - `shortUtterancesAreKept`: an `Utterance("Tuesday")` is sent.
  - `blankUtterancesAreDropped`: `Utterance("  ")` sends nothing.
  - `turnAwayMidUtteranceCommitsAsB`: `Partial("I was going to")`, then `update(false)`; utterances are `["I was going to"]`, `listening == false`, `on == true`, audio closed.
  - `stopBeforeEndCommitsLiveText`: same with `stop()`.
  - `turningOffClosesTheEngine`: after `turnOff()`, the recogniser `isClosed`; turning on again creates a new one (count creations).
  - `failedStartTurnsOffAndFlagsFailure`: `FakeAudioSource(failWith = AudioUnavailableException())`; after `update(true)`, `on == false`, `unavailable == true`. Same for a `createRecogniser` that throws.
  - `restoredOnStateWaitsForResume`: a `SavedStateHandle` with `voice_typing = true`; a new `VoiceTyping` has `on == true`, `listening == false`, and `FakeAudioSource.opened == 0` until `update(true)`.
  - `notListeningWhileOff`: `update(true)` with `on == false` opens no audio.
- [ ] **Step 2: Write the failing `ChatViewModelTest` cases:**
  - `utterancesAreAppendedAsVoiceTypedFromB`: with turn B, an utterance appends a message with `author == B` and `voiceTyped == true`.
  - `turningToAStopsListening`: turn on on B's turn with listening allowed; `selectTurn(A)`; audio closed; any live text committed as B.
  - `returningToBListensAgain`: then `selectTurn(B)`; audio opened again without `turnOn`.
  - `editAppendsToDraftAndUnlinksQuotes`: B's draft is "Sorry"; message 1 is voice-typed "Thursday" and message 2 quotes it; `editVoiceMessage(1)`; voice is off, messages no longer contain "Thursday", message 2's quote has `messageId == null` and its text, B's draft text is "Sorry Thursday".
  - `editDoesNothingOnTheDeafUsersTurn` and `editIgnoresTypedMessages`.
- [ ] **Step 3: Run** `./gradlew :app:testDebugUnitTest --tests '*VoiceTypingTest' --tests '*ChatViewModelTest'`. Expected: FAIL.
- [ ] **Step 4: Implement** `VoiceTyping`, `ChatDrafts.appendText` and the view model changes. Recompute `canListen` in the view model from the conversation flow plus the two flags from `setListeningAllowed`.
- [ ] **Step 5: Run** the same command, then `./gradlew check`. Expected: PASS.
- [ ] **Step 6: Commit.**

### Task 6: Composer, message list and help

**Files:**
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/Composer.kt`, `ChatScreen.kt`, `MessageList.kt`, `MessageMenu.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `help/side-by-side.md`
- Test: `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatScreenTest.kt`, `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatScreenshotTest.kt` (new golden images recorded with `./gradlew recordRoborazziDebug`)

**Interfaces:**
- Consumes: `ChatUiState.Ready.voice`, `ChatViewModel.voice`, `setListeningAllowed`, `editVoiceMessage` (Task 5); `rememberMicrophonePermission()` (Task 4).
- Produces:
  - `ComposerActions.onToggleVoice: () -> Unit` and a `voice: VoiceTypingState?` parameter on `Composer` (null on A's turn, so A never sees the mic).
  - `MessageActions.onEdit: (Int) -> Unit` and `MessageList(..., editable: (Message) -> Boolean)`.
  - Strings (draft copy, for the owner to approve):
    - `chat_voice_on`: "Voice typing is on. Speak clearly towards the phone."
    - `chat_voice_start`: "Start voice typing" (mic button label, off)
    - `chat_voice_stop`: "Stop voice typing" (mic button label, on)
    - `chat_voice_unavailable`: "Voice typing isn't available right now. You can still type."
    - `chat_voice_permission`: "To use voice typing, allow Deaftly to use the microphone. What's said is turned into text on this phone, and the audio isn't kept."
    - `chat_voice_open_settings`: "Open settings"
    - `chat_edit_action`: "Edit"

UI behaviour:
- The mic button sits next to Send, built from `SendButton`'s style (`container = colours.strong`, `content = colours.onStrong`), only when `voice != null`. Its label switches between start and stop; its `stateDescription` says on or off.
- While `voice.on`: the message field is hidden, formatting, template and Send are disabled, and the status line `chat_voice_on` shows with `liveRegion = Polite` (TalkBack announces it once when voice typing turns on). `voice.unavailable` shows `chat_voice_unavailable` instead.
- `voice.live` shows as a trailing line in the message list in B's style, without a live region (so TalkBack doesn't read every partial), with `traversalIndex` after the last message.
- Permission: tapping the mic with `NotAsked` or `Denied` shows `chat_voice_permission` and requests; `DeniedForGood` shows it with `chat_voice_open_settings`; `Granted` toggles. `ChatScreen` calls `setListeningAllowed(foreground, access == Granted)` from a `LifecycleResumeEffect` and whenever access changes.
- Ending the chat calls `viewModel.voice.stop()` before `requestEnd` reads `conversation.messages`.
- On a message where `editable(message)` is true (B's turn, B's voice-typed message), a plain tap calls `onEdit`; the long-press menu and the TalkBack custom actions gain `chat_edit_action` before Copy.

- [ ] **Step 1: Write the failing `ChatScreenTest` cases:** `micShowsOnlyOnTheHearingPersonsTurn`, `voiceOnHidesTheFieldAndDisablesTheOtherButtons`, `voiceOnShowsTheStatusLine`, `liveTextShowsInTheMessageList`, `tappingAVoiceMessageEditsItOnTheHearingPersonsTurn`, `tappingAVoiceMessageOnTheDeafUsersTurnDoesNothing`, `editIsInTheMenuAndTalkBackActions`, `deniedForGoodOffersSettings`, `unavailableShowsTheFallbackLine`, `endingWhileListeningShowsKeepDialog` (live text only, then End: the keep dialog appears).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*ChatScreenTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** the UI.
- [ ] **Step 4: Add screenshot cases** to `ChatScreenshotTest`: `voiceOff`, `voiceListeningWithLiveText`, `voiceUnavailable`, `voicePermission`, in the existing light, dark and large font scale variants. Record with `./gradlew recordRoborazziDebug` and look at every new image.
- [ ] **Step 5: Run** `./gradlew check`. Expected: PASS, including Android Lint accessibility checks and contrast tests.
- [ ] **Step 6: Update `help/side-by-side.md`:** how to start and stop voice typing, that it's for the other person's turn, that a pause sends, tap to edit, and the Edit command in the long-press list. Fix the existing "Long-press a message to Copy, Quote or Delete" line to include Edit.
- [ ] **Step 7: Show the owner** the new strings and screenshots; apply their copy changes.
- [ ] **Step 8: Run on a real phone** (OnePlus 13, then the Pixel 6a): speak, pause, switch turns mid-sentence, background and resume, refuse and grant the permission, end the chat while speaking. Record what you saw in the PR.
- [ ] **Step 9: Commit** and open the PR for Tasks 5 and 6.

### Task 7: Website privacy page (oneloneraven/deaftly-website)

**Files:**
- Modify: `views/privacy/index.md` in `oneloneraven/deaftly-website`

- [ ] **Step 1:** Add the microphone: used only for voice typing in side by side chat, when the other person turns it on; speech is turned into text on the phone; audio is never stored or sent; the text is kept like a typed message, only if the chat is kept. Match the wording in `docs/privacy.md`.
- [ ] **Step 2:** Run the website's checks, open the PR, and merge it once the app release that ships voice typing is ready.

## Still owed before release

These come from #59 and aren't tasks of this plan; track them on #59:
- Measurement on a mid-range phone: Pixel 6a as an optimistic proxy, Galaxy A-series preferred.
- Real recordings of hearing speakers in real places, with their consent, kept out of git. Tune `PAUSE_SECONDS` from them.
