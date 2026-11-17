## On-device speech-to-text: spike results

Branch `spike/285-speech-to-text`. Not for merge as-is. For #59 (speech-to-text for the hearing person in side-by-side chat).

### Question

Can an open-source engine bundled in the app transcribe the hearing person's reply well enough, so that we don't need Android's on-device recogniser? That recogniser is Google's, which is a screening hit under [ADR-0002](../../adr/0002-screen-dependencies-against-bds-boycott-lists.md), the same trade-off as ML Kit in [ADR-0010](../../adr/0010-recognise-text-with-paddleocr-on-ncnn.md).

### Candidates

| Engine | Model | Trained on | Licence | Screening (ADR-0002) |
|---|---|---|---|---|
| sherpa-ncnn 2.1.15 (k2-fsa) | Streaming Zipformer, 20M parameters (2023-02-17) | LibriSpeech | Apache-2.0 | Not on either list. Runs on ncnn (Tencent), which the app already ships. |
| sherpa-ncnn 2.1.15 | Streaming Zipformer, full size (2023-02-13), greedy and beam search | LibriSpeech | Apache-2.0 | As above |
| Vosk 0.3.75 (Alpha Cephei) | `vosk-model-small-en-us-0.15` | Not stated | Apache-2.0 | Not on either list. Needs JNA (Apache-2.0 / LGPL), also not on either list. |
| Vosk 0.3.75 | `vosk-model-en-us-0.22-lgraph` | Not stated | Apache-2.0 | As above |
| Android `createOnDeviceSpeechRecognizer` (reference only) | Speech Services by Google, en-US and en-GB packs | n/a | Proprietary | Google: on both lists (pressure target, No Tech for Apartheid) |

The BDS lists were checked on 17 November 2026, against the two pages ADR-0002 names.

### Test audio

- **Speakers:** 120 clips of read speech from 20 speakers in VCTK (CC-BY 4.0), 6 clips each, sampled with a fixed seed. Accents and speakers:
  - English: 4
  - Scottish: 4
  - Irish: 4
  - Northern Irish: 4
  - Indian: 3
  - Welsh: 1 (the only Welsh speaker found in VCTK)
- **Noise:** each clip is also mixed with café, street and station noise from DEMAND (CC-BY 4.0), at 10 dB and 5 dB signal-to-noise ratio. That gives 840 clips in all. 5 dB is a loud café with the phone held at arm's length.
- **Limits:** this is newsreader-style read speech, not someone at a counter. Absolute error rates in real use will be higher. The comparison between engines is the useful part.
- **Reproducing:** the audio isn't committed. `tools/stt-spike/` rebuilds it (`vctk_index.py`, `vctk_pull.py`, `mix.py`) and holds the desktop harness (`bench.py`) and the analysis (`analyse.py`). The clip list is in `raw/clips-manifest.json`.

### How it was measured

- **Emulator** (the authoritative numbers):
  - Set-up: the `spike-stt` module, Pixel 4a AVD, API 36, x86_64, 4 vCPUs, on an i7-1185G7.
  - Open engines: fed 100 ms chunks as fast as they would take them, with 2 threads.
  - Android's recogniser: fed in real time through `EXTRA_AUDIO_SOURCE`, with a 1 s silence tail. With a file source its final result is always empty, so its last partial result is scored. All 840 clips went through en-US and en-GB.
  - Open engines' coverage: the clean clips and café at 5 dB only.
  - Vosk large's café run was lost when a diagnostic run restarted the test app. Its desktop numbers cover every condition.
- **Desktop** (Python, every condition): sherpa-ncnn and Vosk.
  - The desktop sherpa harness drops the last few frames ("soldi" for "soldier"), so its sherpa error rates are about 1.5 to 2 points worse than the emulator's.
  - Use the desktop numbers for comparing noise types, and the emulator numbers for absolute values.
- **Word error rate** is scored after lower-casing, removing punctuation and expanding contractions.

### Results: accuracy

Word error rate. Lower is better. Emulator results unless the row says desktop.

| Engine | Clean | Café, 5 dB | All six noisy conditions |
|---|---|---|---|
| Android on-device, en-GB | **4.3%** | **9.3%** | **6.1%** |
| Android on-device, en-US | 4.2% | 10.4% | 6.7% |
| sherpa-ncnn full, beam search | 7.1% | 11.5% | n/a (desktop, greedy: 13.8%) |
| sherpa-ncnn full, greedy | 6.9% | 11.9% | n/a (desktop: 13.8%) |
| sherpa-ncnn 20M, greedy | 7.1% | 14.5% | n/a (desktop: 13.5%) |
| Vosk small | 6.8% | 19.2% | n/a (desktop: 11.7%) |
| Vosk large (lgraph) | 5.9% | n/a (desktop: 22.0%) | n/a (desktop: 11.6%) |

By noise type (desktop, so sherpa reads high):

| Engine | Café 10 / 5 dB | Street 10 / 5 dB | Station 10 / 5 dB |
|---|---|---|---|
| sherpa-ncnn 20M | 12.8% / 18.0% | 11.9% / 14.2% | 10.8% / 13.6% |
| sherpa-ncnn full | 12.8% / 16.9% | 13.0% / 14.7% | 11.9% / 13.3% |
| Vosk small | 10.4% / 19.1% | 8.7% / 9.3% | 9.5% / 13.5% |
| Vosk large | 11.0% / 22.0% | 6.9% / 8.7% | 8.2% / 12.6% |

- **Google's recogniser makes roughly half the errors of the best open engine in noise.** Its en-GB pack is slightly better than en-US in noise and the same in clean audio.
- **The open engines are within about 3 points of each other in clean audio, and split by noise type:**
  - Vosk holds up better in steady noise (street, station).
  - sherpa-ncnn holds up much better in café babble: the other voices that make a café hard. Café babble is the most likely setting for this feature.
- **sherpa-ncnn's full model is about 3 points better than 20M in café noise.**
- **Beam search adds little** over greedy search.
- **Per-accent figures** are in `raw/summary.md`. With 6 clips per speaker, and 1 to 4 speakers per accent, they're indicative only. No engine failed badly on any accent. The Welsh speaker is the hardest for every engine, Google included.

### Results: speed, memory and size

| Engine | Real-time factor | First words shown after speech starts, median / p95 | Final text after audio ends, median | App memory, PSS max | Added to the arm64 APK, compressed |
|---|---|---|---|---|---|
| Android on-device | n/a, fed in real time | 1.23 / 1.77 s | about 1 s (waits for silence) | 31 MB in our process; the engine runs in Google's | 0 MB |
| sherpa-ncnn 20M | 0.04 | 1.13 / 1.70 s | 40 ms | 121 MB | about 37 MB model; about 1 MB library if built against our ncnn |
| sherpa-ncnn full | 0.13 | 1.16 / 1.70 s | 106 ms | 223 MB | about 127 MB |
| Vosk small | 0.20 | 1.01 / 1.54 s | 26 ms | 195 MB | about 40 MB model, 10 MB `libvosk.so`, 0.2 MB JNA |
| Vosk large | 0.98 (p95 chunk 669 ms) | 1.34 / 1.84 s | 182 ms | 420 MB | about 127 MB model, plus the Vosk library |

- **The release APK is 15 MB today** (v0.49.2). Every open option at least triples it.
- **Response time:** every engine shows its first words about 1.0 to 1.3 s after speech starts, so none feels slower than another.
- **Vosk large** runs close to real time on the emulator and is too big. It's out.
- **Real phones:** all speed figures come from an x86 emulator. Nothing has been measured on a real arm64 phone. The ncnn models should be faster there relative to Vosk, since ncnn is tuned for ARM, but that isn't verified.

### Network

- **The spike app requests no permissions.** Neither sherpa-ncnn nor Vosk merges any permission into the manifest (checked with `aapt2 dump permissions`).
- **Neither open engine has any network code to observe.**
- **Google's recogniser runs in Google's own process,** which has its own network access. We can't test that it sends nothing.

### Recommendation

1. **Use sherpa-ncnn**, not Vosk:
   - It's markedly better in café babble, the likeliest setting.
   - It runs on the ncnn runtime the app already ships, so the native cost is about 1 MB rather than 10 MB.
   - It's the fastest and uses the least memory of the open options.
2. **Don't ship the 2023 models as they are.** They were trained only on LibriSpeech (audiobooks read by mostly US speakers), which likely explains much of the gap to Google in noise. The full model is also far too big. Before the feature is built, run one short follow-up spike:
   - **int8-quantise the full model** with ncnn's tools, aiming for 20M size with full-model accuracy, then re-measure.
   - **Look for a streaming Zipformer trained on more varied speech** (for example GigaSpeech or a multi-dataset recipe in icefall) and convert it to ncnn. Whether a suitable pretrained checkpoint exists hasn't been checked.
   - **Measure on a real mid-range arm64 phone.**
   - **Add 20 to 30 counter phrases** recorded by the team in real places, since read news speech flatters every engine.
3. **Design the feature to absorb errors:**
   - Words appear as they're spoken.
   - The hearing person can correct them before sending.
   - Typing stays available.
   - Holding the phone close while speaking raises the signal-to-noise ratio well above the 5 dB tested here.
4. **If the follow-up can't get within reach of Google's accuracy in noise,** the choice between the Google recogniser and a weaker open engine is a values trade-off, for the owner to make with these numbers.

### Raw data

`raw/` holds:
- every transcript from every run (`host-*.json` from the desktop, `device-*.json` from the emulator)
- the full generated summary, including per-accent tables (`summary.md`)
- the clip list (`clips-manifest.json`)
