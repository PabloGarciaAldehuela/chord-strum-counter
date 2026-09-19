# 🎨 Google Stitch Prompts — Chord Strum Counter

This document contains tailored, copy-paste prompts designed for **Google Stitch** to generate all screens and overlays for **Chord Strum Counter** with 100% aesthetic consistency.

---

## 🏛️ Master Style Prefix (Include with Every Stitch Prompt)

> **Pro Tip for Stitch:** Prefix your screen prompts with this Global Style block, or use it when establishing the Project Design System in Stitch to ensure uniform colors, tonewood textures, and typography across all screens.

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Warm Acoustic Guitar Tonewood & Luthier Craftsmanship. Cozy, welcoming, tactile, acoustic studio atmosphere with subtle warm radial lighting (subtle sunburst guitar finish vignette).
Color Palette:
- Deep Canvas Background: Ebony & Dark Rosewood (#120E0D).
- Elevated Cards & Sheets: Rich Mahogany Veneer (#1E1211) with warm dark border (#2D1917).
- Primary Highlight & Accent: Glowing Golden Amber Varnish (#FFB300) with Honey highlights (#FFE082).
- Container Fills: Oiled Mahogany (#5C1D1A).
- Primary Text & Numbers: Crisp Polished Bone & Ivory (#FBF4EB).
- Secondary Text & Strings: Aged Spruce Cream (#D9C7B6) & Phosphor Bronze (#C4A482).
- Accents: Fiery Amber Crimson (#D32F2F) for stops/mutes, Vintage Emerald (#43A047) for Personal Best trophies.
UI Elements: Soft rounded corners (16px to 24px), full-pill interactive chips, high-legibility bold monospaced/tabular numerals for timers and counters, tactile elevation with warm ambient amber glow.
```

---

## 📱 Individual Screen Prompts for Google Stitch

---

### Prompt 1: Splash Screen

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Warm Acoustic Guitar Tonewood & Luthier Craftsmanship. Cozy, welcoming, tactile, acoustic studio atmosphere with subtle warm radial lighting.
Color Palette: Dark Rosewood (#120E0D), Rich Mahogany (#2D1917), Glowing Golden Amber (#FFB300), Ivory Bone (#FBF4EB).

[SCREEN DEFINITION]
Screen: Splash / Launch Screen
Layout: Minimalist, centered, high-atmosphere welcoming splash screen.
Elements:
1. Background: Deep rich mahogany and aged rosewood (#2D1B19 / #120E0D) with a warm subtle radial sunburst glow at the center.
2. Centerpiece Emblem:
   - A circular guitar soundhole emblem with acoustic guitar strings and golden rosette inlay rings around the perimeter.
   - Warm glowing ambient amber halo around the emblem.
   - Gentle rhythmic breathing scale feeling.
3. Typography:
   - Title below emblem: "Chord Strum Counter" in warm, elegant, bold ivory serif/display typography (#FBF4EB).
   - Subtitle: "Acoustic Practice Companion" in soft spruce cream (#D9C7B6, 70% opacity).
4. Bottom: Subtle minimal loading indicator in golden amber tone.
```

---

### Prompt 2: Practice / Counter Screen (Idle / Ready State)

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Warm Acoustic Guitar Tonewood & Luthier Craftsmanship. Cozy, welcoming, tactile.
Color Palette: Background (#120E0D), Card Surface (#1E1211 / #2D1917), Primary Accent (#FFB300), Text (#FBF4EB / #D9C7B6).

[SCREEN DEFINITION]
Screen: Main Practice & Counter Screen (Idle / Ready State)
Layout: Vertical scrollable mobile layout with high visual hierarchy.

Elements from Top to Bottom:
1. Top Action Bar:
   - Left: Rounded icon button with History trophy/clock icon (#FBF4EB).
   - Center: Subtitle "Chord Transitions" in warm muted cream (#D9C7B6).
   - Right: Rounded icon button with Settings gear icon (#FBF4EB).

2. Chord Progression Bar:
   - Elevated warm mahogany card (16px radius) across the screen width.
   - Displays active sequence chips: [ A ] ➔ [ D ] in deep mahogany pill containers (#5C1D1A) with golden amber text (#FFE082).
   - Right side: Small golden pencil edit icon ("Edit").

3. Central Practice Timer Ring & Counter:
   - Large 220px circular dual-track progress ring. Background track is dark wood (#2D1917); foreground active track is 100% full glowing golden amber (#FFB300) with rounded stroke caps.
   - Inside Ring:
     * Giant transition counter number: "0" in bold 68px ivory tabular font (#FBF4EB).
     * Label beneath: "transitions" in soft cream.

4. Duration Indicator:
   - Golden amber text: "⏱ Duration: 1 min".

5. Primary CTA Button:
   - Prominent pill-shaped "START PRACTICE 🎸" button (52px height, 65% width).
   - Bright golden amber fill (#FFB300), dark wood bold text (#120E0D), soft warm drop shadow.

6. Integrated Acoustic Metronome Card (Bottom):
   - Elevated rounded card (20px radius) in dark mahogany finish.
   - Header Row:
     * Left: Circular golden metronome/waveform icon badge + Title "Metronome" + Subtitle "80 BPM · Andante" in golden amber.
     * Right: High-contrast toggle switch (ON state).
   - Beat Visualizer:
     * 4-beat horizontal dot indicator (● ○ ○ ○). Beat 1 is larger (14px) and glowing gold (#FFB300); beats 2, 3, 4 are 11px soft dark dots.
   - Quick Stepper Controls:
     * Row with tactile buttons: [-5], [-1], large bold "80" BPM readout, [+1], [+5].
   - Smooth Tempo Slider:
     * Amber slider track from "40 (Largo)" to "240 (Presto)".
```

---

### Prompt 3: Practice / Counter Screen (Active / Listening State)

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Warm Acoustic Guitar Tonewood & Luthier Craftsmanship. Deep dark focus mode.
Color Palette: Dark Ebony (#120E0D), Mahogany (#1E1211), Active Gold (#FFB300), Ember Red (#D32F2F).

[SCREEN DEFINITION]
Screen: Main Practice Screen in Active Running Mode (Focus State)
Layout: Clean, distraction-free live practice view.

Elements from Top to Bottom:
1. Top Progression Strip:
   - Compact pill bar showing [ A ] ➔ [ D ] with small pulse dot indicating active session.

2. Central Live Practice Timer Ring:
   - 220px circular ring where the golden arc (#FFB300) has partially drained down counter-clockwise (representing 42 seconds remaining of 60s).
   - Inside Ring:
     * Giant dynamic transition counter: "37" in extra-bold 68px ivory text (#FBF4EB) with a subtle golden impact bounce glow.
     * Live time countdown below: "0:42" in crisp amber monospace font.

3. Live Acoustic Listening Indicator:
   - Centered below the ring:
   - Glowing microphone icon with expanding concentric acoustic soundwave ripples in golden amber.
   - Text: "🎸 Listening for strums…" in glowing semi-bold amber (#FFE082).

4. Stop Practice Button:
   - Outlined pill button with crimson/amber border and text "STOP SESSION" (#D32F2F) with subtle dark fill.

5. Active Metronome Bottom Card:
   - Synchronized metronome card showing active Beat 2 pulsing (○ ● ○ ○), "80 BPM · Andante".
```

---

### Prompt 4: Session Finished Overlay / Results Modal

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Celebratory Acoustic Tonewood & Luthier Craftsmanship. Cozy, rewarding victory screen.
Color Palette: Deep Translucent Scrim (#120E0D at 95% opacity), Glowing Amber (#FFB300), Emerald Pearl (#43A047), Ivory (#FBF4EB).

[SCREEN DEFINITION]
Screen: Session Finished / Results Modal Overlay
Layout: Full-screen modal overlay with dark warm frosted glass blur backdrop.

Modal Content (Centered Card):
1. Celebration Header:
   - Headline: "Time's up! 🎸" in 28px bold ivory typography.
   - Practiced Chord Progression: Badges showing [ A ] ➔ [ D ] in mahogany chips.

2. Massive Final Score:
   - Giant 88px ExtraBold golden amber number: "54" (#FFB300) with ambient golden backlight.
   - Subtitle: "chord transitions in 1 min" (Spruce cream #D9C7B6).

3. Personal Best Trophy Banner:
   - Prominent pill banner with emerald green and gold trim: "🏆 Personal Best!" (#43A047 background, gold trophy icon, white bold text).

4. Action Buttons:
   - Primary Button (Top): Full-width golden amber pill button "TRY AGAIN 🔁" (#FFB300 background, dark wood bold text).
   - Secondary Button (Bottom): Outlined warm rosewood button "VIEW HISTORY 📊" (Ivory border and text).
```

---

### Prompt 5: Chord Selection & Progression Builder Bottom Sheet

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Warm Acoustic Guitar Tonewood & Luthier Craftsmanship.
Color Palette: Sheet Surface (#1E1211), Border (#2D1917), Active Chip (#FFB300 on #5C1D1A), Text (#FBF4EB).

[SCREEN DEFINITION]
Screen: Chord Selection Bottom Sheet (Modal Drawer)
Layout: Bottom sheet sliding up from bottom with top drag handle (24px top rounded corners).

Elements from Top to Bottom:
1. Sheet Header:
   - Drag handle pill bar at top center.
   - Title: "🎸 Select Practice Chords" (20px bold ivory text).
   - Live Progression Preview: "Current: A ➔ D ➔ E" in golden amber font.

2. Quick Presets Section:
   - Section Title: "QUICK PRESETS" (Small bold caption).
   - Horizontal row of tonal preset buttons:
     * [ A ⇄ D ] (Highlighted)
     * [ A ⇄ D ⇄ E ]
     * [ C ⇄ G ⇄ Am ]
     * [ Em ⇄ Am ]

3. Complete Chord Library Grid:
   - Section Title: "CHORD LIBRARY (Tap to toggle, tap ℹ for fretboard diagram)".
   - Flow grid of interactive chord chips:
     * Selected state: [✓ A   ℹ], [✓ D   ℹ], [✓ E   ℹ] with golden amber fill (#FFB300) and dark bold text.
     * Unselected state: [ C   ℹ], [ G   ℹ], [ Am  ℹ], [ Em  ℹ], [ Dm  ℹ], [ Bm  ℹ], [ F   ℹ], [ A7  ℹ], [ C7  ℹ], [ D7  ℹ], [ E7  ℹ], [ G7  ℹ] in dark mahogany surface with subtle borders.
     * Each chip has a distinct circular info button ("ℹ") on its right edge.

4. Sticky Bottom CTA Button:
   - Full-width pill button: "CONFIRM SELECTION (3 CHORDS)" in golden amber with bold dark text.
```

---

### Prompt 6: Interactive Chord Diagram Viewer Bottom Sheet

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Authentic Luthier Fretboard Diagram on Rich Tonewood.
Color Palette: Surface (#1E1211), Fretboard Card (#2D1917), Nut/Frets (Silver/Bronze #C4A482), Dots (#FFB300), Mutes (#D32F2F).

[SCREEN DEFINITION]
Screen: Chord Diagram Viewer Bottom Sheet
Layout: Focused modal bottom sheet (24px top rounded corners) with centered fretboard graphic.

Elements:
1. Header:
   - Drag handle at top.
   - Large Chord Symbol: "Am" in 32px ExtraBold golden amber (#FFB300).
   - Chord Full Name: "A Minor (Open Position)" in spruce cream (#D9C7B6).

2. Guitar Fretboard Diagram Card:
   - Rounded wood card container (220px width × 240px height) with subtle dark rosewood grain texture.
   - Top Nut Bar: Thick horizontal bone/ivory bar (fret 0).
   - Top String Status Markers (above nut):
     * String 6 (low E): Red "✕" (Muted string, do not play).
     * String 5 (A): Gold hollow circle "⭕" (Open string).
     * String 1 (high e): Gold hollow circle "⭕" (Open string).
   - Fretboard Grid:
     * 4 horizontal metal frets in bronze/silver lines.
     * 6 vertical guitar strings with realistic gauge thickness (thick 6th string on left down to thin 1st string on right).
   - Finger Position Dots (on fretboard):
     * String 4 (D), Fret 2: Golden amber circle with finger number "2" inside.
     * String 3 (G), Fret 2: Golden amber circle with finger number "3" inside.
     * String 2 (B), Fret 1: Golden amber circle with finger number "1" inside.
   - Bottom String Labels:
     * Pitch names below each string: [ E ] [ A ] [ D ] [ G ] [ B ] [ e ].

3. Action Button:
   - Centered "CLOSE" pill button in soft mahogany tone.
```

---

### Prompt 7: Practice Settings & Audio Calibration Bottom Sheet

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Warm Acoustic Guitar Tonewood & Luthier Craftsmanship. Precision audio calibration.
Color Palette: Surface (#1E1211), Track (#2D1917), Active Slider (#FFB300), Text (#FBF4EB).

[SCREEN DEFINITION]
Screen: Settings & Audio Calibration Bottom Sheet
Layout: Clean, spaced setting controls in a dark rosewood modal sheet.

Sections from Top to Bottom:
1. Section 1 — Practice Duration:
   - Header: "⏱ Practice Duration" in bold ivory text.
   - Preset buttons row: [ 30s ] [ 1 min (Active Gold) ] [ 90s ] [ 2 min ].
   - Custom Duration Slider: Golden slider track showing live label "Custom: 1 min 00s" with range labels "15s" to "5 min".

2. Section 2 — Microphone Sensitivity:
   - Header: "🎙 Mic Sensitivity" in bold ivory text.
   - Dynamic Level Subtitle: "Medium — normal acoustic strumming" in spruce cream.
   - Sensitivity Slider: Continuous amber slider with endpoints labeled "Loud only" on left and "Very quiet" on right.

3. Section 3 — Strum Debounce Gap (Double-count protection):
   - Header: "⚡ Minimum Gap Between Strums" in bold ivory text.
   - Live Value Readout: "350 ms" in bold golden amber.
   - Slider: Amber slider track with endpoints "100ms (fast strumming)" to "800ms (slow changes)".
   - Helper note: "Prevents duplicate counts on pick scrape and acoustic reverb."
```

---

### Prompt 8: Session History & Personal Bests Screen

```text
[GLOBAL STYLE & THEME GUIDELINE]
App: Chord Strum Counter (Android Mobile App, Portrait Orientation).
Aesthetic: Warm Acoustic Guitar Tonewood & Luthier Craftsmanship.
Color Palette: Canvas (#120E0D), Normal Card (#1E1211), Best Card (#5C1D1A border with Gold glow), Primary (#FFB300), Trophy (#43A047).

[SCREEN DEFINITION]
Screen: Session History & Statistics Screen
Layout: Full-screen scrollable history list with summary stats bar.

Elements from Top to Bottom:
1. Top App Bar:
   - Left: Back arrow icon button (←).
   - Title: "Session History" in 20px bold ivory font.

2. Summary Statistics Card (Stats Bar):
   - Elevated mahogany banner with 3 equal columns:
     * Col 1: 🏆 Emoji + Giant "62" + Label "Best".
     * Col 2: 📊 Emoji + Giant "44" + Label "Average".
     * Col 3: 🎸 Emoji + Giant "28" + Label "Sessions".
   - Numbers rendered in bold golden amber (#FFB300).

3. Section Divider: Subtle phosphor bronze horizontal rule.

4. Session Cards List (Vertical List):
   - Card 1 (Personal Best Highlighted Card):
     * Elevated dark mahogany card with golden amber glow border.
     * Left: Chord progression chips [ A ] ➔ [ D ] + Date "30 Aug 2026, 14:15" + Duration "1 min".
     * Right: Large "62" transition score + Shiny "🏆 Best" green/gold pill badge.
   - Card 2 (Standard Session Card):
     * Standard dark rosewood card.
     * Left: Progression chips [ C ] ➔ [ G ] ➔ [ Am ] + Date "29 Aug 2026, 19:30" + Duration "1 min".
     * Right: "48" transition score.
   - Card 3 (Standard Session Card):
     * Left: Progression chips [ Em ] ➔ [ Am ] + Date "28 Aug 2026, 10:10" + Duration "45s".
     * Right: "39" transition score.

5. Alternate Empty State (if no sessions):
   - Centered warm acoustic guitar resting on a wooden stand illustration.
   - Title: "No sessions yet".
   - Subtitle: "Complete your first practice session to see your progress and personal best records here."
```

---

## 💡 Best Practices for Generating with Google Stitch

1. **Use Mobile Viewport (Portrait):** Set the device target to **Mobile (Phone)** in Stitch.
2. **Apply the Master Style First:** Paste the `[GLOBAL STYLE & THEME GUIDELINE]` in your initial Stitch design system configuration or at the start of each prompt.
3. **Generate Variants:** For complex interactive components like the **Fretboard Diagram** or **Metronome Card**, use Stitch's variant generation feature to explore subtle variations in wood grain intensity and amber glow.
4. **Exporting to Jetpack Compose:** All color tokens and component layouts map 1-to-1 with the app's Compose theme located at `dev.pablocoding.contadorderasgueosdeacordes.ui.theme`.
