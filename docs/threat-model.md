# Threat model

What Deaftly protects, from whom, and how. Update it in the same PR as any
change that adds or moves a trust boundary ([coding standards](coding-standards.md#security)).

## The system

- **The app** runs entirely on the user's Android phone. It has no backend
  and no accounts. Its only network use is sending a feedback or issue report
  the user writes, to One Lone Raven's feedback system, when they tap Send.
  Permissions are limited to a reviewed allowlist ([ADR-0011](adr/0011-review-permissions-against-an-allowlist.md)).
- **Local storage**: templates and settings in the app's private DataStore
  file ([ADR-0005](adr/0005-store-templates-and-settings-with-datastore.md)).
  Chats are held in memory and discarded when they end, unless the user
  keeps one; each kept chat is a file in the app's private `chats/` folder.
- **Camera**: the `CAMERA` permission is requested at runtime the first time
  Read text opens, after a plain explanation, and the app works without it
  (the user types instead). Frames go from CameraX to on-device text
  recognition and are never stored. Freezing keeps one frame as a bitmap in
  memory until the user unfreezes or leaves the screen.
- **Microphone**: the `RECORD_AUDIO` permission is requested at runtime the
  first time the hearing person turns on voice typing in a side by side chat,
  after a plain explanation, and the app works without it (the user types
  instead). Audio is read only while voice typing is on, it is the hearing
  person's turn and the app is in the foreground. It goes from the microphone
  to an on-device speech recogniser in memory and is never stored, logged or
  sent anywhere. Only the recognised text is kept, as a chat message.
- **Android backup**: the template and settings store and kept chats are
  included in the user's own Android backup and device transfer
  ([ADR-0012](adr/0012-back-up-kept-chats.md)).
- **Release pipeline**: GitHub Actions tags releases with the release App
  and builds an APK signed with the app signing key, attached to the GitHub
  Release. It also builds an app bundle signed with a separate Play upload key
  and uploads it to Google Play's internal testing track with a Play service
  account ([ADR-0014](adr/0014-distribute-through-google-play-and-github-releases.md)).
  Google re-signs Play's APKs with its copy of the same app signing key, so
  people can install from either channel.

## Trust boundaries

| Boundary | Between | What crosses it | Controls |
|---|---|---|---|
| Camera → app | The physical world and the recogniser | Frames of whatever the camera sees, which may include other people's text or faces | Camera access needs the runtime permission, which the user can refuse or revoke (the screen then offers typing or the app settings); the preview is bound to the screen and released on leaving; frames are processed in memory and never stored or logged, and a frozen frame is held in memory only while frozen; recognised text only enters a template when the user selects it and chooses Add to message |
| Microphone → app | The physical world and the speech recogniser | Audio of whatever the microphone hears, which may include other people's speech | Microphone access needs the runtime permission, which the user can refuse or revoke (the chat then works by typing, or the user is offered the app settings); the microphone is open only while voice typing is on, it is the hearing person's turn and the app is in the foreground, and it is released when any of those stops; audio is processed in memory in short chunks and never stored, logged or sent; recognised text becomes a chat message the user can edit or delete, and is stored only if the user keeps the chat |
| Kotlin → native recogniser | The app's Kotlin code and the ncnn-based C++ recogniser (JNI) | Pixel buffers from CameraX, audio chunks from the microphone and the bundled model files | Frames come only from CameraX or the fixture tests and are validated in Kotlin and again in the JNI entry point (positive size within a pixel cap, stride, rotation, buffer capacity), so oversized or inconsistent frames fail before any allocation or read; audio chunks are bounded in length at the JNI entry point; the models are bundled in the APK at build time, and the app never downloads them |
| App → feedback system | The user's phone and One Lone Raven's feedback system | A report the user writes: the message, app version, and an email address, quote consent and name if given. The server sees the request's IP address | Sent only when the user taps Send, over HTTPS to a fixed address, by `HttpFeedbackSender`, which is the app's only network code. The screen asks people not to include health details. Engage validates every field, rate-limits by IP, caps app submissions per day and never echoes content back in errors. Because `INTERNET` is granted to the whole app, a dependency could also use the network: dependencies are screened before adoption ([ADR-0002](adr/0002-screen-dependencies-against-bds-boycott-lists.md)), and the device tests check that recognition sends nothing |
| App → Android backup | The app's private storage and the user's backup transport | Templates, settings and kept chats | Only the store and the `chats/` folder are included, through explicit backup and data-extraction rules; the user controls backup in system settings; Android encrypts it |
| Other apps → the app | Other apps on the device and Deaftly | Intents to exported components | Only the launcher activity is exported; no content providers, deep links or broadcast receivers. Its app shortcut extra only picks one of a fixed set of screens to open |
| Release job → GitHub API | `release.yml` and the release GitHub App | Short-lived App token (Contents and Workflows: read and write) to push release tags and create releases | Token minted only in the release job and scoped to this repository; App installed on this repository only; private key held as a repository secret |
| Release environment → artefact job | The `release` environment and `release-artefacts.yml` | The upload keystore and its passwords | Secrets live in the `release` environment, which only `v*` tags can use; the decoded keystore is deleted on every exit path of the job |
| Release environment → bundle step | The `release` environment and the Play bundle step | The Play upload keystore and its passwords | Same controls as the APK keystore: `release` environment, `v*` tags only, decoded keystore deleted on every exit path. Without the secrets, no bundle is built |
| Artefact job → Google Play API | `release-artefacts.yml` and Play Console | The app bundle, using the service account's JSON key | The service account has access to this app only, with permission to release to testing tracks only; uploads run only when `PLAY_UPLOADS_ENABLED` is `true`; the key is a `release` environment secret; promotion to closed testing and production is done by hand in Play Console |
| GitHub Release → user's phone | The published APK and the person installing it | The app itself | Signed with the app signing key; Android refuses updates signed with any other key |
| Google Play → user's phone | Play's delivered APKs and the person installing them | The app itself | Signed by Google with its copy of the same app signing key, so Google is trusted not to sign anything else; Play's review applies to closed testing, open testing and production releases |

## Assets

| Asset | Why it matters |
|---|---|
| The user's templates and settings | Personal, and may reveal health conditions, names or routines |
| Conversations held in memory | May contain anything two people say to each other |
| Kept chats on the device | Stored conversations, possibly about health, kept until the user deletes them |
| App signing key and its passwords | Anyone holding it can publish an update that installed copies accept; losing it means GitHub installs can never update. Google holds a copy for Play ([ADR-0014](adr/0014-distribute-through-google-play-and-github-releases.md)) |
| Play upload key and its passwords | Anyone holding it can upload a bundle to Play as us, though only to testing tracks through CI; Google can reset it |
| Play service account key | Anyone holding it can upload builds to this app's testing tracks |
| Release App private key | Anyone holding it can mint tokens that push tags and, through Workflows, change workflow files |

## Actors

| Actor | Capability | Motivation |
|---|---|---|
| Someone with the unlocked phone | Opens the app and reads templates and kept chats | Curiosity, finding out about the user |
| Malicious app on the same phone | Sends intents to exported components | Data theft |
| Compromised dependency | Code execution at build or runtime | Data theft, exfiltration |
| Attacker distributing a fake APK | Publishes a look-alike app elsewhere | Malware delivery to deaf users |
| Maintainer account takeover | Repository and platform access | Anything |

## Threats (STRIDE)

| Threat | Category | Mitigation | Status |
|---|---|---|---|
| A look-alike APK published elsewhere | Spoofing | Releases come only from this repository's GitHub Releases, signed with the upload key; the install guide (#57) tells users to install only from there and how to check the signer | Mitigated once #57 ships |
| An update signed with a different key | Tampering | Android only installs updates signed with the installed app's key; the key is held offline by the maintainer with a backup | Mitigated |
| A compromised dependency exfiltrates data | Information disclosure | The merged release manifest may only request allowlisted permissions, enforced in CI (ADR-0011), and `INTERNET` isn't on the list; dependencies are pinned, screened and updated through Dependabot with a cooldown | Mitigated |
| Templates read by someone with the unlocked phone | Information disclosure | Out of the app's control: protected by the phone's lock screen | Accepted |
| Templates exposed through the user's backup | Information disclosure | Backup is the user's own, encrypted by Android and controlled in system settings; only the store is included | Accepted |
| Kept chats read by someone with the unlocked phone, or exposed through the user's backup | Information disclosure | Only chats the user chooses to keep are stored; the phone's own lock guards them; backup is the user's own and encrypted by Android; the user can delete a kept chat. No separate app lock | Accepted |
| Recognised text or messages written to logs | Information disclosure | Logs never contain user content (coding standards), checked in review | Mitigated |
| A crafted or corrupt camera frame corrupts memory or crashes the native recogniser | Tampering, Denial of service | Bounds checks at the JNI entry point reject inconsistent sizes before any pixel is read; the detector caps candidates and the recogniser caps crop width; no frames from other apps or the network; `NativeInputValidationTest` feeds invalid geometry, noise and degenerate frames on every device-test run | Mitigated |
| A tampered ncnn download, source tarball or model file enters the build | Tampering | `fetchNcnn`, the sherpa-ncnn source fetches and `fetchSpeechModel` each refuse an archive whose SHA-256 differs from the pinned value; the OCR models are committed with their hashes and their conversion is reproducible from `third_party/ncnn/README.md`; the speech model is a release asset of this repository, built from the published checkpoint as recorded in `third_party/sherpa-ncnn/README.md` | Mitigated |
| A corrupt or missing speech model, or malformed audio, crashes the native recogniser | Tampering, Denial of service | The model ships inside the APK; before loading, the app checks that each model file exists and has the recorded size, because the native recogniser cannot report errors; the JNI entry point refuses empty input, input over 10 seconds and unknown or released handles; `SpeechNativeInputValidationTest` covers these on every device-test run | Mitigated |
| Speech is heard when the user does not expect it | Information disclosure | The microphone is opened only after the user turns voice typing on and grants the permission; listening stops when the turn passes, the chat ends or the app leaves the foreground; the screen shows when it is listening; the app has no background service | Mitigated |
| Spoken words reach a log, a file or the network | Information disclosure | Audio and partial text are held in memory only; the app has no `INTERNET` permission (ADR-0011); logs never contain user content | Mitigated |
| Recognised speech exposed through a kept chat or the user's backup | Information disclosure | Recognised text is stored only as a message in a chat the user chooses to keep, with the same controls as other kept chats (ADR-0012) | Accepted |
| Another app already holds the microphone, or recording fails to start | Denial of service | Voice typing turns itself off and says it is unavailable; typing is unaffected and the app does not crash | Mitigated |
| Another app drives the app through intents | Elevation of privilege | Only the launcher activity is exported. An app can send the shortcut extra too, but it only opens Start typing, Face to face or Side by side over the home screen, carries no data, and is ignored before first run | Mitigated |
| Malformed stored data crashes the app on start | Denial of service | The store is versioned with tested migrations; unreadable data falls back to the built-in templates rather than crashing | Mitigated once #29 ships |
| Leaked release token or App key used to alter workflow files | Elevation of privilege | The Workflows permission is needed because GitHub refuses a tag whose commit's workflow files differ from the default branch's. Tokens last one hour and are minted only in the release job; the key is a repository secret; the App is installed on this repository only | Accepted |
| Leaked upload keystore used to sign a malicious update | Elevation of privilege | Keystore secrets are scoped to the `release` environment, usable only by `v*` tags; the decoded file is deleted after each build; the key is never committed | Mitigated |

## Known gaps

- Repudiation doesn't apply: the app has no accounts or server-side actions to attribute.
