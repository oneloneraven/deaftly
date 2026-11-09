# Performance

Covers requirements N1 (the app starts in under 1 second on a mid-range device) and N2 (OCR recognition updates in under 500 ms on a mid-range device). See [requirements](requirements.md).

## Baseline Profile

The `:baselineprofile` module generates a Baseline Profile and holds the startup benchmark. The profile is committed at `app/src/release/generated/baselineProfiles/baseline-prof.txt` (with `startup-prof.txt` for dex layout) and is compiled into release builds by the Android Gradle Plugin. `androidx.profileinstaller` installs it on first launch, which matters because releases are sideloaded APKs rather than Play installs.

The generator walks: cold start (with app data cleared, so the first-run screen is included), Home, a template through Fill to Show, and opening Chat. The OCR camera path is excluded because it needs a camera.

Regenerate after a change to a startup or navigation path:

```
./gradlew :app:generateBaselineProfile -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

The module's managed device uses the `aosp` system image (API 36, x86_64). The `aosp-atd` image used by the app's instrumented tests was not tried for macrobenchmarks. The benchmark and generator builds (`benchmarkRelease`, `nonMinifiedRelease`) add the `x86_64` ABI so they run on the emulator; shipped releases stay arm64-v8a and armeabi-v7a.

### Release manifest

Profile installation uses the `ProfileInstallReceiver` from `profileinstaller`. The app manifest overrides its `exported` attribute with a placeholder: `false` everywhere, `true` only in the benchmark build types so the benchmark can install the profile over adb. The release manifest gains that receiver (not exported, guarded by `android.permission.DUMP`) and nothing else. It has no `INTERNET` permission, and `verifyReleaseManifest` passes (the only exported component is `MainActivity`).

## Cold start

`StartupBenchmark` (Macrobenchmark, `StartupMode.COLD`, 20 iterations, `timeToInitialDisplayMs`) compares `CompilationMode.None()` with `CompilationMode.Partial(BaselineProfileMode.Require)`.

```
./gradlew :baselineprofile:pixel6Api36BenchmarkReleaseAndroidTest \
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR \
  -Pandroid.enableAdditionalTestOutput=true
```

Result on 2026-11-09, Android emulator ("Android SDK built for x86_64", API 36, software GPU, KVM, 8-core Linux host):

| Mode | Median | p90 | Min | Max |
| --- | --- | --- | --- | --- |
| No compilation | 918 ms | 1004 ms | 833 ms | 1030 ms |
| Baseline Profile | 1011 ms | 1141 ms | 831 ms | 1190 ms |

On this emulator the profile gives no measurable improvement; the two distributions overlap, and the profile run was slower at the median. The emulator uses software rendering and a different CPU architecture, so it says little about a phone. Do not read this as proof that the profile helps or hurts.

## OCR latency

`LiveRecognitionLatencyTest` (37 fixture frames, 3 timed runs each, detection side 960 px), run on the `aosp-atd` x86_64 emulator on 2026-11-09 after the profile was added: median 468 ms, p95 1045 ms, max 1126 ms. This is within a few per cent of the spike's 481 ms median at the same detection side ([spike](spikes/25-paddleocr-ncnn/results.md)). The profile covers Kotlin and Compose code, not the native OCR engine, so no change is expected.

## Not verified

- N1 (cold start under 1 s on the reference phone) has not been measured on a real device. The emulator median of about 0.9 to 1.0 s is not evidence either way.
- N2 (OCR under 500 ms on the reference phone) has not been measured on a real device. The emulator p95 is above 500 ms.
- The hand checks on the reference device for both are outstanding (#54).
- The CI benchmark job (`startup-benchmark` in `device-tests.yml`) has not run on GitHub-hosted runners yet; KVM availability and run time there are unconfirmed. zizmor was not run locally.
