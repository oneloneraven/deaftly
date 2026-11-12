# Threat model

What Deaftly protects, from whom, and how. Update it in the same PR as any
change that adds or moves a trust boundary ([coding standards](coding-standards.md#security)).

## The system

- **The app** runs entirely on the user's Android phone. It has no backend,
  no accounts, and today requests no network permission. Permissions are
  limited to a reviewed allowlist ([ADR-0011](adr/0011-review-permissions-against-an-allowlist.md)).
- **Local storage**: templates and settings in the app's private DataStore
  file ([ADR-0005](adr/0005-store-templates-and-settings-with-datastore.md)).
  Chats are held in memory and discarded when they end.
- **Camera**: the `CAMERA` permission is requested at runtime the first time
  Read text opens, after a plain explanation, and the app works without it
  (the user types instead). Frames go from CameraX to on-device text
  recognition and are never stored. Freezing keeps one frame as a bitmap in
  memory until the user unfreezes or leaves the screen.
- **Android backup**: the template and settings store is included in the
  user's own Android backup and device transfer
  ([ADR-0008](adr/0008-allow-android-backup-of-templates-and-settings.md)).
- **Release pipeline**: GitHub Actions tags releases with the release App
  and builds an APK signed with the upload key, attached to the GitHub
  Release ([ADR-0007](adr/0007-distribute-signed-apks-on-github-releases.md)).
  Users install that APK from outside the Play Store.

## Trust boundaries

| Boundary | Between | What crosses it | Controls |
|---|---|---|---|
| Camera → app | The physical world and the recogniser | Frames of whatever the camera sees, which may include other people's text or faces | Camera access needs the runtime permission, which the user can refuse or revoke (the screen then offers typing or the app settings); the preview is bound to the screen and released on leaving; frames are processed in memory and never stored or logged, and a frozen frame is held in memory only while frozen; recognised text only enters a template when the user selects it and chooses Add to message |
| Kotlin → native recogniser | The app's Kotlin code and the ncnn-based C++ recogniser (JNI) | Pixel buffers from CameraX and the bundled model files | Frames come only from CameraX or the fixture tests and are validated in Kotlin and again in the JNI entry point (positive size within a pixel cap, stride, rotation, buffer capacity), so oversized or inconsistent frames fail before any allocation or read; the models are bundled assets, never downloaded |
| App → Android backup | The app's private storage and the user's backup transport | Templates and settings | Only the store is included, through explicit backup and data-extraction rules; the user controls backup in system settings; Android encrypts it |
| Other apps → the app | Other apps on the device and Deaftly | Intents to exported components | Only the launcher activity is exported; no content providers, deep links or broadcast receivers |
| Release job → GitHub API | `release.yml` and the release GitHub App | Short-lived App token (Contents and Workflows: read and write) to push release tags and create releases | Token minted only in the release job and scoped to this repository; App installed on this repository only; private key held as a repository secret |
| Release environment → artefact job | The `release` environment and `release-artefacts.yml` | The upload keystore and its passwords | Secrets live in the `release` environment, which only `v*` tags can use; the decoded keystore is deleted on every exit path of the job |
| GitHub Release → user's phone | The published APK and the person installing it | The app itself | Signed with an upload key only the maintainer holds; Android refuses updates signed with any other key |

## Assets

| Asset | Why it matters |
|---|---|
| The user's templates and settings | Personal, and may reveal health conditions, names or routines |
| Conversations held in memory | May contain anything two people say to each other |
| Upload signing key and its passwords | Anyone holding it can publish an update that installed copies accept; losing it means installed copies can never update |
| Release App private key | Anyone holding it can mint tokens that push tags and, through Workflows, change workflow files |

## Actors

| Actor | Capability | Motivation |
|---|---|---|
| Someone with the unlocked phone | Opens the app and reads templates | Curiosity, finding out about the user |
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
| Recognised text or messages written to logs | Information disclosure | Logs never contain user content (coding standards), checked in review | Mitigated |
| A crafted or corrupt camera frame corrupts memory or crashes the native recogniser | Tampering, Denial of service | Bounds checks at the JNI entry point reject inconsistent sizes before any pixel is read; the detector caps candidates and the recogniser caps crop width; no frames from other apps or the network; `NativeInputValidationTest` feeds invalid geometry, noise and degenerate frames on every device-test run | Mitigated |
| A tampered ncnn download or model file enters the build | Tampering | `fetchNcnn` refuses an archive whose SHA-256 differs from the pinned value; models are committed with their hashes and the conversion is reproducible from `third_party/ncnn/README.md` | Mitigated |
| Another app drives the app through intents | Elevation of privilege | Only the launcher activity is exported | Mitigated |
| Malformed stored data crashes the app on start | Denial of service | The store is versioned with tested migrations; unreadable data falls back to the built-in templates rather than crashing | Mitigated once #29 ships |
| Leaked release token or App key used to alter workflow files | Elevation of privilege | The Workflows permission is needed because GitHub refuses a tag whose commit's workflow files differ from the default branch's. Tokens last one hour and are minted only in the release job; the key is a repository secret; the App is installed on this repository only | Accepted |
| Leaked upload keystore used to sign a malicious update | Elevation of privilege | Keystore secrets are scoped to the `release` environment, usable only by `v*` tags; the decoded file is deleted after each build; the key is never committed | Mitigated |

## Known gaps

- Repudiation doesn't apply: the app has no accounts or server-side actions to attribute.
