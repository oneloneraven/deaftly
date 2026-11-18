
### Host (Linux, x86_64): word error rate by condition

| Engine | clean | pcafeter-10db | straffic-10db | pstation-10db | pcafeter-5db | straffic-5db | pstation-5db | All noisy |
|---|---|---|---|---|---|---|---|---|
| sherpa-librigiga-70M | 6.1% | 4.8% | 5.4% | 4.9% | 7.0% | 4.7% | 5.7% | 5.4% |
| sherpa-zipformer-20M | 7.2% | 9.9% | 9.3% | 8.8% | 14.5% | 12.0% | 11.9% | 11.1% |
| sherpa-zipformer-en | 7.6% | 10.4% | 9.4% | 9.2% | 12.4% | 11.3% | 11.6% | 10.7% |
| vosk-en-us-lgraph | 5.6% | 11.0% | 6.9% | 8.2% | 22.0% | 8.7% | 12.6% | 11.6% |
| vosk-small-en-us | 6.8% | 10.4% | 8.7% | 9.5% | 19.1% | 9.3% | 13.5% | 11.7% |

### Host (Linux, x86_64): word error rate by accent (clean / all noisy)

| Engine | English | Scottish | Welsh | Irish | NorthernIrish | Indian |
|---|---|---|---|---|---|---|
| sherpa-librigiga-70M | 5.9% / 6.1% | 4.6% / 2.6% | 14.9% / 9.6% | 8.8% / 5.8% | 4.6% / 6.8% | 3.3% / 4.8% |
| sherpa-zipformer-20M | 4.8% / 11.4% | 5.6% / 10.0% | 14.9% / 9.6% | 6.3% / 9.8% | 8.0% / 11.7% | 10.0% / 13.6% |
| sherpa-zipformer-en | 10.2% / 12.0% | 5.6% / 7.4% | 17.0% / 13.1% | 7.8% / 12.4% | 6.3% / 12.2% | 5.3% / 8.7% |
| vosk-en-us-lgraph | 4.8% / 16.0% | 6.7% / 11.9% | 6.4% / 17.7% | 3.4% / 9.9% | 8.0% / 9.9% | 5.3% / 8.0% |
| vosk-small-en-us | 6.4% / 12.6% | 6.2% / 12.6% | 10.6% / 14.2% | 4.9% / 10.1% | 6.9% / 11.3% | 9.3% / 11.7% |

### Host: speed

| Engine | Real-time factor (median) | First words shown, after speech starts (median / p95) |
|---|---|---|
| sherpa-librigiga-70M | 0.082 | 1.13 s / 1.70 s |
| sherpa-zipformer-20M | 0.035 | 1.13 s / 1.70 s |
| sherpa-zipformer-en | 0.082 | 1.16 s / 1.70 s |
| vosk-en-us-lgraph | 1.966 | 1.31 s / 1.84 s |
| vosk-small-en-us | 0.362 | 1.01 s / 1.54 s |

### OnePlus 13: word error rate by condition

| Engine | clean | pcafeter-10db | straffic-10db | pstation-10db | pcafeter-5db | straffic-5db | pstation-5db | All noisy |
|---|---|---|---|---|---|---|---|---|
| android-en-GB | 4.1% | n/a | n/a | n/a | 9.6% | n/a | n/a | 9.6% |
| sherpa-librigiga-70M | 4.2% | n/a | n/a | n/a | 6.8% | n/a | n/a | 6.8% |
| sherpa-zipformer-20M | 7.1% | n/a | n/a | n/a | 14.1% | n/a | n/a | 14.1% |
| vosk-small-en-us | 6.8% | n/a | n/a | n/a | 19.3% | n/a | n/a | 19.3% |

### OnePlus 13: word error rate by accent (clean / all noisy)

| Engine | English | Scottish | Welsh | Irish | NorthernIrish | Indian |
|---|---|---|---|---|---|---|
| android-en-GB | 3.7% / 9.1% | 1.5% / 10.3% | 10.6% / 12.8% | 6.8% / 12.7% | 4.6% / 8.6% | 1.3% / 5.3% |
| sherpa-librigiga-70M | 3.7% / 9.6% | 2.6% / 3.1% | 8.5% / 6.4% | 5.4% / 5.9% | 4.6% / 8.0% | 3.3% / 8.0% |
| sherpa-zipformer-20M | 4.8% / 16.0% | 5.1% / 11.8% | 14.9% / 8.5% | 6.3% / 14.1% | 8.0% / 16.1% | 10.0% / 14.0% |
| vosk-small-en-us | 6.4% / 21.9% | 6.2% / 20.5% | 10.6% / 12.8% | 4.9% / 20.5% | 6.9% / 20.1% | 9.3% / 14.0% |

### OnePlus 13: speed and memory

| Engine | Load | Per 100 ms chunk (median / p95) | Real-time factor | First words shown after speech starts (median / p95) | Final text after audio ends (median) | App memory, PSS (max) |
|---|---|---|---|---|---|---|
| android-en-GB | 0.0 s | n/a | n/a (fed in real time) | 1.15 s / 1.77 s | 1025 ms | 35 MB |
| sherpa-librigiga-70M | 0.2 s | 0 / 35 ms | 0.10 | 1.13 s / 1.70 s | 86 ms | 229 MB |
| sherpa-zipformer-20M | 0.1 s | 0 / 14 ms | 0.04 | 1.13 s / 1.70 s | 27 ms | 130 MB |
| vosk-small-en-us | 0.4 s | 0 / 96 ms | 0.17 | 1.01 s / 1.54 s | 17 ms | 197 MB |
