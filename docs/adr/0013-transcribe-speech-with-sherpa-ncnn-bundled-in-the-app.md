# 13. Transcribe speech with sherpa-ncnn, bundled in the app

## Status

Accepted

## Context

In side-by-side chat, the hearing person needs to reply by speaking, with their words transcribed into the chat like a typed message (#59). Transcription has to happen on the phone.

Two spikes compared the options on read speech from British, Irish and Indian-English speakers, clean and mixed with café, street and station noise (results: [#285](../spikes/285-speech-to-text/results.md), [#287](../spikes/287-speech-to-text-models/results.md)).

**Android's on-device recogniser:**
- accurate, and adds nothing to the APK
- but it's Google's, a BDS pressure target and a No Tech for Apartheid target ([ADR-0002](0002-screen-dependencies-against-bds-boycott-lists.md))
- it runs in Google's own process, so we can't test that audio stays on the phone

**Open-source engines:**
- Vosk was weaker in café babble.
- sherpa-ncnn's original audiobook-only models were also weaker in noise.
- sherpa-ncnn's streaming Zipformer trained on LibriSpeech and GigaSpeech matched Google in clean speech on a OnePlus 13 (4.2% of words wrong, against 4.1%). It beat Google in café noise (6.8% against 9.6%). Its first words appear as fast as Google's, about 1.1 s after speech starts, and it runs at a tenth of real time.
- That model adds about 124 MB compressed, and int8 quantisation can't shrink it with ncnn's current tools.

## Decision

- **Engine:** transcribe speech with sherpa-ncnn's streaming Zipformer trained on LibriSpeech and GigaSpeech (k2-fsa icefall, Apache-2.0), behind a `SpeechRecogniser` interface in `:core`, as [ADR-0010](0010-recognise-text-with-paddleocr-on-ncnn.md) did for text recognition.
- **Runtime:** build sherpa-ncnn against the app's own ncnn. The two share one runtime, so the native code adds about 1 MB.
- **Where the model comes from:** generate the ncnn files from k2-fsa's published checkpoint with the published export script, not from a third-party upload. Record their checksums.
- **Delivery:** bundle the model in the APK. Don't download it on first use. The app requests no `INTERNET` permission for speech.

## Consequences

- **Size:** the APK grows from about 15 MB to about 140 MB. Everyone downloads the model, including people who never use speech.
- **Privacy:** there's no Google code and no network in the speech path. "Audio never leaves your phone" can be backed by a test, like `OcrSendsNothingTest` ([ADR-0011](0011-review-permissions-against-an-allowlist.md)).
- **Microphone permission:** the feature adds `RECORD_AUDIO`, which needs the permissions allowlist, threat model, record of processing and privacy page updated in the same PR (ADR-0011).
- **Memory:** about 230 MB of extra memory while listening.
- **Still to measure:** speed on a mid-range phone, and accuracy on real recordings made in shops and cafés. Both must be checked before release.
- **Revisit if** distribution moves to a store with on-demand asset delivery (#69), or ncnn's tools learn to quantise sherpa's custom layers.
