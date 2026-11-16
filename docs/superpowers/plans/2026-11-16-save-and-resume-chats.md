# Save and Resume Chats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user keep a side by side chat when it ends, list kept chats, reopen one and carry on, and delete it.

**Architecture:** A file-per-chat store (`filesDir/chats/<id>.json`, written with `AtomicFile`) holds kept chats in the existing `ChatCodec` format with a small wrapper. A `ChatKeeper` helper in the chat view model saves a kept chat after every change. A new Saved chats screen lists and deletes kept chats, and Home shows a card for it once one exists. Android backup includes the `chats/` folder.

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx.serialization, `androidx.core.util.AtomicFile`, coroutines, Robolectric and Roborazzi tests.

**Spec:** `docs/superpowers/specs/2026-11-16-save-and-resume-chats-design.md`

## Global Constraints

- Side by side only. Face to face is unchanged.
- Nothing is stored unless the user chooses Keep. A chat with no messages ends with no dialog and isn't kept.
- Kept chats are in Android cloud backup and device transfer.
- No new dependencies (`androidx.core` is already on the classpath; check before assuming).
- File work runs on an injected `CoroutineDispatcher` (Dispatchers.IO in the app), never the main thread.
- No migrations or backwards compatibility before launch.
- Copy: British English, no en or em dashes, no supplier names. Name "Android backup", not a provider.
- No comments by default; one line for a "why" the code can't express.
- Every change goes through an issue, a branch and a PR (`docs/workflow.md`). Suggested PRs: Tasks 1 and 2 together, Task 3, Task 4, Task 5 (website repo).
- `./gradlew check` passes before every push.

## Review Focus

1. **A corrupt or half-written chat file** (truncated JSON, unknown fields, empty file): the list skips it and the app doesn't crash. Test in Task 1.
2. **Quick successive changes in a kept chat** (two sends in a row): saves run one at a time and the file ends with the latest conversation. Test in Task 2.
3. **Process death while a kept chat is open:** the restored chat still knows its ID and keeps saving to the same file. Test in Task 2.
4. **Opening a kept chat whose file is gone** (deleted, or restored from an older backup): the chat opens as a new, unkept chat instead of crashing. Test in Task 2.
5. **An ID that isn't one of ours** (route or saved state tampered with, e.g. `../templates`): the store refuses it and touches no file outside `chats/`. Test in Task 1.

---

### Task 1: Kept chat store, backup rules and privacy docs

**Files:**
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/storage/SavedChatStore.kt`
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/ChatCodec.kt` (add saved-chat encode/decode)
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/AppContainer.kt` (expose the store)
- Modify: `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`
- Create: `docs/adr/0012-back-up-kept-chats.md`; Modify: `docs/adr/0008-allow-android-backup-of-templates-and-settings.md` (status: superseded by 0012), `docs/adr/README.md` (index, if it lists ADRs)
- Modify: `docs/requirements.md` (H7), `docs/data-protection.md` (chat messages row), `docs/threat-model.md`
- Test: `app/src/test/kotlin/com/ravendarque/deaftly/storage/SavedChatStoreTest.kt`, `app/src/test/kotlin/com/ravendarque/deaftly/storage/BackupRulesTest.kt`

**Interfaces:**
- Produces:
  - `data class SavedChatSummary(val id: String, val otherName: String?, val lastMessage: String?, val changedAt: Long)`
  - `data class SavedChat(val id: String, val conversation: Conversation, val changedAt: Long)`
  - `interface SavedChatStore { val summaries: StateFlow<List<SavedChatSummary>>; suspend fun load(id: String): SavedChat?; suspend fun save(id: String, conversation: Conversation); suspend fun delete(id: String); fun newId(): String }`
  - `class FileSavedChatStore(dir: File, io: CoroutineDispatcher, scope: CoroutineScope, clock: () -> Long = System::currentTimeMillis) : SavedChatStore`
  - `ChatCodec.encodeSaved(conversation: Conversation, changedAt: Long): String` and `ChatCodec.decodeSaved(encoded: String): Pair<Conversation, Long>?` (null on any parse failure)
  - `AppContainer.savedChatStore: SavedChatStore` (lazy, dir `File(context.filesDir, "chats")`)
- A fake for later tasks: `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/FakeSavedChatStore.kt`, in memory, same interface, with `saves: Int` for assertions.

- [ ] **Step 1: Write the failing store tests** in `SavedChatStoreTest` (Robolectric not needed; use a JUnit `TemporaryFolder`, `UnconfinedTestDispatcher`, and a clock you control):
  - `savingThenLoadingRestoresTheConversationExactly`: save a conversation with named participants, two messages (one bold span, one quoting the first) and turn B; `load(id)` returns an equal `Conversation` and `changedAt == clock()`.
  - `summariesListNewestFirstWithOtherNameAndLastLine`: save chats at times 1 and 2; `summaries.value.map { it.id }` is `[second, first]`; the other person's name is `participantB.name`; `lastMessage` is the last message's plain text, or null for no messages.
  - `deleteRemovesTheFileAndTheSummary`: after `delete(id)`, `File(dir, "$id.json").exists()` is false, `load(id)` is null and `summaries.value` doesn't contain it.
  - `aCorruptFileIsSkipped` (Review Focus 1): write `"{\"version\":1,"` to `dir/<valid-id>.json` and an empty file to another; `summaries.value` is empty after the initial load and `load` returns null for both.
  - `refusesIdsThatAreNotItsOwn` (Review Focus 5): `save("../templates", c)` throws `IllegalArgumentException`; no file is created outside `dir`; `load("../templates")` is null.
  - `newIdsAreDistinctAndAccepted`: two `newId()` values differ and both can be saved.
- [ ] **Step 2: Run them and see them fail.** Run: `./gradlew :app:testDebugUnitTest --tests '*SavedChatStoreTest'`. Expected: compilation failure (types don't exist).
- [ ] **Step 3: Implement.** The file format is `{"version":1,"changedAt":<ms>,"conversation":<ConversationDto>}` via a new private `@Serializable SavedChatDto` in `ChatCodec.kt`. IDs are `UUID.randomUUID().toString()`; accept only `^[0-9a-f-]{36}$`. Write with `AtomicFile(File(dir, "$id.json"))` (`startWrite`/`finishWrite`, `failWrite` on exception). Serialise writes with a `Mutex`. Load all summaries once on creation (in `scope`, on `io`), then update `summaries` after each save and delete instead of re-reading the folder.
- [ ] **Step 4: Run the store tests.** Expected: PASS.
- [ ] **Step 5: Update the backup rules and their test.** Add `<include domain="file" path="chats/" />` after the template store line in `backup_rules.xml` and in both sections of `data_extraction_rules.xml`. In `BackupRulesTest`, the expected rules become `listOf(storeRule, "include" to ("file" to "chats/"))`, and the test names say "template store and kept chats". Run `./gradlew :app:testDebugUnitTest --tests '*BackupRulesTest'`. Expected: PASS.
- [ ] **Step 6: Docs.**
  - ADR-0012 "Back up kept chats": context (the two reasons from the spec), decision (kept chats in `chats/`, included in cloud backup and device transfer; supersedes ADR-0008), consequences (health conversations can be in the user's Android backup, encrypted by Android and under their control; the rules must change if the folder moves).
  - H7: "Chats are discarded when they end unless the user keeps them. Kept chats are stored on the device and in the user's Android backup, and can be reopened, carried on and deleted. *(Issue #62)*"
  - The data-protection chat messages row: kept chats are on the device and in the user's Android backup, until the user deletes them or the app; others stay in memory only.
  - Threat model: change "Chats are held in memory and discarded when they end." to cover kept chats, add kept chats as an asset at rest (readable by anyone with the unlocked phone; in Android backup), and say a separate app lock is out of scope.
- [ ] **Step 7: Run `./gradlew check`.** Expected: BUILD SUCCESSFUL.
- [ ] **Step 8: Commit.** `git add` the files above by name; message "Add a store for kept chats and include it in backup".

### Task 2: Keep, save and reopen in the chat view model

**Files:**
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/ChatKeeper.kt`
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/ChatViewModel.kt`, `app/src/main/kotlin/com/ravendarque/deaftly/navigation/Route.kt` (`Route.Chat`), `app/src/main/kotlin/com/ravendarque/deaftly/AppContainer.kt` (`chatViewModelFactory`), `app/src/main/kotlin/com/ravendarque/deaftly/navigation/AppNavigation.kt` (pass the ID)
- Test: `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatKeeperTest.kt`, `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatViewModelTest.kt`

**Interfaces:**
- Consumes: `SavedChatStore`, `FakeSavedChatStore` (Task 1).
- Produces:
  - `Route.Chat(messages: List<String> = emptyList(), draft: String = "", savedChatId: String? = null)`
  - `ChatOpening(messages, draft, savedChatId: String? = null)`
  - `class ChatKeeper(store: SavedChatStore, savedState: SavedStateHandle, scope: CoroutineScope)` with `val keptId: StateFlow<String?>`, `fun keep(conversation: Conversation)`, `fun persist(conversation: Conversation)` (no-op when not kept), `suspend fun open(id: String): Conversation?` (sets `keptId` when found)
  - `ChatViewModel(settings, savedState, opening, insertions, store: SavedChatStore)`, with `val keeper: ChatKeeper`
  - `ChatUiState.Ready` gains `val kept: Boolean = false`

`ChatViewModel` has 10 functions and detekt's limit is 11. Keeping lives in `ChatKeeper`, the way `ChatDisplay` and `ChatDrafts` hold display and draft state, and the screen calls `viewModel.keeper.keep(conversation)` directly. `keep` on an already-kept chat just saves.

- [ ] **Step 1: Write the failing tests.**
  - `ChatKeeperTest.persistDoesNothingUntilKept`: `persist(c)` then `store.saves == 0`; `keep(c)` then `store.saves == 1` and `keptId.value != null`.
  - `ChatKeeperTest.theIdSurvivesRecreationFromSavedState` (Review Focus 3): keep with one `SavedStateHandle`, build a second `ChatKeeper` on the same handle; its `keptId.value` is the same, and `persist` writes to that ID.
  - `ChatKeeperTest.quickSuccessiveSavesEndWithTheLatest` (Review Focus 2): keep, then `persist(c1)` and `persist(c2)` with no yield between; `store.load(id)!!.conversation == c2`.
  - `ChatViewModelTest.everyChangeToAKeptChatIsSaved`: keep, then `send()` a message, `deleteMessage(0)`, `updateParticipant(...)`, `selectTurn(B)`; after each, the store's copy equals `conversation()`.
  - `ChatViewModelTest.anUnkeptChatIsNeverSaved`: send two messages, end; `store.saves == 0`.
  - `ChatViewModelTest.reopeningRestoresTheConversationAndIsKept`: store holds conversation X under ID; a view model with `ChatOpening(savedChatId = id)` has `conversation() == X` and `state.kept == true`.
  - `ChatViewModelTest.reopeningAMissingChatStartsAFreshUnkeptOne` (Review Focus 4): `ChatOpening(savedChatId = "<valid but absent id>")` gives the same conversation as a new chat and `kept == false`.
- [ ] **Step 2: Run them and see them fail.** Run: `./gradlew :app:testDebugUnitTest --tests '*ChatKeeperTest' --tests '*ChatViewModelTest'`. Expected: compilation failure.
- [ ] **Step 3: Implement.** Store the kept ID in saved state under `"keptChatId"`. In `ChatViewModel.change`, after updating saved state, call `keeper.persist(it)`. In `init`, if there's no conversation in saved state and `opening.savedChatId` is set, try `keeper.open(id)` before starting a fresh one. `persist` launches in `scope` and serialises with a `Mutex` or a conflated channel; the store's own `Mutex` covers file safety.
- [ ] **Step 4: Run the tests.** Expected: PASS, including the existing `ChatViewModelTest` cases and every construction site (`WebsiteScreenshotTest`, `BubbleTailTest`, `ChatScreenshotTest`, `ChatScreenTest`) updated to pass a `FakeSavedChatStore()`.
- [ ] **Step 5: Run `./gradlew check`.** Expected: BUILD SUCCESSFUL.
- [ ] **Step 6: Commit.** "Save kept chats as they change and reopen them".

### Task 3: Ending a chat offers Keep

**Files:**
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/EndChatDialog.kt`
- Modify: `app/src/main/kotlin/com/ravendarque/deaftly/ui/chat/ChatScreen.kt` (`requestEnd` and the dialog), `app/src/main/res/values/strings.xml`
- Remove: the now-unused `chat_end_text` and `chat_end_confirm` strings, if nothing else uses them (also in `values-*` overrides)
- Test: `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatScreenTest.kt`, `app/src/test/kotlin/com/ravendarque/deaftly/ui/chat/ChatScreenshotTest.kt`

**Interfaces:**
- Consumes: `ChatUiState.Ready.kept` and `ChatKeeper.keep` from Task 2.
- Produces: `@Composable fun EndChatDialog(onKeep: () -> Unit, onDelete: () -> Unit, onKeepChatting: () -> Unit)`.

Copy (exact):
- Title: "Keep this chat?"
- Text: "Kept chats stay on this phone and in your Android backup, so you can read them or carry on later."
- Buttons, stacked full width: "Keep" (primary), "Delete" (destructive style), "Keep chatting" (text). Back or tapping outside means Keep chatting.

`requestEnd` becomes: kept → keep (saves) and `onEnd()`, no dialog; no messages → `onEnd()`; otherwise show `EndChatDialog`. The system back button uses the same path, as it does today.

- [ ] **Step 1: Write the failing tests** in `ChatScreenTest`:
  - `endingAChatWithMessagesOffersKeepDeleteAndKeepChatting`: all three buttons exist; accessibility checks pass.
  - `keepSavesAndEnds`: tap Keep; `store.saves == 1` and `onEnd` called once.
  - `deleteEndsWithoutSaving`: tap Delete; `store.saves == 0`, `onEnd` called once.
  - `keepChattingClosesTheDialogOnly`: tap Keep chatting; dialog gone, `onEnd` not called.
  - `endingAKeptChatSavesWithoutAsking`: reopen a kept chat, tap End chat; no dialog, `onEnd` called, latest conversation in the store.
  - `endingAnEmptyChatAsksNothing`: unchanged behaviour, still passes.
- [ ] **Step 2: Run and see them fail.** Run: `./gradlew :app:testDebugUnitTest --tests '*ChatScreenTest'`. Expected: FAIL (old dialog).
- [ ] **Step 3: Implement** `EndChatDialog` using the same dialog surface, colours and button components as `ConfirmDialog` (`ui/editor/EditorFields.kt`), and wire `requestEnd`.
- [ ] **Step 4: Add screenshots** `endChatDialogLightTheme`, `endChatDialogDarkTheme`, `endChatDialogLargeFontScale` to `ChatScreenshotTest`; record with `./gradlew :app:recordRoborazziDebug --tests '*ChatScreenshotTest'`; review by eye; commit only the new images (re-recording causes harmless churn in the others; revert it).
- [ ] **Step 5: Run `./gradlew check`.** Expected: BUILD SUCCESSFUL.
- [ ] **Step 6: Commit.** "Offer to keep a chat when it ends".

### Task 4: Saved chats list, Home card and help

**Files:**
- Create: `app/src/main/kotlin/com/ravendarque/deaftly/ui/savedchats/SavedChatsScreen.kt`, `app/src/main/kotlin/com/ravendarque/deaftly/ui/savedchats/SavedChatsViewModel.kt`
- Modify: `navigation/Route.kt` (`Route.SavedChats`), `navigation/AppNavigation.kt`, `ui/hub/HubScreen.kt`, `AppContainer.kt` (factory), `res/values/strings.xml`, `help/side-by-side.md`, `.github/scripts/check-help.mjs` inputs only if that script requires a help entry for new screens (check it)
- Test: `app/src/test/kotlin/com/ravendarque/deaftly/ui/savedchats/SavedChatsScreenTest.kt`, `.../SavedChatsViewModelTest.kt`, `.../SavedChatsScreenshotTest.kt`, `app/src/test/kotlin/com/ravendarque/deaftly/ui/hub/` (existing Hub test file)

**Interfaces:**
- Consumes: `SavedChatStore.summaries`, `SavedChatStore.delete` (Task 1); `Route.Chat(savedChatId = id)` (Task 2).
- Produces:
  - `Route.SavedChats` (`@Serializable data object`)
  - `class SavedChatsViewModel(store: SavedChatStore)` with `val chats: StateFlow<List<SavedChatSummary>>` and `fun delete(id: String)`
  - `@Composable fun SavedChatsScreen(viewModel: SavedChatsViewModel, onOpen: (String) -> Unit, onBack: () -> Unit)`
  - `HubScreen(onNavigate: (Route) -> Unit, hasSavedChats: Boolean)`. AppNavigation collects `container.savedChatStore.summaries` and passes `isNotEmpty()`.

Behaviour and copy:
- Home's Chat group: "Saved chats" card after Side by side, help text "Read or carry on chats you kept", shown only when `hasSavedChats`. Icon: reuse `DeaftlyIcons.Chat`.
- Screen title "Saved chats", with a Back bar like Templates.
- Row: the other person's name (or "Them"), the date of the last change (`DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` in the device locale), and the last message cut to one line (or "No messages"). Tapping the row opens the chat.
- A trailing delete icon button (`DeaftlyIcons.Trash`, content description "Delete chat with %1$s") asks "Delete this chat?" / "It will be removed from this phone and your Android backup." / Delete (destructive) / Cancel, using `ConfirmDialog`.
- Empty state, after deleting the last chat: "No saved chats. When you end a chat, choose Keep to save it here."
- Help (`help/side-by-side.md`): a short "Keeping a chat" section covering Keep when ending, Saved chats on Home, carrying on, deleting, and that kept chats are in Android backup if it's on.

- [ ] **Step 1: Write the failing tests.**
  - `SavedChatsViewModelTest.listsWhatTheStoreHolds` and `.deleteRemovesTheChat` (with `FakeSavedChatStore`).
  - `SavedChatsScreenTest.showsNameDateAndLastLine`: summary for "Kwame", last line "See you at 6": both texts and the formatted date exist; accessibility checks pass.
  - `.aChatWithNoNameShowsThem` and `.aChatWithNoMessagesShowsNoMessages`.
  - `.tappingARowOpensThatChat`: `onOpen` gets the ID.
  - `.deletingAsksFirstThenRemoves`: tap the delete button, confirm; the row is gone; Cancel leaves it.
  - `.deletingTheLastChatShowsTheEmptyMessage` (spec empty state).
  - The Hub test: `savedChatsCardShowsOnlyWhenThereAreSavedChats`.
- [ ] **Step 2: Run and see them fail.** Run: `./gradlew :app:testDebugUnitTest --tests '*SavedChats*' --tests '*Hub*'`. Expected: compilation failure.
- [ ] **Step 3: Implement** the screen, view model, route, navigation entry (`onOpen = { backStack.add(Route.Chat(savedChatId = it)) }`), Home card and strings, following `TemplatesScreen` for the bar and list styling.
- [ ] **Step 4: Run the tests.** Expected: PASS.
- [ ] **Step 5: Screenshots** `SavedChatsScreenshotTest`: `listLightTheme`, `listDarkTheme`, `listLargeFontScale`, `emptyLightTheme`, using diverse names (e.g. Kwame, Aaliyah, Siobhan). Record, review by eye, and commit only new images and the Hub images that genuinely changed.
- [ ] **Step 6: Run `./gradlew check`.** Expected: BUILD SUCCESSFUL (includes the help checks).
- [ ] **Step 7: Commit.** "List saved chats on Home and let people delete them".

### Task 5: Website privacy page (oneloneraven/deaftly-website)

**Files:**
- Modify: `views/privacy/index.md` in `oneloneraven/deaftly-website`

- [ ] **Step 1:** Follow that repo's own workflow (issue, branch, PR, its checks).
- [ ] **Step 2:** In the "what's kept" table, add a row: "Chats you choose to keep | So you can read them or carry on later | Until you delete them or the app". Say plainly that kept chats, like templates and settings, go into your Android backup if you've turned it on. Change any line saying chats are never stored.
- [ ] **Step 3:** Run the site's tests. Expected: pass.
- [ ] **Step 4:** Open the PR, and merge it only after the app release that ships Task 3 is out.
