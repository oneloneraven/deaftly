### mlkit-rgba-xycut

engine: ML Kit Text Recognition v2 16.0.1, bundled Latin, input: RGBA_8888 via Bitmap, order: XY_CUT

| Metric | Value |
|---|---|
| Images | 37 |
| CER, order-agnostic word bag, case-sensitive | 5.5% |
| CER, order-agnostic word bag, case-insensitive | 5.5% |
| WER, order-agnostic word bag, case-sensitive | 13.6% |
| WER, order-agnostic word bag, case-insensitive | 13.6% |
| CER, strict ground-truth order, case-sensitive | 13.7% |
| CER, strict ground-truth order, case-insensitive | 13.6% |
| WER, strict ground-truth order, case-sensitive | 22.4% |
| WER, strict ground-truth order, case-insensitive | 22.4% |
| Line recall (IoU >= 0.5) | 96.2% (714/742) |
| Line precision | 96.6% (714/739) |
| Word box recall (IoU >= 0.5) | 94.6% (1875/1982) |
| Word box recall with matching text | 87.7% |
| Matched-line order equals ground truth, images (native / XY-cut) | 11 / 21 of 37 |
| Latency median / p95 | 2749 / 4122 ms |
| Device | unknown Android ATD built for x86_64 (API 36) |
| ABIs | x86_64 |
| CPU | Android virtual processor |
| Cores | 2 |
| Median stage ms | n/a |
| Peak RSS of test process (kB) | 681076 |

| Group | Images | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|---|
| category: label | 7 | 8.8% | 13.1% | 98.0% | 93.3% | 2912 |
| category: menu | 12 | 4.0% | 14.6% | 95.0% | 96.6% | 3037 |
| category: receipt | 8 | 2.9% | 6.0% | 99.4% | 98.1% | 2779 |
| category: sign | 10 | 7.8% | 26.3% | 90.6% | 81.4% | 2104 |
| tag: chalkboard | 3 | 7.6% | 19.5% | 100.0% | 96.7% | 2553 |
| tag: handwriting | 5 | 8.6% | 23.2% | 97.8% | 96.8% | 2524 |
| tag: small-print | 7 | 1.7% | 6.5% | 99.5% | 97.3% | 3549 |
| tag: rotated | 6 | 4.2% | 14.2% | 98.2% | 96.7% | 2673 |
| tag: perspective | 3 | 2.0% | 10.7% | 97.7% | 96.1% | 2514 |
| tag: accents | 6 | 2.4% | 9.4% | 94.1% | 95.4% | 3015 |
| lighting: low-light | 4 | 3.5% | 16.5% | 100.0% | 96.3% | 2102 |

| Image | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|
| label-cereal | 0.3% | 1.8% | 100.0% (43/43) | 100.0% | 3561 |
| label-crisps | 1.9% | 10.8% | 100.0% (36/36) | 97.6% | 3319 |
| label-medicine | 1.1% | 3.6% | 100.0% (23/23) | 97.1% | 3586 |
| label-olive-oil | 1.5% | 7.3% | 87.5% (7/8) | 90.2% | 2424 |
| label-shampoo | 51.7% | 61.0% | 80.0% (8/10) | 67.5% | 2655 |
| label-soup-tin | 2.2% | 11.4% | 100.0% (13/13) | 97.1% | 2831 |
| label-tea-box | 1.7% | 6.3% | 100.0% (16/16) | 96.3% | 2838 |
| menu-brunch | 1.4% | 8.0% | 100.0% (32/32) | 96.0% | 3040 |
| menu-cafe-chalkboard | 4.1% | 17.7% | 100.0% (32/32) | 98.4% | 2985 |
| menu-chippy | 4.8% | 8.7% | 73.3% (22/30) | 97.8% | 2709 |
| menu-cocktails | 4.7% | 23.6% | 100.0% (29/29) | 100.0% | 3080 |
| menu-curry-house | 4.3% | 22.1% | 100.0% (38/38) | 98.7% | 3265 |
| menu-kids | 11.6% | 30.4% | 92.0% (23/25) | 100.0% | 2622 |
| menu-patisserie | 8.0% | 24.4% | 85.2% (23/27) | 88.9% | 2835 |
| menu-pub-grub | 2.8% | 10.1% | 100.0% (40/40) | 94.2% | 4125 |
| menu-sunday-roast | 2.4% | 13.8% | 100.0% (29/29) | 96.9% | 3133 |
| menu-tapas | 0.3% | 1.6% | 100.0% (30/30) | 98.4% | 2946 |
| menu-trattoria | 2.7% | 11.6% | 89.4% (42/47) | 96.4% | 4487 |
| menu-vegan-board | 10.7% | 20.0% | 100.0% (18/18) | 95.0% | 2553 |
| receipt-cafe | 3.3% | 5.6% | 100.0% (17/17) | 100.0% | 2735 |
| receipt-parking-ticket | 0.0% | 0.0% | 100.0% (11/11) | 100.0% | 2514 |
| receipt-petrol | 0.0% | 0.0% | 100.0% (16/16) | 100.0% | 2672 |
| receipt-pharmacy | 12.1% | 22.6% | 100.0% (21/21) | 96.2% | 2974 |
| receipt-restaurant | 1.1% | 3.8% | 100.0% (26/26) | 98.1% | 3060 |
| receipt-returns | 0.6% | 3.2% | 100.0% (12/12) | 96.8% | 2632 |
| receipt-supermarket | 1.4% | 3.3% | 97.6% (41/42) | 100.0% | 3957 |
| receipt-takeaway | 2.1% | 4.3% | 100.0% (18/18) | 93.6% | 2837 |
| sign-a-board | 12.2% | 23.8% | 100.0% (10/10) | 95.2% | 2320 |
| sign-bakery | 3.1% | 13.3% | 75.0% (3/4) | 86.7% | 2092 |
| sign-barber | 2.1% | 10.0% | 100.0% (3/3) | 90.0% | 1992 |
| sign-butcher | 18.2% | 42.9% | 100.0% (3/3) | 57.1% | 2146 |
| sign-chippy | 8.2% | 30.8% | 100.0% (3/3) | 76.9% | 1989 |
| sign-door-closed | 7.5% | 31.3% | 100.0% (8/8) | 87.5% | 2128 |
| sign-hardware | 5.1% | 21.4% | 100.0% (3/3) | 78.6% | 1979 |
| sign-newsagent | 5.7% | 22.2% | 75.0% (3/4) | 72.2% | 2134 |
| sign-pharmacy-hours | 6.9% | 33.3% | 75.0% (9/12) | 81.8% | 2314 |
| sign-pub | 5.6% | 23.1% | 100.0% (3/3) | 84.6% | 2069 |
