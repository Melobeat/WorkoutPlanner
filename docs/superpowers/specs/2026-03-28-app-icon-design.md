# App Icon Design

**Date:** 2026-03-28
**Status:** Approved

## Overview

Design and implement a new launcher icon for the WorkoutPlanner app (`de.melobeat.workoutplanner`), replacing the default Android Studio placeholder.

## Design Decisions

| Dimension | Decision | Rationale |
|---|---|---|
| Mood | Bold & Expressive | Stands out in launcher grid; strong personality |
| Symbol | Dumbbell | Universal fitness symbol; instantly legible at all sizes |
| Treatment | Outlined / Line Art | Clean, modern; works at 48px and 512px equally well |
| Palette | Deep Purple + Acid Green | Distinctive in launcher; echoes the app's existing purple theme |

## Visual Spec

### Colors

- **Background gradient:** `#1e003e` → `#3d0070`, top-left to bottom-right (135°)
- **Foreground stroke:** `#c8ff00` (acid green), stroke-width 3.5px on a 108×108 canvas
- **Monochrome foreground:** white outline on mid-grey background (for Android 13+ themed icons)

### Dumbbell geometry (108×108 canvas)

| Element | x | y | width | height | rx |
|---|---|---|---|---|---|
| Left weight plate | 12 | 44 | 18 | 20 | 5 |
| Left collar | 30 | 36 | 9 | 36 | 3 |
| Bar | 39 | 48 | 30 | 12 | 3 |
| Right collar | 69 | 36 | 9 | 36 | 3 |
| Right weight plate | 78 | 44 | 18 | 20 | 5 |

All elements are `fill="none"` (outlined only), `stroke="#c8ff00"`, `stroke-width="3.5"`.

### Adaptive icon shape

- Corner radius: 24px (squircle) for the background layer
- Round variant: corner radius 54px (full circle) — same foreground, circular background

## Android Adaptive Icon Structure

Android adaptive icons consist of three layers:

```
res/
  drawable/
    ic_launcher_background.xml   ← gradient background (108×108 safe zone, 72×72 core)
    ic_launcher_foreground.xml   ← dumbbell SVG vector, centered
  mipmap-anydpi/
    ic_launcher.xml              ← references background + foreground layers
    ic_launcher_round.xml        ← same layers (system clips to circle)
  mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
    ic_launcher.webp             ← rasterised fallback (legacy devices)
    ic_launcher_round.webp       ← rasterised round fallback
```

The foreground drawable must be 108dp×108dp with the icon artwork centered in the inner 72dp×72dp safe zone (18dp padding on all sides), so the system can animate/parallax the layers without clipping the dumbbell.

## Implementation Notes

- Implement as Android Vector Drawable (`<vector>`) for the foreground and `<shape>` + `<gradient>` for the background — no raster assets needed for mdpi and above with adaptive icons.
- The rasterised WebP fallbacks (for `mipmap-*` folders) should be re-exported from the vector at the correct densities: mdpi=48px, hdpi=72px, xhdpi=96px, xxhdpi=144px, xxxhdpi=192px.
- The `<monochrome>` layer in `mipmap-anydpi/ic_launcher.xml` reuses `ic_launcher_foreground` — Android 13+ tints it automatically based on the user's wallpaper color. No separate monochrome asset is needed.
- Existing `ic_launcher_background.xml` and `ic_launcher_foreground.xml` in `res/drawable/` are overwritten.
- Existing WebP files in `mipmap-*` folders are replaced.

## Out of Scope

- Notification icon (uses a separate, single-color small icon)
- Splash screen / branding assets
- Any change to in-app colors or theme