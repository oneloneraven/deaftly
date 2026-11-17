
### Host (Linux, x86_64): word error rate by condition

| Engine | clean | pcafeter-10db | straffic-10db | pstation-10db | pcafeter-5db | straffic-5db | pstation-5db | All noisy |
|---|---|---|---|---|---|---|---|---|
| sherpa-zipformer-20M | 8.9% | 12.8% | 11.9% | 10.8% | 18.0% | 14.2% | 13.6% | 13.5% |
| sherpa-zipformer-en | 9.3% | 12.8% | 13.0% | 11.9% | 16.9% | 14.7% | 13.3% | 13.8% |
| vosk-en-us-lgraph | 5.6% | 11.0% | 6.9% | 8.2% | 22.0% | 8.7% | 12.6% | 11.6% |
| vosk-small-en-us | 6.8% | 10.4% | 8.7% | 9.5% | 19.1% | 9.3% | 13.5% | 11.7% |

### Host (Linux, x86_64): word error rate by accent (clean / all noisy)

| Engine | English | Scottish | Welsh | Irish | NorthernIrish | Indian |
|---|---|---|---|---|---|---|
| sherpa-zipformer-20M | 7.0% / 14.3% | 5.6% / 11.7% | 12.8% / 17.0% | 9.8% / 13.1% | 7.5% / 10.7% | 14.7% / 17.8% |
| sherpa-zipformer-en | 11.8% / 14.3% | 7.7% / 9.7% | 10.6% / 14.9% | 10.7% / 15.0% | 7.5% / 12.5% | 8.0% / 17.8% |
| vosk-en-us-lgraph | 4.8% / 16.0% | 6.7% / 11.9% | 6.4% / 17.7% | 3.4% / 9.9% | 8.0% / 9.9% | 5.3% / 8.0% |
| vosk-small-en-us | 6.4% / 12.6% | 6.2% / 12.6% | 10.6% / 14.2% | 4.9% / 10.1% | 6.9% / 11.3% | 9.3% / 11.7% |

### Host: speed

| Engine | Real-time factor (median) | First words shown, after speech starts (median / p95) |
|---|---|---|
| sherpa-zipformer-20M | 0.065 | 1.11 s / 1.70 s |
| sherpa-zipformer-en | 0.161 | 1.11 s / 1.70 s |
| vosk-en-us-lgraph | 1.966 | 1.31 s / 1.84 s |
| vosk-small-en-us | 0.362 | 1.01 s / 1.54 s |

### Emulator: word error rate by condition

| Engine | clean | pcafeter-10db | straffic-10db | pstation-10db | pcafeter-5db | straffic-5db | pstation-5db | All noisy |
|---|---|---|---|---|---|---|---|---|
| android-en-GB | 4.3% | 6.1% | 4.1% | 5.4% | 9.3% | 4.5% | 7.1% | 6.1% |
| android-en-US | 4.2% | 7.2% | 4.3% | 5.8% | 10.4% | 4.9% | 7.5% | 6.7% |
| sherpa-zipformer-20M | 7.1% | n/a | n/a | n/a | 14.5% | n/a | n/a | 14.5% |
| sherpa-zipformer-en-beam | 7.1% | n/a | n/a | n/a | 11.5% | n/a | n/a | 11.5% |
| sherpa-zipformer-en | 6.9% | n/a | n/a | n/a | 11.9% | n/a | n/a | 11.9% |
| vosk-en-us-lgraph | 5.9% | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| vosk-small-en-us | 6.8% | n/a | n/a | n/a | 19.2% | n/a | n/a | 19.2% |

### Emulator: word error rate by accent (clean / all noisy)

| Engine | English | Scottish | Welsh | Irish | NorthernIrish | Indian |
|---|---|---|---|---|---|---|
| android-en-GB | 3.7% / 6.2% | 2.6% / 5.3% | 12.8% / 7.8% | 6.3% / 9.2% | 4.6% / 5.5% | 1.3% / 2.8% |
| android-en-US | 3.2% / 6.1% | 1.5% / 5.8% | 10.6% / 9.6% | 6.8% / 9.8% | 5.2% / 6.4% | 2.0% / 3.7% |
| sherpa-zipformer-20M | 4.8% / 17.6% | 5.1% / 12.3% | 14.9% / 8.5% | 6.3% / 14.1% | 8.0% / 16.1% | 10.0% / 14.0% |
| sherpa-zipformer-en-beam | 8.6% / 13.9% | 5.1% / 10.3% | 14.9% / 4.3% | 7.8% / 9.8% | 6.3% / 14.9% | 5.3% / 10.7% |
| sherpa-zipformer-en | 8.6% / 13.4% | 4.6% / 9.7% | 14.9% / 4.3% | 7.3% / 12.7% | 6.3% / 14.9% | 5.3% / 10.7% |
| vosk-en-us-lgraph | 7.0% / n/a | 6.7% / n/a | 6.4% / n/a | 3.4% / n/a | 7.5% / n/a | 5.3% / n/a |
| vosk-small-en-us | 7.0% / 21.4% | 6.2% / 20.5% | 10.6% / 12.8% | 4.9% / 20.5% | 6.9% / 19.5% | 8.7% / 14.7% |

### Emulator: speed and memory

| Engine | Load | Per 100 ms chunk (median / p95) | Real-time factor | First words shown after speech starts (median / p95) | Final text after audio ends (median) | App memory, PSS (max) |
|---|---|---|---|---|---|---|
| android-en-GB | 0.0 s | n/a | n/a (fed in real time) | 1.23 s / 1.77 s | 1014 ms | 31 MB |
| android-en-US | 0.0 s | n/a | n/a (fed in real time) | 1.23 s / 1.77 s | 1023 ms | 32 MB |
| sherpa-zipformer-20M | 0.2 s | 0 / 14 ms | 0.04 | 1.13 s / 1.70 s | 40 ms | 121 MB |
| sherpa-zipformer-en-beam | 0.3 s | 0 / 49 ms | 0.13 | 1.15 s / 1.70 s | 110 ms | 222 MB |
| sherpa-zipformer-en | 0.6 s | 0 / 47 ms | 0.13 | 1.16 s / 1.70 s | 106 ms | 223 MB |
| vosk-en-us-lgraph | 2.7 s | 0 / 669 ms | 0.98 | 1.34 s / 1.84 s | 182 ms | 420 MB |
| vosk-small-en-us | 0.7 s | 0 / 116 ms | 0.20 | 1.01 s / 1.54 s | 26 ms | 195 MB |
