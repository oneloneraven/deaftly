# ncnn and the PP-OCRv5 models

Decision: [ADR-0010](../../docs/adr/0010-recognise-text-with-paddleocr-on-ncnn.md).

## ncnn runtime

Not vendored in git. `./gradlew` runs `fetchNcnn` (`buildSrc/.../nativelibs/FetchNcnnTask.kt`, registered in `app/build.gradle.kts`) before any CMake step. It downloads the release below, refuses it unless the SHA-256 matches, and extracts the `arm64-v8a`, `armeabi-v7a` and `x86_64` slices into `third_party/ncnn/dist/` (git-ignored). The task is up to date until the URL, checksum or ABI list changes, so CI caches that directory.

| Artefact | Source | Licence | SHA-256 |
|---|---|---|---|
| ncnn 20261020, Android static CPU build (no Vulkan) | https://github.com/Tencent/ncnn/releases/download/20261020/ncnn-20261020-android.zip | BSD-3-Clause (Tencent) | `62a9edb493ceb42a5c9cbf2d3a79b3f44bff06c625134ffb64d183dcc42c25fc` |

Release APKs ship `arm64-v8a` and `armeabi-v7a`. `x86_64` is built into debug builds only, for the emulator and the managed-device tests.

## Models

The models live in `app/src/main/assets/ocr/`. They are PaddlePaddle's own PP-OCRv5 mobile detection model and Latin recognition model (Baidu, Apache-2.0), converted to ncnn format from the ONNX files PaddlePaddle publishes on Hugging Face. Nothing else is used: no third-party pre-converted weights, and the multilingual recogniser is not shipped.

| Input (PaddlePaddle, Hugging Face) | SHA-256 |
|---|---|
| `PaddlePaddle/PP-OCRv5_mobile_det_onnx` `inference.onnx` | `a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d` |
| `PaddlePaddle/latin_PP-OCRv5_mobile_rec_onnx` `inference.onnx` | `7888113072263cb471b93f66dd5e2ad70548dc526fa1ace760d0d973dd121498` |
| `PaddlePaddle/latin_PP-OCRv5_mobile_rec_onnx` `inference.yml` | `0bbe984570f597af3638e50bdf2e8276f3ab26a61966096538b3b0d1849f5c84` |

| Asset | Made by | SHA-256 |
|---|---|---|
| `ppocrv5_mobile_det.param` | pnnx from the detection ONNX | `e8edfc052be45735978adb8b7da9a03b1bac01bb8a6653a36993a402f6b38922` |
| `ppocrv5_mobile_det.bin` | as above (fp16 weights) | `6ac1844ccec3a93922bf7aa5126e940cfa9ea11e53e38b2ad169166e3ebf50bc` |
| `ppocrv5_latin_rec.param` | pnnx from the Latin recognition ONNX | `f2e815efc778054ea6d495350906bd636d95ff9fd8ed82cd3a75f601f862f3ab` |
| `ppocrv5_latin_rec.bin` | as above (fp16 weights) | `0ffebbebf4cec1b54b0b0fa08a64c85683be0a8bae5a24920382155bf88f92ed` |
| `ppocrv5_latin_dict.txt` | `PostProcess.character_dict` from the Latin `inference.yml`, one entry per line, plus a trailing space entry (CTC class 0 is the blank, so dictionary line N is class N+1) | `b95923300a0656f8169feee90143cbfcdb62d82a37b54e6b12c224c3e584916f` |

### Reproducing the conversion

```
tools/ocr-models/convert.sh /path/to/pnnx app/src/main/assets/ocr
```

The script downloads the three inputs, checks them against the hashes above, runs pnnx and regenerates the five assets. The pnnx invocations are:

```
pnnx inference.onnx inputshape=[1,3,640,640] inputshape2=[1,3,960,800] device=cpu   # detection
pnnx inference.onnx inputshape=[1,3,48,320]   inputshape2=[1,3,48,640]  device=cpu   # Latin recognition
```

The two input shapes mark the spatial dimensions as dynamic (detection: height and width; recognition: width), which the engine relies on. Converter used: pnnx 20261020, https://github.com/pnnx/pnnx/releases/download/20261020/pnnx-20261020-linux.zip, SHA-256 `6cfff631178e02e62542e72bf14e0045dfce62095bbc1f633176e9c0778ab8a6` (the extracted `pnnx` binary is `4db234cb90a27dc15e04e0d0edf5706c7a488ab8e6235cf8c146c6ec80fd9355`). Conversion is deterministic: the Latin recogniser re-converted for this change is byte-identical to the one converted during the spike.

### ONNX Runtime and the conversion step

pnnx statically embeds ONNX Runtime (Microsoft, on the BDS lists) as its ONNX importer, so running the conversion executes Microsoft code on the developer's machine. It runs under the free-tool exception in [ADR-0002](../../docs/adr/0002-screen-dependencies-against-bds-boycott-lists.md): it is a build-time tool, used once per model update, with no alternative that converts these models. Nothing from ONNX Runtime is in the app, in the converted model files, or in the build that CI runs; the converted files are committed.

Alternatives tried first, to avoid ONNX Runtime:

- ncnn's own `onnx2ncnn` (legacy, in the ncnn source tree but not wired into its CMake build). Built from the ncnn 20261020 source tarball (SHA-256 `968d8d8e65ccadc803322f8238736176e0f9c929fcf484895f58a63e6b227127`) with protobuf 3.21.12 (`protobuf-cpp-3.21.12.tar.gz`, SHA-256 `4eab9b524aa5913c6fffb20b2a8abf5ef7f95a80bc0701f3a6dbb4c607f73460`), adding `add_subdirectory(onnx)` to `tools/CMakeLists.txt`; no host C++ compiler was available, so both were compiled with `zig c++` (zig 0.17.1 from PyPI `ziglang`).
- Detection: `onnx2ncnn` rejects the model's 192 `Identity` nodes. With them removed (a few lines of Python using the `onnx` package, which does not include a runtime) it converts, and `ncnnoptimize` fuses it to 307 layers. Run side by side with the pnnx output on three real fixtures (960 px longest side), the probability maps differed by a mean of 0.0003 but by up to 0.4 at about 0.2% of pixels (1,340 of 675,840 on `menu-brunch`), mostly text edges. It is a different, unvalidated lowering, so it was not adopted.
- Latin recognition: `onnx2ncnn` fails on the model (245 `Identity`, 7 `Shape`, unsupported squeeze and unsqueeze axes, unsupported 5-D transpose and unknown data types). The SVTR attention blocks need shape folding that only pnnx (through ONNX Runtime) provides.

Since recognition cannot avoid ONNX Runtime at conversion time and the pnnx detection output is the validated one, both models are converted with pnnx.

### Licence notes

PaddleOCR models: Apache-2.0, Baidu. The JNI layer and the DB post-processing are written for this project against ncnn only; no code is taken from the ncnn-android-ppocrv5 sample repository or from PaddleOCR.

### Accuracy after conversion

Benchmark on the 37 generated fixtures (x86_64 managed device, API 36, 2 threads), models as committed: word-bag CER 9.8% at detection side 960 and 5.8% at 1280; line recall 81.8% and 92.2%; median latency 476 and 621 ms. The spike, which used the ncnn maintainer's pre-converted detection model, measured 9.7% and 5.8%. `PaddleOcrBenchmarkTest` fails if either configuration drifts beyond those figures.
