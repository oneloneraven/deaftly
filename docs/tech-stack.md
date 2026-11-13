# Deaftly – Tech Stack (MVP)

Status: Approved (2026-11-07)
Inputs: [requirements.md](requirements.md), [research.md](research.md), mockups (private canvas)

The repository is set up with the `project-bootstrap` plugin, using the `core` module plus a new `android-toolchain` module (decision D9). The plugin's rules apply throughout: workflow, versioning, coding standards, definition of done, privacy, threat model, and dependency screening (ADR-0002). This document records only what is specific to Deaftly. Each significant choice becomes an ADR once the repository exists.

## 1. Drivers

| Driver | Source |
|--------|--------|
| Android only | C1 |
| Offline, on-device, no backend, no telemetry | C3, C4, N7 |
| Live, low-latency OCR (under 500 ms) | O1, N2 |
| Fast start (under 1 s); accessible (WCAG 2.2 AA, system font scale) | N1, N3 |
| Simplest thing that works | KISS principle |
| Dependencies screened against the BDS lists | Plugin ADR-0002 |

## 2. Application stack

| Concern | Choice | Why |
|---------|--------|-----|
| Language | **Kotlin** | The standard language for Android. |
| UI | **Jetpack Compose** (Material 3 as a base, themed to the mockups) | Declarative, and the current Android default. Supports the system font scale and handles the rotated face-to-face half (H9). |
| Architecture | Single-activity app, MVVM with unidirectional data flow (`ViewModel` + `StateFlow`) | Google's recommended pattern; easy to test. |
| Navigation | **Jetpack Navigation 3** | Stable since November 2025 and built for Compose ([Android Developers Blog](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html)). |
| Dependency injection | **Manual** (one `AppContainer`) | A handful of dependencies doesn't justify a framework (KISS). |
| Camera | **CameraX** (`Preview` + `ImageAnalysis`) | Handles device quirks, freezing a frame (O5) and the torch (O6). |
| OCR | **PP-OCRv5 mobile (PaddleOCR) on ncnn**, behind the `TextRecogniser` interface ([ADR-0010](adr/0010-recognise-text-with-paddleocr-on-ncnn.md)). | See §3. |
| Persistence | **DataStore + kotlinx.serialization (JSON)** for templates and settings | Tens of templates; no database needed. Chats are in memory only (H7). |
| Light, dark and system theme | `UiModeManager.setApplicationNightMode` on Android 12+, **AppCompat** `AppCompatDelegate.setDefaultNightMode` below ([Android Developers](https://developer.android.com/develop/ui/views/theming/darktheme#change-themes)). The stored setting is applied at startup and on every change. | On Android 12+ the system applies the chosen mode to the splash and starting window as well. Below Android 12 the system draws the starting window from the system's own theme before any app code runs, so it can show the other theme for about a second; AppCompat then applies the chosen mode from the activity's first frame. AppCompat is Jetpack (Google), covered by ADR-0002. |
| Rich text | Own minimal span model (bold, italic, highlight, quote) rendered with `AnnotatedString` | Four formats don't justify a library. |
| Help pages | The repository's `help/*.md`, copied into the app's assets at build time by `CopyHelpTask`, rendered with **multiplatform-markdown-renderer** (`com.mikepenz:multiplatform-markdown-renderer-m3`, Apache-2.0), which parses with JetBrains' `markdown` (Apache-2.0) | One source for the website and the app (#187). Screened under ADR-0002 on 2026-11-13: Mike Penz (individual) and JetBrains aren't on the BDS consumer boycott, pressure or No Tech for Apartheid lists. Chosen over compose-richtext (less active) and Markwon (unmaintained since 2021, View-based). |
| Show-mode text sizing | Compose auto-size text with a minimum size, then scrolling | S2. |

### Modules

- `:core` – pure Kotlin: the template model, placeholder parsing and filling, repeatable joins, the span model, and the `TextRecogniser` interface. Kept free of Android dependencies so it could move to Kotlin Multiplatform later.
- `:app` – Android: UI, camera, the OCR implementation, storage.

## 3. OCR spike (D6)

The first engineering work after bootstrap. It compares the two engines on a mid-range device and on an emulator, using the same fixture photos of menus, signs and receipts.

| Criterion | Target |
|-----------|--------|
| Recognition latency per frame | Under 500 ms (N2) |
| Word and line accuracy on the fixtures | Measured and compared |
| Block, line and word bounding boxes | Required for O2 and O3 |
| APK size added | Measured |
| Network activity with no `INTERNET` permission | None observed, and no crashes |
| Screening (ADR-0002) | Recorded for the engine and its inference runtime |

**Screening notes for the spike:**
- **ML Kit** is maintained by Google, which is a BDS pressure target. It is free, but viable alternatives exist and by default it sends usage data to Google ([data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)), so the free-tool exception does not clearly apply.
- **PaddleOCR** is maintained by Baidu and licensed under Apache 2.0; Baidu was not on the lists checked. It needs an inference runtime, and each candidate needs screening:
  - Paddle Lite (Baidu);
  - NCNN (Tencent);
  - MNN (Alibaba);
  - ONNX Runtime, maintained by Microsoft, which is a BDS consumer-boycott priority target. Avoid it.

The spike's outcome is recorded as an ADR.

## 4. Platform targets

| Setting | Value | Why |
|---------|-------|-----|
| `minSdk` | **26** (Android 8.0) | Covers roughly 95% or more of active devices. Android 11 and later alone was about 88% in October 2026 ([Beebom](https://gadgets.beebom.com/news/google-android-17-adoption-figures-october-2026)). |
| `targetSdk` / `compileSdk` | **36** | The current Google Play requirement ([target SDK](https://developer.android.com/google/play/requirements/target-sdk)). Kept even though we don't publish to Play, so a later move to Play needs no work. |
| Permissions | Only what's in `config/permissions-allowlist.txt` ([ADR-0011](adr/0011-review-permissions-against-an-allowlist.md)): today `CAMERA`, requested the first time OCR is used. Merged library permissions not on the list fail the build or are removed. | N6, N7. |
| Backup | Android Auto Backup **enabled** for templates and settings (D7) | Users keep their own templates when they change phone. The backup is controlled by the user in Android settings and encrypted; the app itself runs no sync. |

## 5. Toolchain, testing and CI

These come from the `android-toolchain` module (D9) and the plugin's `core` module:

- **Build:** Gradle (Kotlin DSL) with a version catalog and a checksummed wrapper. JDK 21 toolchain.
- **Quality:** ktlint (via Spotless), detekt, and Android Lint, with accessibility and security checks treated as errors.
- **Tests:**
  - unit tests in `:core`, written test-first;
  - ViewModel tests with coroutines-test and Turbine;
  - Compose UI tests on Robolectric, with accessibility checks;
  - Roborazzi screenshots in light and dark themes and at large font scale;
  - emulator tests (Gradle Managed Devices) for the camera and OCR fixtures.
- **Dependencies:** Dependabot (Gradle and GitHub Actions) with the plugin's 3-day cooldown. Every new dependency is screened first.
- **CI:** GitHub Actions on exact runner images, with every action pinned by commit SHA and least-privilege permissions. The plugin's zizmor, Scorecard and dependency-review workflows, plus GitHub's secret scanning and push protection.
- **Releases:** each PR's `release:` label cuts a `vX.Y.Z` tag on merge. A workflow on the published release builds a **signed APK** and attaches it to the GitHub Release. `versionName` and `versionCode` are derived from the tag; nothing reads a version from a file.
- **Repository:** public (D10).

## 6. Distribution

**Signed APK on GitHub Releases only, for now (D8).** Google Play would mean paying Google, a BDS pressure target, where an alternative exists. The cost is reach: installing from outside the Play Store is a real barrier for the target users. This is in the backlog for review ([#69](https://github.com/oneloneraven/deaftly/issues/69)). F-Droid isn't an option, because the project's licence is non-commercial and so isn't open source.

## 7. Security and privacy

- No network access, enforced by the manifest. Only the launcher activity is exported. R8 on release builds.
- The release signing key is held in GitHub encrypted secrets and created by the owner as a manual step; it is never committed.
- Logs never contain user content (templates, OCR text, chat messages).
- The threat model covers the device, the camera and the release pipeline; there is no backend. The record of processing states that no personal data leaves the device, apart from the user's own Android backup (D7).
- No provider or library names appear in user-facing copy.

## 8. Considered and rejected

| Option | Why not |
|--------|---------|
| Flutter / React Native | Android only, so a cross-platform layer adds nothing. |
| Kotlin Multiplatform now | iOS is out of scope; a pure-Kotlin `:core` keeps the option open. |
| Room, Hilt | More than this app needs (KISS). |
| Tesseract | Too slow and inaccurate for live camera OCR. |
| Google Play (for now) | BDS screening (D8). |
| Renovate | Replaced by the plugin's Dependabot setup. |
