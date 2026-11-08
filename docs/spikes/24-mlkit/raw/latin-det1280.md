### latin-det1280

engine: PaddleOCR PP-OCRv5 mobile on ncnn 20261020, config: PP-OCRv5 mobile latin rec, det 1280px, 2 threads, fp16=true

| Metric | Value |
|---|---|
| Images | 37 |
| CER, order-agnostic word bag, case-sensitive | 5.8% |
| CER, order-agnostic word bag, case-insensitive | 5.8% |
| WER, order-agnostic word bag, case-sensitive | 10.2% |
| WER, order-agnostic word bag, case-insensitive | 10.1% |
| CER, strict ground-truth order, case-sensitive | 26.7% |
| CER, strict ground-truth order, case-insensitive | 26.5% |
| WER, strict ground-truth order, case-sensitive | 32.2% |
| WER, strict ground-truth order, case-insensitive | 32.1% |
| Line recall (IoU >= 0.5) | 92.3% (685/742) |
| Line precision | 89.3% (685/767) |
| Word box recall (IoU >= 0.5) | 86.3% (1711/1982) |
| Word box recall with matching text | 84.4% |
| Matched-line order equals ground truth, images (native / XY-cut) | 11 / 19 of 37 |
| Latency median / p95 | 626 / 1159 ms |
| Device | unknown Android ATD built for x86_64 (API 36) |
| ABIs | x86_64 |
| CPU | Android virtual processor |
| Cores | 2 |
| Median stage ms | prepare=3, detect=259, postprocess=3, recognise=363 |
| Peak RSS of test process (kB) | 1178964 |

| Group | Images | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|---|
| category: label | 7 | 9.6% | 9.7% | 91.3% | 81.5% | 737 |
| category: menu | 12 | 5.6% | 12.8% | 89.4% | 87.8% | 716 |
| category: receipt | 8 | 0.3% | 0.8% | 97.5% | 94.8% | 623 |
| category: sign | 10 | 5.2% | 18.6% | 100.0% | 77.8% | 373 |
| tag: chalkboard | 3 | 1.6% | 3.3% | 91.7% | 87.0% | 525 |
| tag: handwriting | 5 | 3.5% | 9.7% | 77.4% | 85.4% | 525 |
| tag: small-print | 7 | 0.8% | 3.1% | 94.8% | 88.6% | 1085 |
| tag: rotated | 6 | 3.4% | 12.1% | 83.9% | 88.3% | 612 |
| tag: perspective | 3 | 0.5% | 2.9% | 97.7% | 89.3% | 489 |
| tag: accents | 6 | 7.8% | 12.1% | 89.4% | 86.5% | 718 |
| lighting: low-light | 4 | 2.8% | 12.8% | 97.9% | 93.6% | 373 |

| Image | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|
| label-cereal | 0.0% | 0.0% | 95.3% (41/43) | 92.0% | 1004 |
| label-crisps | 0.2% | 1.2% | 83.3% (30/36) | 92.8% | 1041 |
| label-medicine | 0.2% | 0.7% | 100.0% (23/23) | 84.8% | 1095 |
| label-olive-oil | 0.0% | 0.0% | 100.0% (8/8) | 82.9% | 504 |
| label-shampoo | 63.9% | 70.1% | 60.0% (6/10) | 37.7% | 545 |
| label-soup-tin | 0.0% | 0.0% | 100.0% (13/13) | 85.7% | 737 |
| label-tea-box | 0.6% | 2.5% | 93.8% (15/16) | 87.5% | 694 |
| menu-brunch | 9.4% | 16.0% | 81.3% (26/32) | 88.0% | 725 |
| menu-cafe-chalkboard | 0.6% | 3.2% | 84.4% (27/32) | 85.5% | 626 |
| menu-chippy | 5.3% | 15.2% | 100.0% (30/30) | 97.8% | 631 |
| menu-cocktails | 3.0% | 12.7% | 100.0% (29/29) | 96.4% | 720 |
| menu-curry-house | 3.1% | 16.9% | 100.0% (38/38) | 98.7% | 859 |
| menu-kids | 7.5% | 21.7% | 36.0% (9/25) | 82.6% | 604 |
| menu-patisserie | 6.8% | 8.9% | 96.3% (26/27) | 84.4% | 646 |
| menu-pub-grub | 3.3% | 12.3% | 97.5% (39/40) | 81.9% | 1086 |
| menu-sunday-roast | 0.8% | 4.6% | 96.6% (28/29) | 90.8% | 731 |
| menu-tapas | 1.6% | 6.3% | 96.7% (29/30) | 92.1% | 716 |
| menu-trattoria | 14.3% | 21.7% | 80.9% (38/47) | 80.4% | 1210 |
| menu-vegan-board | 0.0% | 0.0% | 100.0% (18/18) | 90.0% | 525 |
| receipt-cafe | 2.8% | 5.6% | 100.0% (17/17) | 97.2% | 621 |
| receipt-parking-ticket | 0.0% | 0.0% | 100.0% (11/11) | 87.0% | 489 |
| receipt-petrol | 0.0% | 0.0% | 100.0% (16/16) | 96.9% | 588 |
| receipt-pharmacy | 0.0% | 0.0% | 95.2% (20/21) | 94.3% | 795 |
| receipt-restaurant | 0.0% | 0.0% | 96.2% (25/26) | 98.1% | 800 |
| receipt-returns | 0.0% | 0.0% | 91.7% (11/12) | 100.0% | 576 |
| receipt-supermarket | 0.2% | 1.1% | 97.6% (41/42) | 100.0% | 1159 |
| receipt-takeaway | 0.0% | 0.0% | 100.0% (18/18) | 78.7% | 660 |
| sign-a-board | 8.2% | 9.5% | 100.0% (10/10) | 85.7% | 385 |
| sign-bakery | 0.0% | 0.0% | 100.0% (4/4) | 86.7% | 375 |
| sign-barber | 6.4% | 30.0% | 100.0% (3/3) | 80.0% | 340 |
| sign-butcher | 3.0% | 14.3% | 100.0% (3/3) | 71.4% | 389 |
| sign-chippy | 0.0% | 0.0% | 100.0% (3/3) | 76.9% | 337 |
| sign-door-closed | 7.5% | 25.0% | 100.0% (8/8) | 81.3% | 359 |
| sign-hardware | 10.2% | 42.9% | 100.0% (3/3) | 64.3% | 352 |
| sign-newsagent | 2.9% | 11.1% | 100.0% (4/4) | 66.7% | 407 |
| sign-pharmacy-hours | 5.0% | 24.2% | 100.0% (12/12) | 81.8% | 510 |
| sign-pub | 7.4% | 30.8% | 100.0% (3/3) | 76.9% | 368 |
