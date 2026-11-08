## ML Kit Text Recognition v2 (bundled Latin): spike results

Branch `spike/24-mlkit`, created from `spike/25-paddleocr-ncnn` to reuse its benchmark harness. Not for merge as-is. Same 37 generated fixtures (12 menus, 10 signs, 8 receipts, 7 labels; 742 ground-truth lines, 1,982 words), same harness, same managed device (`pixel6Api36`, x86_64 ATD emulator, API 36, KVM, 2 vCPUs, same host: i7-1185G7), 1 warm-up plus 3 timed runs per image (111 timed frames per configuration).

The PaddleOCR rows below were re-run in the same session on the same emulator with the same harness and reproduce the #25 figures (word-bag CER 5.8% and 9.7%), so the two engines are directly comparable.

### What was built

- Dependency: `com.google.mlkit:text-recognition:16.0.1` (latest stable; Maven metadata lists 16.0.1 as `release`, 7 Aug 2024). This is the bundled Latin model, not `play-services-mlkit-text-recognition`. The recogniser reports `Selected local version of com.google.mlkit.dynamite.text.latin` at start-up, i.e. no Play services module is used.
- Screening: maintainer Google (listed on the BDS lists; ADR-0004 already records ML Kit as a screening hit that uploads diagnostics by default). Recorded for the decision, not cleared by this spike.
- `MlKitTextRecogniser : TextRecogniser` (`app/src/main/kotlin/.../ocr/mlkit/`): maps `Text` blocks, lines and elements (words) to `TextBlock`, `TextLine` (with `cornerPoints` as `corners`, angle from the top edge, confidence) and `TextWord` (axis-aligned bounds of the element's corner points, confidence). Accepts `RGBA_8888` (copied into a `Bitmap`, `InputImage.fromBitmap`) and `NV21` (`InputImage.fromByteBuffer`). Rotation is passed to `InputImage`; only rotation 0 was exercised.
- Manifest: ML Kit merges `INTERNET` and `ACCESS_NETWORK_STATE`. Both are removed with `tools:node="remove"` in the app manifest. The merged release manifest then has only the app's own `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, and `verifyReleaseManifest` passes. ML Kit merges no exported components (its service and providers are `exported="false"`), so nothing else needed removing. It does merge a Firebase Transport scheduler service, a job service and an alarm receiver (all not exported), `GoogleApiActivity` (not exported) and the `com.google.android.gms.version` meta-data.
- Harness changes (small, backwards compatible): `OcrScorer.score` takes an `OrderMode` (`XY_CUT` as before, or `NATIVE` to use the engine's own block and line order) and records, per image, whether the order of matched lines equals ground-truth order under each ordering; `Candidate` gained `frameOf` (so a candidate can be fed NV21 frames) and `order`; `Fixture` gained an `nv21Frame` (BT.601 conversion of the decoded bitmap, even dimensions, chroma subsampled 2x2, conversion done outside the timed region).
- Tests: `MlKitBenchmarkTest` (RGBA native order, RGBA XY-cut order, NV21 native order) and `MlKitNoNetworkTest`.

### Results (emulator, 37 images)

| Configuration | CER, word bag (case-sens. / insens.) | WER, word bag (case-sens. / insens.) | CER / WER, strict GT order (insens.) | Line recall / precision (IoU >= 0.5) | Word box recall (with matching text) | Latency median / p95 | Accented words exact |
|---|---|---|---|---|---|---|---|
| **ML Kit bundled Latin, RGBA via Bitmap, ML Kit's own order** | 5.5% / 5.5% | 13.6% / 13.6% | 31.3% / 39.6% | 96.2% / 96.6% | 94.6% (87.7%) | 2754 / 4162 ms | 18 of 22 |
| ML Kit, RGBA, order re-derived by the harness's XY-cut | 5.5% / 5.5% | 13.6% / 13.6% | 13.6% / 22.4% | 96.2% / 96.6% | 94.6% (87.7%) | 2749 / 4122 ms | 18 of 22 |
| ML Kit, NV21 via `fromByteBuffer`, ML Kit's own order | 6.3% / 6.3% | 15.5% / 15.4% | 31.9% / 41.1% | 95.7% / 96.1% | 93.3% (85.2%) | 2773 / 4244 ms | 18 of 22 |
| PaddleOCR PP-OCRv5 Latin rec, det 1280 (re-run) | 5.8% / 5.8% | 10.2% / 10.1% | 26.5% / 32.1% | 92.3% / 89.3% | 86.3% (84.4%) | 626 / 1159 ms | 20 of 22 |
| PaddleOCR PP-OCRv5 Latin rec, det 960 (re-run) | 9.7% / 9.6% | 14.8% / 14.6% | 29.6% / 37.1% | 82.1% / 80.9% | 80.8% (78.2%) | 486 / 976 ms | 19 of 22 |

Notes on the columns:

- Strict-order metrics use whichever order the row says. PaddleOCR rows use the harness's XY-cut (PP-OCR returns no blocks). The ML Kit XY-cut row is the like-for-like comparison with the PaddleOCR rows, because the same ordering code is applied to both; it is much better than ML Kit's native order because ML Kit's lines are cleaner (fewer merged rows), so the same heuristic works better on them.
- Accented words exact: a ground-truth word containing a non-ASCII letter counts when an identical token (case-sensitive, surrounding punctuation stripped) appears in the image's predicted text. The same script scored all rows, and reproduces #25's 20, 19 of 22 for PaddleOCR. ML Kit's misses: `fraîche` (`menu-brunch`), `Pâtisserie`, `Éclair`, `crème` (`menu-patisserie`).
- Latency is wall time of `recognise` on the emulator. For NV21, the RGBA-to-NV21 conversion is outside the timed region (a camera delivers NV21 for free), so the two input paths are timed equally.

**Input path.** There is no measurable latency difference between RGBA via Bitmap (2754 ms median) and NV21 (2773 ms). The Bitmap path costs one allocation and a 3.7 MB copy that disappears in the noise. NV21 was less accurate here (word-bag CER 6.3% vs 5.5%, word box recall 93.3% vs 94.6%), but that is partly an artefact of my RGB-to-YUV round trip (chroma subsampling, coloured text on coloured backgrounds, BT.601 limited range), so it should not be read as a property of ML Kit. Recommendation: whichever the camera pipeline produces. CameraX can deliver either; there is no cost reason to prefer one.

### By category (ML Kit RGBA / Paddle det 1280, word bag case-insensitive)

| Category | Images | CER | WER | Line recall | Median ms |
|---|---|---|---|---|---|
| receipt | 8 | 2.9% / 0.3% | 6.0% / 0.8% | 99.4% / 97.5% | 2754 / 623 |
| sign | 10 | 7.8% / 5.2% | 26.3% / 18.6% | 90.6% / 100% | 2032 / 365 |
| menu | 12 | 4.0% / 5.6% | 14.6% / 12.8% | 95.0% / 89.4% | 2931 / 703 |
| label | 7 | 8.8% / 9.6% | 13.1% / 9.7% | 98.0% / 91.3% | 3024 / 736 |

By tag (ML Kit RGBA; mean over images, case-insensitive bag): chalkboard CER 7.6% / WER 19.5%, line recall 100% (3); handwriting 8.6% / 23.2%, 97.8% (5); small-print 1.7% / 6.5%, 99.5% (7); rotated 4.2% / 14.2%, 98.2% (6); perspective 2.0% / 10.7%, 97.7% (3); low-light 3.5% / 16.5%, 100% (4); accents 2.4% / 9.4%, 94.1% (6). The tiny blurred print fixture (`label-shampoo`) is CER 51.7%, WER 61.0% (PaddleOCR 64%).

Where the ML Kit errors are:

- Characters that look like digits or letters. `100%` read as `l00%`, `£11.50` as `£ll, SO`, `£10.95` as `£I0,95`, `£11.25` as `£ll.2S`, `5pm` as `Spm`, `ROYAL OAK` as `ROYAL 0AK` (`menu-vegan-board` CER 15.8%, `receipt-pharmacy` 10.3%). This matters for a menu app: prices are where it hurts. PaddleOCR's equivalent figure was not measured per character class, so this is an observation, not a ranking.
- The en dash (U+2013) comes out as a hyphen, as with the Latin PaddleOCR model.
- Bullets and middle dots are dropped, not glued to neighbouring words as PaddleOCR does (`Paint•Tools•`). That is better for word segmentation (sign WER would otherwise be lower still), but the symbol is lost.
- Prices are returned as separate blocks after the dish list on two-column menus, so ML Kit's own order is not reading order (below).

### Reading order

ML Kit returns blocks, each with lines. Using ML Kit's own block order and, within a block, line order, the sequence of matched lines equals ground-truth order on only 11 of 37 images (30%). The harness's XY-cut applied to ML Kit's lines gets 21 of 37; applied to PaddleOCR's lines it gets 19 (det 1280) and 21 (det 960) under the same definition. (#25 quoted 20 of 37 for the XY-cut at 1280 using a different definition, so use the figures in this document for comparison.) Dishes and prices are in different blocks on most menus, which is where the native order loses. Consequence: a consumer should treat the ML Kit order as a block grouping, not a reading order, and reorder by geometry as it would for PaddleOCR.

### Word boxes (requirement O3)

I looked at the overlays for `menu-patisserie`, `menu-kids`, `receipt-cafe`, `sign-a-board` and `label-shampoo` (orange: word boxes, green: line quads; files in this folder, `overlay-*.jpg`).

- ML Kit gives native word boxes and per-line quadrilaterals for rotated text. Line quads are tight and follow the baseline slope (`menu-kids` at about 13 degrees, `receipt-cafe`, `sign-a-board`). Word boxes sit on the right words, with edges close to the glyphs on clean horizontal text, which is the fine-grained accuracy PaddleOCR's CTC-derived boxes lack. Word box recall at IoU >= 0.5 is 94.6% against 86.3% (PaddleOCR det 1280), with matching text 87.7% against 84.4%.
- On rotated lines the axis-aligned word box is much bigger than the word and neighbours overlap (`menu-kids`, `receipt-cafe`, `sign-a-board`), exactly as for PaddleOCR: drag-to-select on rotated text would need word quads in `TextWord`. ML Kit does provide per-element corner points, so the data exists; the current `TextWord` interface discards it.
- `menu-patisserie`: dotted leaders are sometimes absorbed into a word or line (`beurre . .`, `chocolat . . . .` extend to the price column, and one empty box of leader dots). The 27-line fixture scores 85.2% line recall; the missed lines are those merged lines.
- `menu-kids`: the failing case for PaddleOCR (line recall 36% at det 1280) is found at 92% by ML Kit (23 of 25), with a stray leader-dot box after `scoops)`.
- `sign-a-board` (chalkboard): every line found (10 of 10), word boxes correct, but the order is price column last (native order not preserved).
- `label-shampoo`: large text fine; the ingredients paragraph in tiny type is fragmented into many small boxes and misread (8 of 10 lines matched at 80%, but word text is mostly wrong). Neither engine can read it at this scale.

### Network behaviour without `INTERNET` (the key decision input)

Setup: debug build of the ML Kit configuration, merged manifest with both network permissions removed (asserted by `MlKitNoNetworkTest.mergedManifestHasNoNetworkPermissions`: `requestedPermissions` contains only the app's own signature permission, and `checkSelfPermission(INTERNET)` is `DENIED`). `recognisingAndWaitingSendsNothingAndDoesNotFail` recognises 3 fixtures, waits 15 s, recognises the same 3 again, waits 15 s, and reads `TrafficStats` for the app uid around it. Per-test logcat from all five test runs (about 3,400 lines each for the benchmarks, 415 for the network test) was searched for `mlkit`, `firebase`, `clearcut`, `datatransport`, `transport`, `UnknownHost`, `SecurityException`, `INTERNET`, `network`, `socket`, `upload`, `cct`.

What ML Kit tried:

- At first use, and again each time ML Kit logs a recognition, Firebase's Transport runtime (the `datatransport` library that `text-recognition` pulls in, with its CCT backend) tries to schedule an upload job. It fails before any network access: `W TransportRuntime: Error scheduling event ConnectivityService: Neither user 10100 nor current process has android.permission.ACCESS_NETWORK_STATE.` This is a `SecurityException` raised by `ConnectivityManager` when the scheduler checks for connectivity, caught and logged at WARN. 137 identical lines in the first run's five per-test logs (about 40 to 45 per benchmark run, 8 in the network test), and 8 again in the network test's re-run.
- The events are not discarded. After 12 recognitions (read in a re-run of the network test, which added the file listing) the app's private storage held `databases/com.google.android.datatransport.events` (40,960 bytes) plus an empty journal, and `shared_prefs/com.google.mlkit.internal.xml` (154 bytes). So ML Kit's usage telemetry is queued in a local SQLite database that can never be flushed while `INTERNET` is absent.
- Nothing else: no `UnknownHostException`, no `SecurityException` mentioning `INTERNET`, no connection or socket attempt, no model download attempt (models are local: `mlkit-google-ocr-models/...` loaded from the APK assets by `tflite_model_pooled_runner`), no Play services dynamite remote lookup beyond `Considering local module ... remote module ...:0` then `Selected local version`.

Whether anything failed, crashed or degraded: no. All three benchmark runs and both network tests passed (5 of 5) and recognition returned results throughout (I did not run a with-permissions control, so any quality difference against an unrestricted ML Kit is not measured), no crash, no ANR, no `E`-level line from the app process other than an unrelated `ashmem` deprecation notice. The only visible effect is the WARN spam, about one line every few seconds while recognising.

Caveats on this evidence:

- `TrafficStats` for the uid reads `(0, 0)` both before and after. On this emulator the counters may simply not account for the app, so that assertion is weak evidence by itself (the PaddleOCR `NoNetworkTest` has the same limitation). The strong evidence is that the process holds no `INTERNET` permission, so the kernel refuses socket creation, plus the absence of any network exception in the logs.
- I did not inspect the contents of the queued events, so what ML Kit records (as opposed to that it records) is not verified. Its published data-use disclosure should be read before any claim is made in the privacy documents.
- I did not verify how the queue is bounded or aged over days. From memory TransportRuntime caps its store (about 10 MB) and prunes old events, but I have not checked this version, so treat unbounded local growth as unconfirmed rather than ruled out.
- Backup: `android:allowBackup` is false, but ADR-0008 allows backup of templates and settings through `data_extraction_rules`. If a future change widens those rules, the datatransport database and ML Kit shared preferences would be backed up and, on restore, uploaded the first time a build with `INTERNET` ran. Worth a check against `data_extraction_rules.xml` if ML Kit is chosen.
- Run on the emulator only; no real device, no Play services behaviour to compare (the AOSP ATD image has none).

Net: the ADR-0004 configuration (permissions removed with `tools:node="remove"`, guard green) works. ML Kit degrades gracefully, with a steady stream of caught `SecurityException` warnings and a local, never-sent telemetry queue. The remaining risk is the guard being the only thing between that queue and the network.

### APK size (release, unsigned, `-PwithoutOcrEngine`, R8 and resource shrinking on)

| APK | Size |
|---|---|
| Without any engine (#25 baseline) | 1,286,423 B (1.2 MiB) |
| With ML Kit, universal (arm64-v8a, armeabi-v7a, x86_64, x86 libraries all included) | 44,187,774 B (42.1 MiB), +42.9 MB |
| arm64-v8a only (computed from APK entries, no ABI split configured) | about 14.1 MB, +12.8 MB |
| armeabi-v7a only | about 9.8 MB, +8.5 MB |
| x86_64 only | about 14.6 MB, +13.4 MB |

The cost is `libmlkit_google_ocr_pipeline.so` (11.1 MB arm64-v8a, 6.8 MB armeabi-v7a, 11.6 MB x86_64 and 11.6 MB x86 as stored, uncompressed), 1.3 MB of model assets and roughly 1.0 MB of dex. Method matches #25 (same baseline, same entries-based per-ABI estimate). For comparison #25 estimated arm64-only +10.5 MB for PaddleOCR with the Latin model (+13.6 MB multilingual); universal +37.2 MB. ML Kit ships four ABIs by default, so an APK that restricts `abiFilters` to arm64-v8a (plus x86_64 for the emulator in debug) matters as much for ML Kit as for PaddleOCR. A 32-bit ARM phone would need the armeabi-v7a library as well.

### Environment and what was not done

- Emulator only: the x86_64 ATD emulator with 2 vCPUs. **No physical mid-range device was available (`adb devices` lists none), so the issue's "one mid-range device" criterion is not met.**
- ML Kit latency on this emulator is 2.75 s median, 4.4 times PaddleOCR det 1280 (626 ms) and 5.7 times det 960. I did not investigate why (an x86 build running under an emulator without the optimised paths its arm64 build has is plausible, but that is a guess). Do not read this as a phone number, and do not use it to rank the engines on latency: it may invert or shrink on arm64. It is the same figure with the same harness on the same machine, which is what was asked for. Per-frame spread is 1.9 to 5.3 s.
- Memory: peak resident set of the test process was 676 MB (RGBA) and 544 MB (NV21) with ML Kit, against 1.18 GB with PaddleOCR in the same harness. These are process-wide upper bounds (37 decoded fixtures included), not engine figures.
- ML Kit exposes no stage timings, so there is no detect/recognise split.
- Frame dropping and back-pressure for a live camera feed were not tested; the recogniser awaits one call at a time.
- Fixtures are synthetic; real photographs (glare, motion blur, curved packaging) are untested, as in #25.

### Comparison

Accuracy. Equal on characters (word-bag CER 5.5% against 5.8%), slightly worse on words (13.6% against 10.2% WER), and clearly better on finding things: line recall 96.2% against 92.3%, line precision 96.6% against 89.3%, word box recall 94.6% against 86.3%, and it handles the rotated two-column menus (`menu-kids`) that defeat PaddleOCR at lower detection sizes without any tuning. It also returns the structure the app wants (blocks, lines, words, per-line quads, confidence) natively, where PaddleOCR needed a custom CTC word-box derivation and heuristic ordering. PaddleOCR is better at receipts (0.3% against 2.9% CER), accented characters (20 of 22 against 18 of 22) and digits and prices (not measured per class, but ML Kit's `l`/`1`, `S`/`5`, `0`/`O` confusions are visible in the menus and signs).

Latency and size. Not decided by this spike. On the emulator ML Kit is about 4 times slower; the arm64 APK is about 2 MB bigger than PaddleOCR's (Latin): +12.8 MB against +10.5 MB. Neither number is from a phone.

Network and privacy. PaddleOCR has no network code at all. ML Kit runs correctly with `INTERNET` and `ACCESS_NETWORK_STATE` removed, does not crash and does not download models, but it carries Firebase Transport and Play-services-base libraries that try to schedule telemetry uploads on every recognition and queue the events on disk. That is acceptable only as long as the ADR-0004 guard stays in force, and it is a permanent source of log warnings, a dependency on Google libraries with BDS screening implications (ADR-0002), and a standing review item. PaddleOCR avoids all of it at the cost of owning the model conversion, the C++ layer and the post-processing.

My read: ML Kit wins on detection and structure, PaddleOCR on recognition quality and on principle (no telemetry code present). Neither is clearly ahead on speed until both are measured on an arm64 phone, and that single measurement, plus a decision on how much ADR-0002 weighs a Google dependency that only works safely behind a build guard, is what should decide #26. If the phone run shows ML Kit's latency is competitive, ML Kit is the lower-effort path and the privacy cost is containable; if it shows PaddleOCR within the 500 ms target at det 960 or 1280, PaddleOCR is the better fit for the app's stated values.

### Artefacts

Raw reports (`.md` and `.json`, including per-image predicted text) for the three ML Kit configurations (`mlkit-*`) and the two PaddleOCR re-runs (`latin-*`) are in `raw/`. `./gradlew check` passes on this branch (86 JVM tests, 0 failures, `verifyReleaseManifest` ok). The logcat excerpt used for the network findings is `raw/network-logcat-excerpt.txt`.
