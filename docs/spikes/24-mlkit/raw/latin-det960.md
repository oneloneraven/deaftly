### latin-det960

engine: PaddleOCR PP-OCRv5 mobile on ncnn 20261020, config: PP-OCRv5 mobile latin rec, det 960px, 2 threads, fp16=true

| Metric | Value |
|---|---|
| Images | 37 |
| CER, order-agnostic word bag, case-sensitive | 9.7% |
| CER, order-agnostic word bag, case-insensitive | 9.6% |
| WER, order-agnostic word bag, case-sensitive | 14.8% |
| WER, order-agnostic word bag, case-insensitive | 14.6% |
| CER, strict ground-truth order, case-sensitive | 29.8% |
| CER, strict ground-truth order, case-insensitive | 29.6% |
| WER, strict ground-truth order, case-sensitive | 37.2% |
| WER, strict ground-truth order, case-insensitive | 37.1% |
| Line recall (IoU >= 0.5) | 82.1% (609/742) |
| Line precision | 80.9% (609/753) |
| Word box recall (IoU >= 0.5) | 80.8% (1602/1982) |
| Word box recall with matching text | 78.2% |
| Matched-line order equals ground truth, images (native / XY-cut) | 15 / 21 of 37 |
| Latency median / p95 | 486 / 976 ms |
| Device | unknown Android ATD built for x86_64 (API 36) |
| ABIs | x86_64 |
| CPU | Android virtual processor |
| Cores | 2 |
| Median stage ms | prepare=3, detect=151, postprocess=2, recognise=332 |
| Peak RSS of test process (kB) | 1178964 |

| Group | Images | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|---|
| category: label | 7 | 11.4% | 12.0% | 78.5% | 77.2% | 617 |
| category: menu | 12 | 12.7% | 21.8% | 75.9% | 78.4% | 577 |
| category: receipt | 8 | 0.1% | 0.3% | 97.5% | 93.7% | 489 |
| category: sign | 10 | 6.8% | 18.6% | 88.7% | 78.4% | 260 |
| tag: chalkboard | 3 | 0.5% | 2.4% | 75.0% | 87.8% | 411 |
| tag: handwriting | 5 | 2.7% | 9.2% | 60.2% | 85.4% | 411 |
| tag: small-print | 7 | 5.7% | 8.0% | 83.1% | 82.3% | 875 |
| tag: rotated | 6 | 7.1% | 17.1% | 69.6% | 84.2% | 468 |
| tag: perspective | 3 | 6.5% | 15.5% | 95.5% | 82.5% | 380 |
| tag: accents | 6 | 13.8% | 20.5% | 71.2% | 75.6% | 583 |
| lighting: low-light | 4 | 1.0% | 5.5% | 97.9% | 94.5% | 260 |

| Image | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|
| label-cereal | 0.0% | 0.0% | 74.4% (32/43) | 88.4% | 875 |
| label-crisps | 0.2% | 1.2% | 61.1% (22/36) | 90.4% | 765 |
| label-medicine | 0.3% | 1.4% | 95.7% (22/23) | 81.9% | 949 |
| label-olive-oil | 1.0% | 4.9% | 100.0% (8/8) | 75.6% | 377 |
| label-shampoo | 75.7% | 85.7% | 60.0% (6/10) | 24.7% | 443 |
| label-soup-tin | 0.0% | 0.0% | 92.3% (12/13) | 88.6% | 617 |
| label-tea-box | 0.3% | 1.3% | 93.8% (15/16) | 81.3% | 584 |
| menu-brunch | 40.0% | 44.0% | 68.8% (22/32) | 58.7% | 587 |
| menu-cafe-chalkboard | 0.9% | 4.8% | 62.5% (20/32) | 83.9% | 513 |
| menu-chippy | 0.9% | 2.2% | 100.0% (30/30) | 97.8% | 470 |
| menu-cocktails | 1.2% | 7.3% | 100.0% (29/29) | 98.2% | 601 |
| menu-curry-house | 3.8% | 20.8% | 100.0% (38/38) | 98.7% | 722 |
| menu-kids | 7.5% | 21.7% | 20.0% (5/25) | 78.3% | 459 |
| menu-patisserie | 0.8% | 2.2% | 100.0% (27/27) | 86.7% | 505 |
| menu-pub-grub | 27.2% | 37.7% | 82.5% (33/40) | 59.4% | 934 |
| menu-sunday-roast | 9.7% | 24.6% | 93.1% (27/29) | 83.1% | 568 |
| menu-tapas | 1.2% | 6.3% | 66.7% (20/30) | 87.3% | 583 |
| menu-trattoria | 16.6% | 32.6% | 40.4% (19/47) | 67.4% | 1029 |
| menu-vegan-board | 0.0% | 0.0% | 88.9% (16/18) | 90.0% | 411 |
| receipt-cafe | 0.0% | 0.0% | 100.0% (17/17) | 94.4% | 486 |
| receipt-parking-ticket | 0.0% | 0.0% | 100.0% (11/11) | 87.0% | 380 |
| receipt-petrol | 0.0% | 0.0% | 100.0% (16/16) | 96.9% | 441 |
| receipt-pharmacy | 0.0% | 0.0% | 95.2% (20/21) | 94.3% | 652 |
| receipt-restaurant | 0.0% | 0.0% | 96.2% (25/26) | 98.1% | 657 |
| receipt-returns | 0.0% | 0.0% | 91.7% (11/12) | 96.8% | 469 |
| receipt-supermarket | 0.2% | 1.1% | 97.6% (41/42) | 98.9% | 980 |
| receipt-takeaway | 0.0% | 0.0% | 100.0% (18/18) | 76.6% | 526 |
| sign-a-board | 0.0% | 0.0% | 90.0% (9/10) | 95.2% | 280 |
| sign-bakery | 0.0% | 0.0% | 100.0% (4/4) | 73.3% | 260 |
| sign-barber | 0.0% | 0.0% | 100.0% (3/3) | 90.0% | 230 |
| sign-butcher | 3.0% | 14.3% | 100.0% (3/3) | 71.4% | 281 |
| sign-chippy | 0.0% | 0.0% | 66.7% (2/3) | 69.2% | 230 |
| sign-door-closed | 6.0% | 25.0% | 75.0% (6/8) | 87.5% | 249 |
| sign-hardware | 44.1% | 50.0% | 100.0% (3/3) | 64.3% | 244 |
| sign-newsagent | 10.0% | 38.9% | 100.0% (4/4) | 72.2% | 286 |
| sign-pharmacy-hours | 5.7% | 27.3% | 83.3% (10/12) | 78.8% | 396 |
| sign-pub | 3.7% | 15.4% | 100.0% (3/3) | 76.9% | 260 |
