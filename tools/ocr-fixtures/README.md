# OCR fixtures

A set of 37 synthetic phone-camera frames with ground truth, used to compare OCR engines (ML Kit and PaddleOCR) on the same input. The images are generated, not photographed: each one is a flat designed scene (a menu, shop sign, receipt or product label) pushed through a simulated camera.

| Category | Count | Examples |
|---|---|---|
| `menu` | 12 | two-column menu with dotted leaders, chalkboard, kraft paper, low light |
| `sign` | 10 | bakery, butcher, barber (glow), pharmacy opening hours, A-board |
| `receipt` | 8 | supermarket, café, petrol, parking ticket, low-light return slip |
| `label` | 7 | cereal nutrition table, medicine dosage, soup tin, shampoo ingredients |

The committed output lives in `app/src/androidTest/assets/ocr-fixtures/`, one `<id>.jpg` and one `<id>.json` per fixture.

## Regenerating

```
java tools/ocr-fixtures/Generate.java app/src/androidTest/assets/ocr-fixtures
```

Needs JDK 21 and nothing else (Java AWT and ImageIO only). Run it from anywhere inside the repository. A second argument writes debug overlays (ground truth drawn over each image as PNGs) to that directory; do not commit those.

Output is deterministic: each fixture has a fixed seed derived from its position in the list. The same JDK build produces identical files. A different JDK or platform may rasterise glyphs slightly differently, so JPEG bytes can change while the ground truth stays valid, because it is computed from the layout and the camera transform, not measured from the pixels.

The generator fails loudly if a font cannot draw a character, a polygon falls outside its image, words do not rebuild their line, there are fewer than 30 fixtures or the set exceeds 15 MB. `OcrFixtureSetTest` in `:core` checks the committed files on every build: each image has a JSON file and vice versa, every JSON parses, dimensions match the image, polygons lie inside the image and boxes match polygons.

## Ground truth format

```json
{
  "schema": 1,
  "image": "receipt-cafe.jpg",
  "width": 960,
  "height": 1280,
  "category": "receipt",
  "tags": ["short", "monospace", "rotated"],
  "fonts": "Courier Prime",
  "conditions": {"rotationDegrees": -9.0, "perspectiveYaw": -0.05, "perspectivePitch": 0.03,
                 "blurSigma": 0.9, "noiseSigma": 2.6, "jpegQuality": 85, "lighting": "gradient"},
  "lines": [
    {"text": "THE DAILY GRIND", "block": 0,
     "polygon": [[211.4,367.9],[630.3,286.2],[635.1,319.2],[216.9,399.0]],
     "box": [211.4, 286.2, 635.1, 399.0],
     "words": [{"text": "THE", "polygon": [[...]], "box": [...]}]}
  ]
}
```

- Coordinates are pixels in the output image, origin top left, x right, y down.
- `polygon` has four points in the order top-left, top-right, bottom-right, bottom-left of the text as written, so a rotated line has a rotated quadrilateral (the shape PP-OCR returns). `box` is `[left, top, right, bottom]`, the axis-aligned bounds of the polygon (the shape ML Kit returns).
- Boxes are tight to the drawn glyphs (visual bounds, plus one pixel), so a line with a descender is taller than one without.
- `lines` are in reading order: header first, then each column top to bottom (left column before right), then the footer. Within a row, the left text comes before the right text.
- Text separated by a wide gap on the same row (a dish and its price, a receipt item and its amount, a table cell) is stored as separate lines, because detectors split them. Wrapped paragraphs are stored one line per printed line.
- `block` numbers sections: the header is block 0 and each heading starts a new block.
- `words` split the line on spaces; each has its own polygon and box.
- Not in the ground truth: decorative dotted leaders, rules, borders and the background. Hyphens, bullets (`•`) and middle dots (`·`) are text and are included.
- `conditions` records what was applied: in-plane rotation, perspective strength (positive yaw shrinks the right side, positive pitch the bottom), Gaussian blur sigma in pixels, sensor noise sigma in 8-bit levels, JPEG quality and a lighting label (`even`, `gradient`, `shadow`, `gradient+shadow`, `low-light`).

## What is simulated

Scenes are drawn flat, then warped with a homography (in-plane rotation up to about 13 degrees plus perspective tilt) and sampled over a perspective-correct surface (wood, brick, pavement, plaster, dark cloth). The camera model adds Gaussian blur, a brightness gradient and vignette, optional soft shadow bands, low-light gain with a black level and colour cast, signal-dependent sensor noise and JPEG compression (quality 85 for most, 70 for two). Chalk text, faded thermal ink and neon glow are applied to the text layer.

Frames are 1280x960 or 960x1280, a typical analysis-frame size.

## What is not captured

Specular glare and reflections off glossy paper or glass, real paper and print texture, curved or crumpled surfaces (tins, bottles, folded receipts), motion blur from hand shake, rolling shutter, lens distortion, depth of field, occlusion by fingers or objects, real handwriting, and the colour and noise behaviour of a particular sensor and ISP. Backgrounds are procedural and look it. Treat results as a controlled comparison between engines, not as a prediction of field accuracy.

## Licence

The images and ground truth are generated by this project from text written for it; every business name, address, phone number (Ofcom drama ranges), product and price is invented. They carry the repository's licence. The fonts are licensed under the SIL Open Font License 1.1, and each family's licence text is in `fonts/<family>/OFL.txt`.

| Family | Source | Used for |
|---|---|---|
| Atkinson Hyperlegible Next | `app/src/main/res/font/` | sans body text, medicine and cereal labels |
| Bricolage Grotesque | `app/src/main/res/font/` | display sans, curry-house menu, soup tin |
| Old Standard TT | `fonts/old-standard-tt/` | serif menus, pub and bakery signs |
| Barlow Condensed | `fonts/barlow-condensed/` | condensed sans, chippy and hardware signs |
| Kalam | `fonts/kalam/` | handwriting-style chalkboards |
| Courier Prime | `fonts/courier-prime/` | receipts and tickets |

The three downloaded families come from the `ofl/` folder of the google/fonts repository. The two app fonts are variable fonts; Java renders their default instance and synthesises bold, so their bold text is slightly heavier than the designer's bold.
