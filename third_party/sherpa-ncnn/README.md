# sherpa-ncnn and the streaming speech model

Used for voice typing in side by side chat. The plan is `docs/superpowers/plans/2026-11-18-voice-typing.md` (Task 3). The model was chosen in the spike at `docs/spikes/287-speech-to-text-models/results.md`.

## What this directory is for

- `src/` (git-ignored) holds the recogniser source, fetched by the build. Nothing is vendored in git.
- The model is produced by hand, once, and published as a release asset on this repository. The build fetches it by URL and refuses it unless the SHA-256 matches. This README records exactly how it was made, so it can be regenerated.

## Source tarballs

`./gradlew` runs `fetchSherpaSources` (`fetchSherpaNcnn`, `fetchKaldiNativeFbank`, `fetchKissFft`, registered in `app/build.gradle.kts`) before any CMake step. Each download is refused unless its SHA-256 matches, and only the listed paths are extracted into `third_party/sherpa-ncnn/src/`. The recogniser is compiled into the app's own `deaftly_native` library against the app's ncnn (see `third_party/ncnn/README.md`). sherpa-ncnn's prebuilt Android libraries are not used.

| Artefact | Source | Licence | SHA-256 |
|---|---|---|---|
| sherpa-ncnn v2.1.15 | https://github.com/k2-fsa/sherpa-ncnn/archive/refs/tags/v2.1.15.tar.gz | Apache-2.0 (`LICENSE`) | `f4bb8bdd3cea20049cff97aa90dec1cee048b7fdac9a92464ca64df7efe5f27b` |
| kaldi-native-fbank v1.21.5 | https://github.com/csukuangfj/kaldi-native-fbank/archive/refs/tags/v1.21.5.tar.gz | Apache-2.0 (`LICENSE`) | `499966d4cdeb05060df2b26a0c171f92a6a2a119e7747b69e621e62c7fad0e9c` |
| kissfft, commit `febd4caeed32e33ad8b2e0bb5ea77542c40f18ec` | https://github.com/mborgerding/kissfft/archive/febd4caeed32e33ad8b2e0bb5ea77542c40f18ec.zip | BSD-3-Clause (`COPYING`, SPDX identifier) | `497103e664168ebe39580b757adbe616f6cf85a16572af581ca7bc42d0ab13fd` |

## Model

`icefall-libri-giga-pruned-transducer-stateless7-streaming-2023-04-04`: a streaming Zipformer transducer (70.37 million parameters) trained on LibriSpeech and GigaSpeech with icefall (k2-fsa, icefall PR 984). The ncnn files are regenerated from the published PyTorch checkpoint with icefall's own export script and pnnx, not taken from a third-party conversion. Weights are fp16 as pnnx writes them. There is no int8 version (see the spike results).

### Inputs

| Input | Source | SHA-256 |
|---|---|---|
| Checkpoint `exp/pretrained.pt` (281,766,253 bytes) | https://huggingface.co/marcoyang/icefall-libri-giga-pruned-transducer-stateless7-streaming-2023-04-04, revision `df8a1ee67abb67244b87d8011ec64ba46c6e97c0` | `89321422457b5f14e728c2594ea02989bf5127277c7bfb76baedd0e30683fd4b` (matches the Hugging Face LFS object id) |
| `data/lang_bpe_500/tokens.txt`, same repository and revision | as above | `49e3c2646595fd907228b3c6787069658f67b17377c60aeb8619c4551b2316fb` |
| icefall, `master` at `ed16de9534d79252cac51f1729fdcee5699e9758` (18 November 2026) | https://github.com/k2-fsa/icefall | n/a (git commit) |
| pnnx, built from `4a10ad2adf39da05ad212eb290d851756d46814a` (`tools/pnnx`) | https://github.com/csukuangfj/ncnn, the fork that icefall's ncnn export documentation tells you to build | n/a (git commit) |

- The checkpoint repository belongs to Xiaoyu Yang, the author of icefall PR 984, and is the repository that icefall's `egs/librispeech/ASR/RESULTS.md` links for this model. `exp/pretrained.pt` is the model the repository's own `exp/export.sh` writes (`--epoch 20 --avg 4`).
- k2-fsa does not publish a second checksum, so the only integrity check on the checkpoint is Hugging Face's own LFS hash.
- The pnnx on PyPI (`pip install pnnx`, tried at 20240410 and 20261020) cannot be used. It leaves `pnnx.Expression` and `Tensor.to` layers in the output, which sherpa-ncnn refuses to load ("layer pnnx.Expression not exists or registered"), and 20261020 crashes on the encoder. The fork adds the layers icefall's export needs (`SimpleUpsample`, `TensorAsStrided`, `PoolingModuleNoProj`).

### Tool versions

- Python 3.10.22 (uv venv), torch 1.13.1+cpu, torchaudio 0.13.1+cpu, k2 1.24.4.dev20250425+cpu.torch1.13.1, lhotse 1.33.0, numpy 1.26.4. icefall's documentation says torch above 2.0 may not work with pnnx.
- pnnx built with g++ 15.2.0 (Ubuntu 15.2.0-16ubuntu1) and CMake with Ninja, against the venv's libtorch, with TorchVision off. The result has SHA-256 `171bbe172b7ce6d1babf75a5a64354605629d12a47f02333f98d752ec5893436`.
- sherpa-ncnn 2.1.15 from PyPI, used only to check the output on a desktop.

### Commands

Run from `egs/librispeech/ASR/pruned_transducer_stateless7_streaming` in the icefall checkout with `PYTHONPATH` set to the checkout root. The `_multi` recipe's own `export-for-ncnn.py` does not import (it asks for a data module class that the recipe's symlinked `asr_datamodule.py` does not define), so the sibling `pruned_transducer_stateless7_streaming` script that the sherpa-ncnn documentation uses is run instead. Both recipes link the same model code (`zipformer.py`, `model.py`, `decoder.py`, `joiner.py`, `scaling_converter.py`), and the model's default hyper-parameters are the ones below.

```
# $exp holds pretrained.pt symlinked as epoch-99.pt
./export-for-ncnn.py \
  --tokens data/lang_bpe_500/tokens.txt \
  --exp-dir $exp \
  --use-averaged-model 0 \
  --epoch 99 \
  --avg 1 \
  --decode-chunk-len 32 \
  --num-encoder-layers "2,4,3,2,4" \
  --feedforward-dims "1024,1024,2048,2048,1024" \
  --nhead "8,8,8,8,8" \
  --encoder-dims "384,384,384,384,384" \
  --attention-dims "192,192,192,192,192" \
  --encoder-unmasked-dims "256,256,256,256,256" \
  --zipformer-downsampling-factors "1,2,4,8,2" \
  --cnn-module-kernels "31,31,31,31,31" \
  --decoder-dim 512 \
  --joiner-dim 512

cd $exp
pnnx encoder_jit_trace-pnnx.pt
pnnx decoder_jit_trace-pnnx.pt
pnnx joiner_jit_trace-pnnx.pt
```

`--context-size` was left at its default of 2 and `--num-left-chunks` at 4. pnnx was run with its defaults (`fp16=1`, `optlevel=2`).

Building pnnx:

```
cd ncnn/tools/pnnx && mkdir build && cd build
cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_PREFIX_PATH=<torch cmake prefix path> ..
ninja -j4
```

### The encoder edit

sherpa-ncnn needs a `SherpaMetaData` layer in the encoder, added as the documentation describes (icefall `docs/source/model-export/export-ncnn-zipformer.rst`, "Modify the exported encoder for sherpa-ncnn"). In `encoder_jit_trace-pnnx.ncnn.param`, the layer count on line 2 went from `2108` to `2109` (the blob count `2616` is unchanged), and this line was inserted as line 3, before `Input in0`:

```
SherpaMetaData           sherpa_meta_data1        0 0 0=2 1=32 2=4 3=7 15=1 -23316=5,2,4,3,2,4 -23317=5,384,384,384,384,384 -23318=5,192,192,192,192,192 -23319=5,1,2,4,8,2 -23320=5,31,31,31,31,31
```

The values come from the export flags: `1=32` is `--decode-chunk-len`, `2=4` is `--num-left-chunks`, `3=7` is the Conv2dSubsampling padding, `15=1` is the model version (sherpa-ncnn 2.0 and later need 1 or more), and the five arrays are `--num-encoder-layers`, `--encoder-dims`, `--attention-dims`, `--zipformer-downsampling-factors` and `--cnn-module-kernels`.

### Output files

Stored in the release asset and in the app under `speech/`. The pnnx outputs are renamed from `<part>_jit_trace-pnnx.ncnn.{param,bin}`.

| File | Bytes | SHA-256 |
|---|---|---|
| `encoder.ncnn.param` (with the edit above) | 161,888 | `dcb32897220695689ca2bfca6461e79a9782fb354947ecfb00a13471d13dcffc` |
| `encoder.ncnn.bin` | 139,191,256 | `717fb4852e9516c46d6fdbc1e67988ac945ca1df8cd934196183d6b9475663c3` |
| `decoder.ncnn.param` | 437 | `500ba1924361f2b88006c08dbe6bbd295057742e637efb4cbeb965a414a835eb` |
| `decoder.ncnn.bin` | 520,200 | `5613a9c8f5e77128520a831524f14780e96a36cb5d55411c898e9bcde24a5613` |
| `joiner.ncnn.param` | 488 | `40e97798f71b43048dea5e52bb13b2dfc7179b2df9a2ec8ce604b09c30400d14` |
| `joiner.ncnn.bin` | 1,435,612 | `72c24d3fe29d8febb9049709206dc3b4dd9193ae22503891dd31a668729ccb0b` |
| `tokens.txt` (unchanged from the checkpoint repository) | 5,048 | `49e3c2646595fd907228b3c6787069658f67b17377c60aeb8619c4551b2316fb` |

### Check against the spike

The spike used a third-party ncnn export of the same checkpoint (Hugging Face `IconicAI/sherpa-ncnn-zipformer-libri-giga-70M-2023-04-04`, revision `f3c0c299cb00bfd67c94c01474a170184b5c8237`). The regenerated files were compared with it.

- The decoder and joiner `.param` and `.bin` files are byte-identical. The encoder `.param` is identical once line endings (the third-party files use CRLF) and run-on spacing are normalised: the same 2,109 layers, blob names and `SherpaMetaData` line.
- The encoder `.bin` files are the same size and differ in 9,081 bytes (0.007%), all within the first 1 MB of the file, which looks like numerical noise in precomputed constants. Their effect was not isolated.
- Both exports gave the same transcript for the fixture below and for four other LibriSpeech test-clean utterances (1089-134686-0000, 0005, 0006 and 0011), using sherpa-ncnn 2.1.15 on a desktop with greedy search and two threads.

### Reproducibility

Reproduction was not re-run end to end after the first successful export, so byte-for-byte determinism of the export is not established. The checksums above are those of the files that were checked and are to be published.

## Test fixture

`app/src/androidTest/assets/speech-fixtures/1089-134686-0008.wav` (6.7 s, 16 kHz mono 16-bit, 215,404 bytes) and its transcript `1089-134686-0008.txt`, from LibriSpeech test-clean (speaker 1089, chapter 134686, utterance 0008), converted from FLAC without resampling. LibriSpeech is by Panayotov, Chen, Povey and Khudanpur, distributed by OpenSLR (https://www.openslr.org/12), and licensed CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/). The corpus is built from LibriVox public-domain audiobook recordings. Ground truth: `THE CHAOS IN WHICH HIS ARDOUR EXTINGUISHED ITSELF WAS A COLD INDIFFERENT KNOWLEDGE OF HIMSELF`. The source archive `test-clean.tar.gz` had MD5 `32fa31d27d2e1cad72775fee3f4849a9`.

## Licences

| Component | Licence | Verified from |
|---|---|---|
| sherpa-ncnn | Apache-2.0 | `LICENSE` in the v2.1.15 tarball |
| kaldi-native-fbank | Apache-2.0 | `LICENSE` in the v1.21.5 tarball |
| kissfft (Mark Borgerding) | BSD-3-Clause | `COPYING` in the pinned archive |
| icefall | Apache-2.0 | `LICENSE` at the commit above |
| Model weights | Apache-2.0 | `license: apache-2.0` in the checkpoint repository's README front matter at the revision above (a metadata tag, not a separate licence file) |
| LibriSpeech fixture | CC BY 4.0 | OpenSLR resource 12 |

The model was trained on LibriSpeech and GigaSpeech. Their licence terms for model weights were not reviewed beyond the Apache-2.0 declaration by the model's publisher.

## ADR-0002 screening

Checked on 19 November 2026 against https://bdsmovement.net/Guide-to-BDS-Boycott and https://bdsmovement.net/no-tech-oppression-apartheid-or-genocide by reading the text of both pages for each name below. Neither page names any of them. The check is a text search of those two pages as served on that date. It does not look into corporate ownership beyond what is stated here.

| Dependency | Maintainer | On a list | Note |
|---|---|---|---|
| sherpa-ncnn, icefall, k2, the checkpoint | k2-fsa (the Next-gen Kaldi project) | N | Already cleared in the 287 spike on 17 November 2026. |
| kaldi-native-fbank | csukuangfj (Fangjun Kuang, k2-fsa) | N | Apache-2.0. |
| kissfft | Mark Borgerding (individual open source maintainer) | N | BSD-3-Clause. |
| Checkpoint host: Hugging Face (account `marcoyang`) | Hugging Face, Xiaoyu Yang | N | The checkpoint is downloaded once by hand. The app's build fetches the model from a release asset on this repository, not from Hugging Face. |
| LibriSpeech (OpenSLR) and GigaSpeech (SpeechColab) | OpenSLR, SpeechColab | N | Training data and test audio only. |
| PyTorch 1.13.1 (export only) | PyTorch (created at Meta, now under the Linux Foundation) | Y, indirectly | Meta appears on the No Tech for Apartheid list. PyTorch is a free, industry-standard tool run once on the developer's machine to export the model, with no viable alternative for icefall's export script. It is not in the app or in the model files, so the free-tool exception in ADR-0002 applies. The owner accepted it on 19 November 2026. |
| Ubuntu g++ 15.2.0, CMake, Ninja (pnnx build only) | Canonical, GNU, Kitware, Ninja authors | Not checked | Build-time tools for a one-off conversion. Not looked up. |

The licence and screening columns record what could be read on the date above. Re-check before substantially expanding the relationship, as ADR-0002 says.
