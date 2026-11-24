# Deaftly style guide

How Deaftly looks and sounds, in the app, on deaftly.com and in anything else we publish. It records the choices already in use. It isn't a redesign. Values come from the code, and the code wins if the two ever disagree: fix whichever is wrong in the same change.

Sources: the app's `ui/theme/` (`DeaftlyColors.kt`, `Type.kt`, `DeaftlyShapes.kt`, `DeaftlySpacing.kt`, `ShowColours.kt`, `ParticipantColours.kt`) and the website's `static/styles/site.css`.

## Logo

The logo is a white lower-case **d** followed by a lavender dot, on Deaftly purple. The master artwork is [`deaftly-logo.svg`](deaftly-logo.svg), 2048 px square.

| Part | Colour |
|---|---|
| Background | Accent `#6A4596` |
| The **d** | White `#FFFFFF`. Its counter, the bowl's inside, is cut out, so the background shows through |
| The dot | Lavender `#C7A8E6`, the dark-theme accent |

Contrast: white on the accent is 7.2:1. The dot is 3.5:1, which is enough for a graphic element but not for text.

### Where each version goes

| Use | Version |
|---|---|
| Android launcher icon | Adaptive icon: the accent background layer, the white **d** and lavender dot as the foreground (`ic_launcher_foreground.xml`) |
| Android themed icon | Monochrome layer (`ic_launcher_monochrome.xml`): the **d** and the dot as one shape, with the counter as a hole. Android tints it |
| Website favicon and Apple touch icon | The full-colour square: `favicon.png` and `favicon.ico`, and the 180 px `apple-touch-icon.png` |
| Profile pictures | The full-colour square. Keep the mark inside the `profile-pic-and-favicon` guide circle, which is 70% of the width, so a circular crop doesn't clip it |

### Safe zone and clear space

The master file holds Android's adaptive-icon guides as layers. As fractions of the square's width:
- **Visible area:** a circle 66.7% wide (`android-adaptive-max`, 72 of 108 dp).
- **Guaranteed safe zone:** a circle 61.1% wide (`android-adaptive-guaranteed-min`, 66 of 108 dp).
- **The mark itself:** sits inside a circle 52.6% wide (`android-adaptive-logo-guide`), which leaves room on every launcher shape.

Proposed, not yet used anywhere: away from the square, leave clear space around the mark of at least the dot's diameter on every side.

### Minimum size

The smallest size shipped is 16 px, in `favicon.ico`, which also holds 32 and 48 px. Proposed: below 24 px, always use the full-colour square rather than the mark on its own, so the dot stays visible.

### Don't

- Recolour the **d**, the dot or the background, except as Android does with the monochrome icon.
- Fill the counter, or put the mark on a background other than the accent.
- Stretch, rotate, outline or add effects.

## Colour

Light and dark themes follow the system setting unless the user picks one in Settings. Both repos use the same values for the shared tokens.

### Core tokens

| Token | Light | Dark | What it's for |
|---|---|---|---|
| `ground` | `#F7F6F2` | `#14141A` | Page and screen background |
| `surface` | `#FFFFFF` | `#1E1E26` | Cards, sheets and fields |
| `surfaceSunken` | `#ECEAE4` | `#2A2A34` | Wells and pressed areas |
| `ink` | `#15151A` | `#F1F0EC` | Main text |
| `inkSecondary` | `#45454F` | `#CFCFD6` | Supporting text |
| `inkMuted` | `#5C5C66` | `#A9A9B3` | Hints, captions and section labels |
| `line` | `#E4E2DC` | `#2E2E38` | Hairline dividers |
| `lineStrong` | `#D6D3CB` | `#40404C` | Stronger dividers |
| `outline` | `#85837C` | `#7E7E8C` | Field and control borders |
| `accent` | `#6A4596` | `#C7A8E6` | Primary actions, links, selection and the brand |
| `accentStrong` | `#4E2F75` | `#E0CCF4` | Pressed or emphasised accent |
| `accentSoft` | `#EFE7F7` | `#33264A` | Tinted panels, such as the Listen for me sign |
| `onAccent` | `#FFFFFF` | `#1E1030` | Text and icons on `accent` |
| `highlight` | `#FFE15C` | `#FFE15C` | The highlight mark |
| `onHighlight` | `#15151A` | `#15151A` | Text on `highlight` |

The app also has tokens for the second person in a chat (`personB`, a warm peach and orange family) and for errors (Material's red family). They're in `DeaftlyColors.kt`, and the website doesn't use them.

### Contrast

All text pairs meet WCAG 2.2 AA (4.5:1), and controls meet 3:1. `ThemeContrastTest` checks them in the app.

| Pair | Light | Dark |
|---|---|---|
| `ink` on `ground` | 16.8:1 | 16.1:1 |
| `inkSecondary` on `ground` | 8.8:1 | 11.8:1 |
| `inkMuted` on `ground` | 6.1:1 | 7.9:1 |
| `accent` on `ground` (links) | 6.7:1 | 8.9:1 |
| `onAccent` on `accent` (buttons) | 7.2:1 | 8.7:1 |
| `onHighlight` on `highlight` | 14.0:1 | 14.0:1 |
| `outline` on `ground` (control borders) | 3.5:1 | 4.6:1 |

### Show colours

Show offers three high-contrast schemes. The user cycles them on the Show screen and sets the starting one in Settings.

| Scheme | Background | Text | Muted | Text contrast |
|---|---|---|---|---|
| Black on white | `#FFFFFF` | `#000000` | `#55555E` | 21:1 |
| White on black | `#000000` | `#FFFFFF` | `#A9A9B3` | 21:1 |
| Yellow on black | `#000000` | `#FFE15C` | `#BDB27A` | 16.2:1 |

### Chat colours

Each person in a side by side chat picks a colour. Their messages carry it as a coloured rule and label, so the conversation reads like a script.

| Colour | Swatch | Text on it |
|---|---|---|
| Red | `#CD001A` | White |
| Orange | `#EF6A00` | `#15151A` |
| Gold | `#F2CD00` | `#15151A` |
| Green | `#79C300` | `#15151A` |
| Blue | `#1961AE` | White |
| Indigo | `#61007D` | White |

Labels use a darker or lighter version of each colour to stay readable in light and dark themes. See `ParticipantColours.kt`.

## Typography

Two families, both variable fonts bundled in the app and served from deaftly.com:
- **Atkinson Hyperlegible Next** for everything people read: body text, labels, buttons and messages. It was designed for legibility by the Braille Institute. Weights 400, 500 and 700.
- **Bricolage Grotesque** for headings and the wordmark only. Weights 700 and 800, at an optical size of 24 in the app.

### App type scale

Sizes are in sp, so they follow the system font size.

| Style | Family | Size / line height | Weight | Letter spacing |
|---|---|---|---|---|
| `displayLarge` | Bricolage | 57 / 64 | 800 | -0.57 |
| `displayMedium` | Bricolage | 46 / 53 | 800 | -0.46 |
| `displaySmall` | Atkinson | 26 / 40 | 500 | |
| `headlineLarge` | Bricolage | 30 / 34 | 800 | -0.9 |
| `headlineMedium` | Bricolage | 26 / 32 | 700 | |
| `headlineSmall` | Bricolage | 22 / 28 | 700 | |
| `titleLarge` | Bricolage | 20 / 26 | 700 | |
| `titleMedium` | Atkinson | 18 / 24 | 700 | |
| `titleSmall` | Atkinson | 17 / 24 | 700 | |
| `bodyLarge` | Atkinson | 19 / 27 | 400 | |
| `bodyMedium` | Atkinson | 17 / 24 | 400 | |
| `bodySmall` | Atkinson | 15 / 22 | 400 | |
| `labelLarge` | Atkinson | 17 / 22 | 700 | |
| `labelMedium` | Atkinson | 14 / 18 | 700 | 0.84 |
| `labelSmall` | Atkinson | 13 / 16 | 700 | 0.52 |

Two sizes sit outside the scale, because they're shown to someone else:
- **Show text:** Atkinson 500, Small 36, Medium 46 or Large 56 sp, chosen with the volume keys. Each size is capped at 1.5 times itself at large system font sizes, and long words hyphenate.
- **Chat messages:** Atkinson, Small 18, Medium 20 or Large 22 sp, chosen in the chat.

### Website

Body text is Atkinson at 1.1875rem (19 px), matching the app's `bodyLarge`. Headings are Bricolage and scale with the viewport. The page title is `clamp(2.5rem, 6vw, 4.5rem)`.

## Shape

Corners are rounded, and larger things have rounder corners.

| Shape | Radius | Used for |
|---|---|---|
| `chip` | 8 dp | Placeholder chips |
| `pill` | Fully round | Category chips and toggles |
| `controlSmall` | 14 dp | Small controls |
| `control` | 16 dp | Text fields |
| `cardSmall` | 18 dp | List rows |
| `button` | 20 dp | Buttons |
| `card` | 22 dp | Cards and hub tiles |
| `cardLarge` | 24 dp | Large panels |
| `sheet` | 28 dp, top corners only | Bottom sheets |

The website uses 28 px (`--radius`) for large panels and 18 px (`--radius-small`) for smaller ones.

## Spacing

The app spaces things in steps of 4, 8, 10, 12, 14, 16, 20, 22 and 24 dp (`DeaftlySpacing.kt`). Screen edges use 16 or 20 dp. Touch targets are at least 48 dp.

## Copy and voice

- **British English.** For example colour, organise, centre.
- **No en dashes or em dashes** in anything a user reads. Rewrite the sentence, or use a full stop, comma, colon or brackets.
- **Titles** separate parts with " | ", page first, as in "Privacy | Deaftly".
- **Sentence case** for headings, buttons and labels.
- **Say "free" plainly.** Deaftly is free on principle.
- **Say "device", not "phone",** unless something is only true of phones. Words said to a hearing person avoid "device" and use "here" instead, as in "please type here".
- **Plain, direct and kind.** Name things by what people do, not how the app works. Errors say what happened and what to do, without apologising.
- **Example names** mix English, Arabic and Afro-Caribbean names across genders. Never default to white names, and never use a real person's name.
- **Don't promise "no internet".** It isn't a design rule, and it would stop being true the moment a feature needs a connection.

## Known differences

- `deaftly-logo.svg`: the `standard` guide layer uses `#6A4696` where the accent is `#6A4596`. It's a guide layer, so no exported icon is affected. Correct it the next time the master is edited.
