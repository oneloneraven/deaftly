# ncnn and PP-OCRv5 models (spike, issue #25)

Not vendored in git: `./gradlew` downloads the ncnn Android release on demand (`fetchNcnn` in `app/build.gradle.kts`), checks its SHA-256 and extracts the `arm64-v8a` and `x86_64` slices into `third_party/ncnn/dist/` (git-ignored).

| Artefact | Source | Licence | SHA-256 |
|---|---|---|---|
| ncnn 20261020, Android static CPU build (no Vulkan) | https://github.com/Tencent/ncnn/releases/download/20261020/ncnn-20261020-android.zip | BSD-3-Clause (Tencent) | `62a9edb493ceb42a5c9cbf2d3a79b3f44bff06c625134ffb64d183dcc42c25fc` |

The models live in `app/src/main/assets/ocr/`.

| File | Origin | Licence | SHA-256 |
|---|---|---|---|
| `ppocrv5_mobile_det.param` | ncnn conversion of PP-OCRv5_mobile_det by the ncnn maintainer, https://github.com/nihui/ncnn-android-ppocrv5 (`app/src/main/assets/`) | Apache-2.0 (PaddleOCR weights) | `358f459680ae0e7a73e477469e529ce116f68c629019ec7a0b6457d2d9117934` |
| `ppocrv5_mobile_det.bin` | as above | Apache-2.0 | `857a96bc963725105b78a178dfcc3c0c3db1a7b9eef32244367b2cb105ccf60b` |
| `ppocrv5_multi_rec.param` | ncnn conversion of PP-OCRv5_mobile_rec (multilingual, 18,385 classes), same repository | Apache-2.0 | `f52a6586ac3338d8c350db0c9f3c55ff2ecd3763327f9bc7f0efc091f8a63e74` |
| `ppocrv5_multi_rec.bin` | as above | Apache-2.0 | `49d9907a55ba20fa6637f9f788f66ab00793bc8ce57a733a09dbe86b9a2e3db0` |
| `ppocrv5_multi_dict.txt` | `PostProcess.character_dict` from https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_rec_onnx `inference.yml` (SHA-256 `5dfeb2777f6d0db8177d8128a8acfcf6e6276dc4ac73ea3bf0dc06d6a5e85d8e`), plus a trailing space | Apache-2.0 | `e5f8ca61ba03d3a247d06b013119982fa6de2bd48a846018b67bca57ffc56de1` |
| `ppocrv5_latin_rec.param` | converted here with pnnx 20261020 from https://huggingface.co/PaddlePaddle/latin_PP-OCRv5_mobile_rec_onnx `inference.onnx` (SHA-256 `7888113072263cb471b93f66dd5e2ad70548dc526fa1ace760d0d973dd121498`) | Apache-2.0 | `f2e815efc778054ea6d495350906bd636d95ff9fd8ed82cd3a75f601f862f3ab` |
| `ppocrv5_latin_rec.bin` | as above | Apache-2.0 | `0ffebbebf4cec1b54b0b0fa08a64c85683be0a8bae5a24920382155bf88f92ed` |
| `ppocrv5_latin_dict.txt` | `PostProcess.character_dict` from the latin `inference.yml` (SHA-256 `0bbe984570f597af3638e50bdf2e8276f3ab26a61966096538b3b0d1849f5c84`), plus a trailing space | Apache-2.0 | `b95923300a0656f8169feee90143cbfcdb62d82a37b54e6b12c224c3e584916f` |

Conversion of the Latin model (build-time only, nothing from it ships):

```
pnnx latin_rec.onnx inputshape=[1,3,48,320] inputshape2=[1,3,48,640] device=cpu
```

pnnx 20261020 from https://github.com/pnnx/pnnx/releases/download/20261020/pnnx-20261020-linux.zip (SHA-256 `6cfff631178e02e62542e72bf14e0045dfce62095bbc1f633176e9c0778ab8a6`). The pnnx binary statically embeds ONNX Runtime for ONNX import, so the conversion step executes Microsoft code on the developer machine; the converted model and the app contain none of it.
