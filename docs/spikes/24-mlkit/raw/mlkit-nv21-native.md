### mlkit-nv21-native

engine: ML Kit Text Recognition v2 16.0.1, bundled Latin, input: NV21 via InputImage.fromByteBuffer, order: NATIVE

| Metric | Value |
|---|---|
| Images | 37 |
| CER, order-agnostic word bag, case-sensitive | 6.3% |
| CER, order-agnostic word bag, case-insensitive | 6.3% |
| WER, order-agnostic word bag, case-sensitive | 15.5% |
| WER, order-agnostic word bag, case-insensitive | 15.4% |
| CER, strict ground-truth order, case-sensitive | 32.0% |
| CER, strict ground-truth order, case-insensitive | 31.9% |
| WER, strict ground-truth order, case-sensitive | 41.1% |
| WER, strict ground-truth order, case-insensitive | 41.1% |
| Line recall (IoU >= 0.5) | 95.7% (710/742) |
| Line precision | 96.1% (710/739) |
| Word box recall (IoU >= 0.5) | 93.3% (1850/1982) |
| Word box recall with matching text | 85.2% |
| Matched-line order equals ground truth, images (native / XY-cut) | 11 / 23 of 37 |
| Latency median / p95 | 2773 / 4244 ms |
| Device | unknown Android ATD built for x86_64 (API 36) |
| ABIs | x86_64 |
| CPU | Android virtual processor |
| Cores | 2 |
| Median stage ms | n/a |
| Peak RSS of test process (kB) | 543916 |

| Group | Images | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|---|
| category: label | 7 | 8.8% | 14.0% | 96.0% | 93.8% | 2996 |
| category: menu | 12 | 5.1% | 16.9% | 95.2% | 94.2% | 3037 |
| category: receipt | 8 | 3.5% | 8.5% | 97.5% | 96.7% | 2825 |
| category: sign | 10 | 10.5% | 28.1% | 92.5% | 79.6% | 2154 |
| tag: chalkboard | 3 | 12.0% | 26.0% | 100.0% | 95.1% | 2631 |
| tag: handwriting | 5 | 11.6% | 27.6% | 98.9% | 95.1% | 2580 |
| tag: small-print | 7 | 2.1% | 7.0% | 98.6% | 95.5% | 3587 |
| tag: rotated | 6 | 6.8% | 17.9% | 96.4% | 92.9% | 2650 |
| tag: perspective | 3 | 2.2% | 10.7% | 97.7% | 96.1% | 2592 |
| tag: accents | 6 | 3.0% | 10.6% | 92.4% | 94.0% | 3077 |
| lighting: low-light | 4 | 6.0% | 24.8% | 97.9% | 93.6% | 2147 |

| Image | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|
| label-cereal | 0.3% | 1.8% | 95.3% (41/43) | 99.1% | 3875 |
| label-crisps | 2.4% | 12.0% | 100.0% (36/36) | 98.8% | 3351 |
| label-medicine | 1.0% | 3.6% | 100.0% (23/23) | 97.1% | 3587 |
| label-olive-oil | 1.0% | 4.9% | 87.5% (7/8) | 92.7% | 2292 |
| label-shampoo | 51.7% | 68.8% | 70.0% (7/10) | 75.3% | 2715 |
| label-soup-tin | 2.2% | 10.0% | 100.0% (13/13) | 92.9% | 2822 |
| label-tea-box | 1.7% | 6.3% | 100.0% (16/16) | 95.0% | 2996 |
| menu-brunch | 0.7% | 4.0% | 100.0% (32/32) | 94.7% | 3141 |
| menu-cafe-chalkboard | 8.5% | 25.8% | 100.0% (32/32) | 96.8% | 2988 |
| menu-chippy | 2.6% | 8.7% | 80.0% (24/30) | 95.7% | 2650 |
| menu-cocktails | 5.0% | 27.3% | 100.0% (29/29) | 98.2% | 3193 |
| menu-curry-house | 5.0% | 23.4% | 100.0% (38/38) | 97.4% | 3305 |
| menu-kids | 11.6% | 30.4% | 96.0% (24/25) | 97.8% | 2607 |
| menu-patisserie | 9.1% | 24.4% | 74.1% (20/27) | 84.4% | 2764 |
| menu-pub-grub | 5.1% | 13.8% | 100.0% (40/40) | 88.4% | 4054 |
| menu-sunday-roast | 2.7% | 13.8% | 100.0% (29/29) | 96.9% | 2872 |
| menu-tapas | 2.8% | 9.5% | 93.3% (28/30) | 92.1% | 3062 |
| menu-trattoria | 3.0% | 13.0% | 95.7% (45/47) | 97.1% | 4586 |
| menu-vegan-board | 15.8% | 27.5% | 100.0% (18/18) | 92.5% | 2631 |
| receipt-cafe | 3.3% | 5.6% | 94.1% (16/17) | 100.0% | 2825 |
| receipt-parking-ticket | 0.0% | 0.0% | 100.0% (11/11) | 100.0% | 2592 |
| receipt-petrol | 3.7% | 6.3% | 100.0% (16/16) | 100.0% | 2702 |
| receipt-pharmacy | 10.3% | 22.6% | 100.0% (21/21) | 94.3% | 3104 |
| receipt-restaurant | 2.1% | 7.7% | 96.2% (25/26) | 96.2% | 3270 |
| receipt-returns | 7.9% | 22.6% | 91.7% (11/12) | 90.3% | 2703 |
| receipt-supermarket | 0.8% | 2.2% | 97.6% (41/42) | 98.9% | 4006 |
| receipt-takeaway | 1.0% | 4.3% | 100.0% (18/18) | 93.6% | 2845 |
| sign-a-board | 15.3% | 23.8% | 100.0% (10/10) | 95.2% | 2217 |
| sign-bakery | 3.1% | 13.3% | 75.0% (3/4) | 86.7% | 2120 |
| sign-barber | 2.1% | 10.0% | 100.0% (3/3) | 90.0% | 1977 |
| sign-butcher | 4.5% | 21.4% | 100.0% (3/3) | 71.4% | 2157 |
| sign-chippy | 8.2% | 30.8% | 100.0% (3/3) | 84.6% | 2061 |
| sign-door-closed | 7.5% | 31.3% | 100.0% (8/8) | 87.5% | 2222 |
| sign-hardware | 44.1% | 50.0% | 100.0% (3/3) | 50.0% | 2072 |
| sign-newsagent | 5.7% | 22.2% | 75.0% (3/4) | 66.7% | 2208 |
| sign-pharmacy-hours | 7.5% | 36.4% | 83.3% (10/12) | 78.8% | 2451 |
| sign-pub | 9.3% | 30.8% | 100.0% (3/3) | 84.6% | 2130 |
