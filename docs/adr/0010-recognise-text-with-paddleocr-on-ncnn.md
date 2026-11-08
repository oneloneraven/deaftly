# 10. Recognise text with PaddleOCR on ncnn

## Status

Accepted

## Context

Live OCR (requirements O1–O6) needs on-device text recognition with line and
word geometry, fast enough for a live camera (N2: under 500 ms on a mid-range
phone), and no network access ([ADR-0004](0004-the-app-has-no-network-access.md)).

Two candidates were benchmarked on the same 37 generated fixtures, emulator
and harness (#24, #25; results in `docs/spikes/`):

| | Word-bag CER | Word-bag WER | Line recall | Median / p95 latency (emulator) | Accented words exact | APK, arm64 |
|---|---|---|---|---|---|---|
| ML Kit v2, bundled Latin | 5.5% | 13.6% | 96.2% | 2,754 / 4,162 ms | 18 of 22 | about +12.8 MB |
| PP-OCRv5 mobile on ncnn, Latin recogniser, detection at 1280 | 5.8% | 10.2% | 92.3% | 626 / 1,159 ms | 20 of 22 | about +10.5 MB |
| The same, detection at 960 | 9.7% | 14.8% | 82.1% | 486 / 976 ms | 19 of 22 | about +10.5 MB |

ML Kit finds lines better (rotated two-column menus especially) and returns
blocks, words and corners natively. PaddleOCR recognises text better (fewer
word errors, better on receipts, prices and accents), was about four times
faster on the same emulator, is smaller, and contains no telemetry.

ML Kit is Google's, a BDS pressure target, and viable alternatives exist, so
the free-tool exception in [ADR-0002](0002-screen-dependencies-against-bds-boycott-lists.md)
does not clearly apply. It also bundles Firebase Transport, which tries to
upload usage events; without network permissions it fails safely, but queues
events on the device. The project's rule is to choose the ethical option
when it is viable.

## Decision

Use PP-OCRv5 mobile (detection plus the Latin recogniser) on ncnn, through
the `TextRecogniser` interface in `:core`.

- ncnn: Tencent, BSD-3-Clause; not on the BDS lists. PaddleOCR models:
  Baidu, Apache-2.0; not on the BDS lists.
- Models are converted from PaddlePaddle's own published files. ONNX
  Runtime (Microsoft) is not used in the app; where a conversion tool
  embeds it at build time only, that is recorded in
  `third_party/ncnn/README.md`.
- Release APKs ship arm64-v8a and armeabi-v7a; x86_64 is for emulators
  only, in debug builds.

## Consequences

- No Google code and no telemetry in the OCR path.
- Line detection on rotated, densely packed layouts is weaker than ML Kit's;
  the live screen offers freeze-frame, tap and drag selection, and editing
  before insertion (O3–O5), which absorb some misses.
- Performance on a real arm64 phone is not yet measured. The emulator puts
  detection at 1280 above N2 and at 960 near it; the live screen (#48)
  must hit N2 on the reference device (#54), using a lower detection size,
  detection every few frames, or showing lines as they finish, as needed.
- The build needs the Android NDK and CMake, and fetches a checksummed ncnn
  release.
- The fixtures are synthetic; accuracy on real photographs is checked by
  hand on a phone and in user testing (#56).
- If PaddleOCR proves non-viable on real phones, a new ADR revisits the
  choice; ML Kit's spike branch (`spike/24-mlkit`) keeps the measured
  alternative.
