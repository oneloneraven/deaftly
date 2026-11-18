## Finding a shippable speech-to-text model: spike results

Follow-up to [#285](../285-speech-to-text/results.md), for #59. The harness is on branch `spike/285-speech-to-text`, not for merge. The test clips, scoring and harness are the same as in #285, so the numbers compare directly.

### Summary

- **A sherpa-ncnn model trained on LibriSpeech and GigaSpeech is as accurate as Google's recogniser in clean speech, and more accurate in café noise.** On a real phone it got 4.2% of words wrong in clean speech against Google's 4.1%, and 6.8% in café noise against Google's 9.6%.
- **It's big:** about 124 MB compressed, which takes the APK from 15 MB to about 140 MB.
- **It can't be shrunk with int8 quantisation.** ncnn's conversion tools corrupt the Zipformer encoder (see below). Even a working int8 conversion would only bring it to about 84 MB, because most of its weights sit in layer types that ncnn doesn't quantise.
- **sherpa-ncnn runs on stock ncnn 20261020,** the version the app already ships. The app and the speech engine can share one ncnn, so the native code adds about 1 MB.

### Models tested

| Model | Trained on | Licence | Format |
|---|---|---|---|
| `sherpa-ncnn-zipformer-libri-giga-70M-2023-04-04` | LibriSpeech and GigaSpeech: audiobooks plus podcasts and YouTube, about 10,000 hours | Apache-2.0 | ncnn export by IconicAI on Hugging Face of icefall's `icefall-libri-giga-pruned-transducer-stateless7-streaming-2023-04-04` (k2-fsa, icefall PR 984), with weights stored as fp16 |
| `sherpa-ncnn-streaming-zipformer-20M-2023-02-17` (baseline from #285) | LibriSpeech, about 960 hours of audiobooks | Apache-2.0 | ncnn, fp16 |
| Vosk small (baseline from #285) | Not stated | Apache-2.0 | Kaldi |
| Android on-device, en-GB (reference) | Not stated | Proprietary | n/a |

Screening (ADR-0002): k2-fsa, icefall, IconicAI, Hugging Face, LibriSpeech (OpenSLR) and GigaSpeech (SpeechColab) are on neither BDS list. The lists were checked on 17 November 2026. Before shipping, the ncnn files should be regenerated from k2-fsa's own checkpoint with the published export script, rather than taken from a third-party upload.

Considered and not tested:

- **Kroko (Banafo, 2025):** a newer streaming Zipformer2. It's licensed CC-BY-SA, so converted weights would have to be published under the same licence. More importantly, sherpa-ncnn doesn't support the Zipformer2 architecture. Using it would mean either sherpa-onnx, which needs ONNX Runtime (Microsoft, a BDS target), or porting Zipformer2 to ncnn ourselves.
- **Other 2024–2026 English streaming models on Hugging Face** are all Zipformer2 or ONNX-only, so the same limits apply.

### Results

Word error rate. Lower is better. 120 clips per condition: read speech from 20 speakers with British, Irish and Indian-English accents. Café is DEMAND café noise at 5 dB.

| Engine | OnePlus 13: clean | OnePlus 13: café 5 dB | Emulator: clean | Emulator: café 5 dB | Desktop: all six noisy conditions |
|---|---|---|---|---|---|
| sherpa-ncnn libri-giga | **4.2%** | **6.8%** | 4.2% | 6.8% | 5.4% |
| Android on-device, en-GB | 4.1% | 9.6% | 4.3% | 9.3% | 6.1% (emulator) |
| sherpa-ncnn 20M | 7.1% | 14.1% | 7.1% | 14.5% | 11.1% |
| Vosk small | 6.8% | 19.3% | 6.8% | 19.2% | 11.7% |

- **The libri-giga model holds up across every noise type** on the desktop: café, street and station, at both 10 dB and 5 dB. Its error rate stays between 4.7% and 7.0% throughout.
- **Per-accent results** are in `raw/`, but with 1 to 4 speakers per accent they're indicative only. On the phone, the libri-giga model is ahead of or level with Google for every accent except English in noise (9.6% against 9.1%).
- **The desktop harness from #285 had a bug,** now fixed. sherpa's Python `reset()` didn't clear its buffers, so the end of one clip leaked into the next. With that fixed, the desktop figures for the #285 sherpa models also come down: the 20M model from 13.5% to 11.1% across the noisy conditions. The emulator figures in #285 were unaffected.

| Engine on the OnePlus 13 | Real-time factor | First words after speech starts, median / p95 | Final text after audio ends, median | App memory, PSS max | Added to the APK, compressed |
|---|---|---|---|---|---|
| sherpa-ncnn libri-giga | 0.10 | 1.13 / 1.70 s | 86 ms | 229 MB | about 124 MB model, about 1 MB library |
| sherpa-ncnn 20M | 0.04 | 1.13 / 1.70 s | 27 ms | 130 MB | about 37 MB model, about 1 MB library |
| Vosk small | 0.17 | 1.01 / 1.54 s | 17 ms | 197 MB | about 40 MB model, about 10 MB library |
| Android on-device, en-GB | n/a, fed in real time | 1.15 / 1.77 s | about 1 s (waits for silence) | 35 MB in our process | 0 MB |

- **The OnePlus 13 is a flagship** (Snapdragon 8 Elite, SM8750, 16 GB of RAM).
- **A mid-range phone wasn't available.** Expect a real-time factor two to three times higher there: about 0.2 to 0.3 for libri-giga, still well under real time. That's an estimate, not a measurement, and it has to be checked before release.
- **Memory:** 229 MB of extra memory while listening is acceptable on a modern phone. It could matter on 3 to 4 GB devices.

### int8 quantisation: why it failed

- **What we built:** ncnn 20261020's tools (`ncnnoptimize`, `ncnn2table`, `ncnn2int8`), sherpa-ncnn's `generate-int8-scale-table`, and sherpa-ncnn itself against stock ncnn. All were compiled from source with zig.
- **Calibration:** the scale tables were generated from 40 clips of 8 VCTK speakers who aren't in the test set, half of them mixed with noise.
- **The joiner quantises and runs correctly.**
- **The encoder doesn't.** Every int8 encoder fails to load ("ModelBin read weight_data failed"). The cause is that ncnn's model writer doesn't know the weights of sherpa's custom layers, so any rewrite of the encoder misplaces the weight data that follows them. Even a plain fp32 round trip through `ncnnoptimize`, with no quantisation, produces garbage output. That's likely why k2-fsa has never published an int8 Zipformer for ncnn.
- **Fixing it** would mean teaching ncnn's writer about those custom layers. Even then, only the 181 linear layers would shrink, leaving the libri-giga encoder at about 84 MB rather than 133 MB.

### Real-world recordings

Not done. Recordings of everyday counter phrases by hearing speakers are still to come. When they're available, they'll be scored with the same harness and the results added here.

### Recommendation

1. **Adopt the sherpa-ncnn libri-giga model**, built against the app's own ncnn, with the ncnn files regenerated from k2-fsa's checkpoint.
2. **Delivering a 124 MB model is the open decision for the owner.** Options:
   - **Bundle it in the APK:** about 140 MB. No new permission, and "audio never leaves your phone" still holds. A large download for an app distributed as a signed APK on GitHub (ADR-0007).
   - **Download it on first use** from a host we control. The APK stays small, and people who never use speech don't pay for it. But it needs the `INTERNET` permission (ADR-0011): a visible change to the app's privacy posture, a threat-model update, and a host vendor to screen.
   - **Ship the 20M model in the APK:** about 53 MB. It makes twice as many errors in café noise.
3. **Measure on a mid-range phone, and score real recordings, before release.**

### Raw data

`raw/` holds every transcript from the OnePlus 13 (`phone-*.json`), the emulator run of the libri-giga model, and the desktop runs with the fixed harness (`host-*.json`). It also has generated summaries with per-accent tables.
