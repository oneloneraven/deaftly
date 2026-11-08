## PaddleOCR PP-OCRv5 mobile on ncnn: spike results

Branch `spike/25-paddleocr-ncnn`. Not for merge as-is. Measured on the 37 generated fixtures in `app/src/androidTest/assets/ocr-fixtures/` (12 menus, 10 signs, 8 receipts, 7 labels; 742 ground-truth lines, 1,982 words).

### What was built

- PP-OCRv5 mobile detection plus recognition on ncnn 20261020 (CPU, no Vulkan), through a small JNI layer (`app/src/main/cpp/`), behind `PaddleOcrNcnnRecogniser : TextRecogniser`. No OpenCV: the DB post-processing (threshold 0.3, connected components, convex hull and minimum-area rectangle, unclip ratio 1.5, box threshold 0.6) and the perspective crop are written in C++ against ncnn only. No angle classifier (upside-down text is not handled). Accepts `RGBA_8888` and all four rotations.
- Two recognition models were benchmarked: the default multilingual PP-OCRv5 mobile (18,385 classes) and the Latin PP-OCRv5 mobile model, converted here from Paddle's published ONNX export with pnnx (about a minute, no problems).
- Benchmark harness: `app/src/androidTest/kotlin/com/ravendarque/deaftly/ocr/bench/` (`FixtureSet`, `Candidate`, `OcrBenchmark`). It takes any `TextRecogniser` factory, so #24 only needs its own `Candidate` and a four-line test class. Metric code (CER, WER, word-bag errors, polygon IoU, reading order, percentiles, JSON and markdown writers) lives in `:core` test fixtures with JVM unit tests (`core/src/testFixtures/.../ocr/bench`). Output goes to `additionalTestOutputDir`, which the managed-device task pulls to `app/build/outputs/managed_device_android_test_additional_output/`.
- Run: `./gradlew pixel6Api36DebugAndroidTest` (x86_64 ATD emulator, API 36, KVM). 1 warm-up plus 3 timed runs per image, 37 images, so 111 timed frames per configuration.

### Environment (read this before the latency numbers)

Host: 11th Gen Intel Core i7-1185G7 @ 3.00 GHz (4 cores, 8 threads), Linux, KVM. The emulator has 2 vCPUs. Latency from an x86_64 emulator is not representative of a phone; it is reported because it was asked for. An arm64 run on a real mid-range phone is still required and has not been done. The arm64-v8a library builds and packages (see APK size) but has never been executed.

### Results (emulator, 2 threads, fp16 enabled, fixtures are 1.2 MP)

| Configuration | CER, word bag (case-sens. / insens.) | WER, word bag (case-sens. / insens.) | CER / WER, strict GT order (insens.) | Line recall / precision (IoU >= 0.5) | Word box recall (with matching text) | Latency median / p95 |
|---|---|---|---|---|---|---|
| Latin rec, detection side 1280 | 5.8% / 5.8% | 10.2% / 10.1% | 26.5% / 32.1% | 92.3% / 89.3% | 86.3% (84.4%) | 629 / 1142 ms |
| Latin rec, detection side 960 | 9.7% / 9.6% | 14.8% / 14.6% | 29.6% / 37.1% | 82.1% / 80.9% | 80.8% (78.2%) | 481 / 983 ms |
| Latin rec, detection side 640 | 20.9% / 20.9% | 24.6% / 24.3% | 35.3% / 42.9% | 57.7% / 57.1% | 65.9% (64.5%) | 372 / 838 ms |
| Multilingual rec, detection side 960 | 12.9% / 12.7% | 18.4% / 17.9% | 27.7% / 40.2% | 82.2% / 81.0% | 79.4% (74.9%) | 573 / 1183 ms |
| Latin rec, detection side 960, 1 thread | 9.7% / 9.6% | 14.8% / 14.6% | 29.6% / 37.1% | 82.1% / 80.9% | 80.8% (78.2%) | 726 / 1598 ms |

Median stage times (ms, 2 threads): detection side 1280: det 257, rec 355; 960: det 153, rec 324 (latin) or 414 (multilingual); 640: det 68, rec 302. Post-processing and RGBA conversion are 2 to 3 ms each. Recognition cost scales with the number of detected lines (about 20 ms per line per thread), so signs (3 to 10 lines) are fast and menus and labels are slow.

How to read the two text-error columns:

- Word bag compares the multiset of words, matching identical words first and then near-identical pairs (edit distance at most half the longer word), so it is independent of line order and of how the detector splits or merges lines. This is the recognition-quality number.
- Strict ground-truth order concatenates predicted lines in a reading order my code derives (rotation-levelled XY-cut) and compares with the fixtures' own order. PP-OCR returns no block structure, so the order is the app's problem. The XY-cut reproduces the ground-truth order exactly on only 20 of 37 fixtures (two-column menus with prices, nutrition tables), so this column mostly measures ordering and segmentation, not recognition. It is included because it is what a naive consumer would see.

### By category (Latin rec, detection side 1280 / 960)

| Category | Images | CER word bag | WER word bag | Line recall | Median ms |
|---|---|---|---|---|---|
| receipt | 8 | 0.3% / 0.1% | 0.8% / 0.3% | 97.5% / 97.5% | 623 / 482 |
| sign | 10 | 5.2% / 6.8% | 18.6% / 18.6% | 100% / 88.7% | 365 / 254 |
| menu | 12 | 5.6% / 12.7% | 12.8% / 21.8% | 89.4% / 75.9% | 703 / 575 |
| label | 7 | 9.6% / 11.4% | 9.7% / 12.0% | 91.3% / 78.5% | 736 / 618 |

By tag at detection side 1280 (mean per-image CER, line recall): chalkboard 2.9%, 91.7% (3 images); low-light 4.2%, 97.9% (4); handwriting 4.7%, 77.4% (5); rotated up to about 13 degrees 5.0%, 83.9% (6); light-on-dark 2.2%, 96.2% (6); small-print 0.6%, 94.8% (7); tiny blurred print 63.9%, 60.0% (1).

### Accuracy failures

- Tiny blurred print (`label-shampoo`, the only tiny-print fixture): the detector fragments the lines or misses them, CER 64%. Detection side 1280 only helps a little (960: 76%). Anything at that scale needs a higher-resolution crop or a closer shot.
- Detection side matters more than anything else. At 640 line recall drops to 58% and CER doubles; at 960, rotated two-column menus with dotted leaders (`menu-kids`, `menu-trattoria`) merge dish, leader and price from neighbouring rows into one slanted box (`menu-kids` line recall 20% at 960, 36% at 1280). Stray `.` fragments from leaders also appear in the text.
- The dictionary choice matters for UK English. The Latin model's dictionary has no en dash (U+2013), so `09:00 – 15:00` comes out with a hyphen; the multilingual dictionary has it. The multilingual dictionary is missing `î` and `ï`, and the multilingual model drops accents far more often. Accented words recognised exactly: 20 of 22 (91%) for Latin at 1280, 19 of 22 at 960, 12 of 22 (55%) for multilingual. The two misses for Latin at 1280 are both `fraîche`. Only 22 accented words exist in the fixtures, so that is a small sample.
- Bullets and middle dots are recognised, but the model drops the spaces around them (`Paint•Tools•`), which is a word error and would also mis-segment drag-to-select words on signs. This is why `sign` WER is 18.6% while CER is 5.2%.
- Small, condensed, shadowed italic descriptions (`menu-brunch`) produce garbled words (`hollandaise oa toasted`, `Geeky,y, (`); CER 9% in that image at 1280.
- Chalkboard, handwriting-style fonts and low-light fixtures are not the problem: they are among the best. Caveat: these are generated images, not photographs, so real chalk, glare and motion blur are untested.

### Word boxes (requirement O3)

The recognition model returns per-timestep class probabilities (`T = crop width / 8`). Greedy CTC decoding keeps, for each emitted character, the first and last timestep of its run. A word is a run of characters between space tokens. Each word's horizontal extent is the centre of its first character minus half the line-wide average character spacing, to the centre of its last character plus half that spacing, clamped to the midpoint between neighbouring words and to the crop. That extent is mapped through the line's perspective crop (quad corners) back to image coordinates, and the word's axis-aligned bounds are reported (the `TextWord` interface carries a `BoundingBox` only).

I drew the boxes on all fixtures and looked at them (overlays for `menu-patisserie` at 960, `receipt-cafe`, `sign-a-board` at 1280 are in this folder). On horizontal and mildly rotated text they sit on the right words, with edges within about half a character of the glyphs. 86.3% of ground-truth word boxes are matched at IoU >= 0.5 (84.4% with identical text) at detection side 1280. On rotated lines the axis-aligned box of a slanted word is much larger than the word, so neighbouring boxes overlap; the ground truth has the same property, which is why it still scores, but drag-to-select on rotated text would want the word quad exposed in `TextWord` (the line already carries `corners`). Short words and single punctuation marks get the least accurate boxes, because a CTC peak is one or two timesteps wide.

### Size, memory, network

| Item | Size |
|---|---|
| Detection model (`ppocrv5_mobile_det`) | 2.36 MB bin + 25 KB param |
| Latin recognition model | 4.00 MB bin + 19 KB param (838 classes) |
| Multilingual recognition model | 8.24 MB bin + 20 KB param (18,385 classes) |
| `libdeaftly_ppocr.so` (ncnn statically linked, stripped) | arm64-v8a 5.2 MB, x86_64 19.9 MB (the prebuilt x86_64 ncnn is 31 MB) |

`assembleRelease`, unsigned, same branch built with and without `-PwithoutOcrEngine`:

| APK | Size |
|---|---|
| Without engine | 1,286,423 B (1.2 MiB) |
| With engine, universal (arm64-v8a, x86_64, both recognition models) | 38,487,288 B (36.7 MiB), +37.2 MB |
| arm64-v8a only, Latin model only (computed from the APK entries, no ABI split is configured) | about +10.5 MB |
| arm64-v8a only, multilingual model only | about +13.6 MB |

Models are compressed by the APK by 9 to 32%; native libraries are stored uncompressed (AGP default). Compiled with NDK r30 (30.0.16248370) and CMake 4.1.2.

Memory: peak resident set of the whole instrumentation process (cumulative `VmHWM`, includes 37 decoded fixture bitmaps and the test harness) was 1.0 GB with one thread and 1.3 GB across the later configurations. That is an upper bound, not an engine figure; a per-engine number needs a device run.

Network: the app still has no `INTERNET` permission. `./gradlew check` passes, including `verifyReleaseManifest` (result `ok`). `NoNetworkTest` on the emulator asserts the permission is absent and denied, that a socket connect attempt fails, and that `TrafficStats` tx and rx bytes for the app's uid are unchanged after recognising three fixtures. The per-test logcat for all benchmark runs (about 58,000 lines) holds no application network activity, only system service chatter.

### Vendored and downloaded artefacts

| Artefact | Source | Licence | SHA-256 |
|---|---|---|---|
| ncnn 20261020 Android static, CPU (arm64-v8a and x86_64 extracted; fetched by Gradle, not committed) | https://github.com/Tencent/ncnn/releases/download/20261020/ncnn-20261020-android.zip | BSD-3-Clause | `62a9edb493ceb42a5c9cbf2d3a79b3f44bff06c625134ffb64d183dcc42c25fc` |
| `ppocrv5_mobile_det.param` / `.bin` | ncnn conversion of PP-OCRv5_mobile_det from https://github.com/nihui/ncnn-android-ppocrv5 (`app/src/main/assets/`) | Apache-2.0 (PaddleOCR weights) | see `third_party/ncnn/README.md` |
| `ppocrv5_multi_rec.param` / `.bin` | same repository, PP-OCRv5_mobile_rec | Apache-2.0 | see `third_party/ncnn/README.md` |
| `ppocrv5_multi_dict.txt` | `inference.yml` of https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_rec_onnx plus a trailing space | Apache-2.0 | see `third_party/ncnn/README.md` |
| `ppocrv5_latin_rec.param` / `.bin` | converted here by pnnx from https://huggingface.co/PaddlePaddle/latin_PP-OCRv5_mobile_rec_onnx `inference.onnx` | Apache-2.0 | see `third_party/ncnn/README.md` |
| `ppocrv5_latin_dict.txt` | `inference.yml` of the same model plus a trailing space | Apache-2.0 | see `third_party/ncnn/README.md` |
| pnnx 20261020 (conversion only) | https://github.com/pnnx/pnnx/releases/download/20261020/pnnx-20261020-linux.zip | BSD-3-Clause | `6cfff631178e02e62542e72bf14e0045dfce62095bbc1f633176e9c0778ab8a6` |

Full hashes, the exact pnnx command and provenance are in `third_party/ncnn/README.md`.

Licence notes: the reference repository (nihui/ncnn-android-ppocrv5) has no LICENSE file; its source files carry BSD-3-Clause headers. None of its code is copied (the JNI layer and post-processing here are written from scratch against ncnn); only its pre-converted detection and multilingual recognition models are used, which carry no licence text of their own and derive from Apache-2.0 PaddleOCR weights. For a shipping build the detection and multilingual models should be reconverted from Paddle's own published files so provenance is first-party.

Conversion caveat: the pnnx binary statically embeds ONNX Runtime for its ONNX importer, so the Latin conversion step executed Microsoft code on the developer machine. Nothing from it is in the app or in the converted model, and the ONNX file is only an interchange format. If that is unacceptable under ADR-0002, the alternative is ncnn's own `onnx2ncnn` built from source, which has no ONNX Runtime; I have not tried it on this model.

### Viability

Accuracy: usable. On receipts it is effectively solved (0.3% word-bag CER). On signs, menus and labels, at detection side 1280 with the Latin recogniser, 94% of characters and about 90% of words are right and 92% of lines are found; chalkboards, handwriting-style fonts, low light and rotation up to 13 degrees all work. The remaining gaps are fixable or bounded: tiny blurred print, leader-dot merging at lower detection resolution, spaces around bullets, and the missing en dash in the Latin dictionary (a post-processing substitution or a patched dictionary would cover that). Accent handling is good with the Latin model and poor with the multilingual one, so ship Latin.

N2 (under 500 ms on a mid-range phone): not demonstrated, and not safe to assume. On the emulator the full pipeline is 372 ms (det 640, accuracy too low to use), 481 ms (det 960) and 629 ms (det 1280) median, with p95 up to 1.1 s, on a laptop-class core with 2 threads. A mid-range phone's big cores are probably slower per core than this host but have more of them and a fp16 NEON path; I cannot say which dominates without a device run, so treat the emulator figure as roughly the right order of magnitude, not a prediction. The structure of the cost is clear: detection scales with frame area and recognition scales with the number of lines, so a menu with 40 lines costs about twice a sign. A design that meets N2 on a phone probably cannot run the whole pipeline on every frame at 1280 px. Realistic options are a lower detection side with a recognition crop at full resolution, running detection every few frames and tracking lines between them, recognising only lines whose crop has changed, and showing results progressively as lines finish. All of these need the engine behind an interface that reports partial results, which `TextRecogniser.recognise` (one call, one result) does not yet allow.

Cost: the engine adds about 10.5 MB to an arm64 APK with the Latin model (13.6 MB with multilingual), compared with a 1.3 MB app today. That is an order of magnitude on the download size, and ADR-0007 distributes APKs from GitHub releases, so it is a user-visible but tolerable cost. A leaner ncnn build (only the layers PP-OCR uses) would cut the 5.2 MB library, untried.

Recommendation: keep PP-OCRv5 on ncnn as a live candidate. The decision hinges on one measurement not yet taken: median and p95 on a real arm64 mid-range phone at detection sides 960 and 1280. Run `./gradlew connectedDebugAndroidTest` with the same harness on a device and compare with the ML Kit candidate on the same frames.

### Things this spike did not do

- No arm64 execution. No Vulkan or GPU path. No angle classifier, so 180-degree text is unhandled. No int8 or other quantised models, and no model tuning.
- The fixtures are synthetic. A handful of real photographs (glare, motion blur, curved packaging) should be added before the decision is final.
- The reading-order reconstruction is a heuristic, not a feature; the interface has no block structure to fill (one `TextBlock` per detected line).
- Word-bag CER is my own metric; its definition is in `core/src/testFixtures/.../TextMetrics.kt` and covered by unit tests.
