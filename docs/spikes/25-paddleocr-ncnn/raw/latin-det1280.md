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
| Latency median / p95 | 629 / 1142 ms |
| Device | unknown Android ATD built for x86_64 (API 36) |
| ABIs | x86_64 |
| CPU | Android virtual processor |
| Cores | 2 |
| Median stage ms | prepare=3, detect=257, postprocess=3, recognise=355 |
| Peak RSS of test process (kB) | 1278096 |

| Group | Images | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|---|
| category: label | 7 | 9.6% | 9.7% | 91.3% | 81.5% | 736 |
| category: menu | 12 | 5.6% | 12.8% | 89.4% | 87.8% | 703 |
| category: receipt | 8 | 0.3% | 0.8% | 97.5% | 94.8% | 623 |
| category: sign | 10 | 5.2% | 18.6% | 100.0% | 77.8% | 365 |
| tag: chalkboard | 3 | 1.6% | 3.3% | 91.7% | 87.0% | 520 |
| tag: handwriting | 5 | 3.5% | 9.7% | 77.4% | 85.4% | 520 |
| tag: small-print | 7 | 0.8% | 3.1% | 94.8% | 88.6% | 1007 |
| tag: rotated | 6 | 3.4% | 12.1% | 83.9% | 88.3% | 611 |
| tag: perspective | 3 | 0.5% | 2.9% | 97.7% | 89.3% | 488 |
| tag: accents | 6 | 7.8% | 12.1% | 89.4% | 86.5% | 709 |
| lighting: low-light | 4 | 2.8% | 12.8% | 97.9% | 93.6% | 365 |

| Image | CER (ci) | WER (ci) | Line recall | Word box recall | Median ms |
|---|---|---|---|---|---|
| label-cereal | 0.0% | 0.0% | 95.3% (41/43) | 92.0% | 1007 |
| label-crisps | 0.2% | 1.2% | 83.3% (30/36) | 92.8% | 887 |
| label-medicine | 0.2% | 0.7% | 100.0% (23/23) | 84.8% | 1097 |
| label-olive-oil | 0.0% | 0.0% | 100.0% (8/8) | 82.9% | 504 |
| label-shampoo | 63.9% | 70.1% | 60.0% (6/10) | 37.7% | 536 |
| label-soup-tin | 0.0% | 0.0% | 100.0% (13/13) | 85.7% | 736 |
| label-tea-box | 0.6% | 2.5% | 93.8% (15/16) | 87.5% | 696 |
| menu-brunch | 9.4% | 16.0% | 81.3% (26/32) | 88.0% | 717 |
| menu-cafe-chalkboard | 0.6% | 3.2% | 84.4% (27/32) | 85.5% | 631 |
| menu-chippy | 5.3% | 15.2% | 100.0% (30/30) | 97.8% | 637 |
| menu-cocktails | 3.0% | 12.7% | 100.0% (29/29) | 96.4% | 717 |
| menu-curry-house | 3.1% | 16.9% | 100.0% (38/38) | 98.7% | 857 |
| menu-kids | 7.5% | 21.7% | 36.0% (9/25) | 82.6% | 595 |
| menu-patisserie | 6.8% | 8.9% | 96.3% (26/27) | 84.4% | 644 |
| menu-pub-grub | 3.3% | 12.3% | 97.5% (39/40) | 81.9% | 1078 |
| menu-sunday-roast | 0.8% | 4.6% | 96.6% (28/29) | 90.8% | 709 |
| menu-tapas | 1.6% | 6.3% | 96.7% (29/30) | 92.1% | 703 |
| menu-trattoria | 14.3% | 21.7% | 80.9% (38/47) | 80.4% | 1199 |
| menu-vegan-board | 0.0% | 0.0% | 100.0% (18/18) | 90.0% | 520 |
| receipt-cafe | 2.8% | 5.6% | 100.0% (17/17) | 97.2% | 618 |
| receipt-parking-ticket | 0.0% | 0.0% | 100.0% (11/11) | 87.0% | 488 |
| receipt-petrol | 0.0% | 0.0% | 100.0% (16/16) | 96.9% | 576 |
| receipt-pharmacy | 0.0% | 0.0% | 95.2% (20/21) | 94.3% | 795 |
| receipt-restaurant | 0.0% | 0.0% | 96.2% (25/26) | 98.1% | 806 |
| receipt-returns | 0.0% | 0.0% | 91.7% (11/12) | 100.0% | 567 |
| receipt-supermarket | 0.2% | 1.1% | 97.6% (41/42) | 100.0% | 1148 |
| receipt-takeaway | 0.0% | 0.0% | 100.0% (18/18) | 78.7% | 661 |
| sign-a-board | 8.2% | 9.5% | 100.0% (10/10) | 85.7% | 386 |
| sign-bakery | 0.0% | 0.0% | 100.0% (4/4) | 86.7% | 375 |
| sign-barber | 6.4% | 30.0% | 100.0% (3/3) | 80.0% | 343 |
| sign-butcher | 3.0% | 14.3% | 100.0% (3/3) | 71.4% | 390 |
| sign-chippy | 0.0% | 0.0% | 100.0% (3/3) | 76.9% | 332 |
| sign-door-closed | 7.5% | 25.0% | 100.0% (8/8) | 81.3% | 358 |
| sign-hardware | 10.2% | 42.9% | 100.0% (3/3) | 64.3% | 355 |
| sign-newsagent | 2.9% | 11.1% | 100.0% (4/4) | 66.7% | 406 |
| sign-pharmacy-hours | 5.0% | 24.2% | 100.0% (12/12) | 81.8% | 502 |
| sign-pub | 7.4% | 30.8% | 100.0% (3/3) | 76.9% | 362 |
