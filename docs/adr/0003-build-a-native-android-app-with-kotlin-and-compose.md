# 3. Build a native Android app with Kotlin and Jetpack Compose

## Status

Accepted

## Context

Deaftly targets Android only for the MVP. It needs fast start-up (under one
second), a live camera with on-device text recognition, and an accessible
interface that honours the system font scale. iOS is out of scope.

Cross-platform frameworks (Flutter, React Native) add a runtime layer and
indirect access to CameraX and on-device machine learning, with no benefit
while there is only one platform.

## Decision

- Kotlin with Jetpack Compose, one activity, and MVVM with unidirectional
  data flow (`ViewModel` and `StateFlow`).
- Jetpack Navigation 3 for navigation; the back stack is plain app state.
- Two Gradle modules: `:core` is pure Kotlin with no Android dependencies
  (template logic, the rich-text span model, the text recognition
  interface); `:app` holds the UI, camera, storage and the recogniser
  implementation.

Keeping `:core` free of Android dependencies means it could move to Kotlin
Multiplatform if iOS is ever scheduled.

## Screening

The Android SDK, Jetpack (including Compose and Navigation 3) and
Android Lint are Google's. Google is a BDS pressure target and a No Tech
for Apartheid target; they are allowed under the free-tool exception in
[ADR-0002](0002-screen-dependencies-against-bds-boycott-lists.md): free,
industry standard, and with no viable alternative for a native Android app.
Kotlin is JetBrains' and is not listed.

## Consequences

- Direct, idiomatic access to CameraX and on-device recognition.
- Business logic in `:core` is tested on the JVM without Android.
- A future iOS app means either Kotlin Multiplatform for `:core` or a
  separate codebase.
