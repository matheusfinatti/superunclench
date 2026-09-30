# Design sign-off: US-01 App shell, Home skeleton & theming

- **Reviewer:** Designer · **Date:** 2026-09-30
- **Inputs:** `docs/design/design-system.md`, `docs/design/US-01-app-shell.md`, QA report and screenshots 01–07 in this folder (plus `../US-02/05`, `../US-02/15`), and the Compose code (`HomeScreen.kt`, `InfoBanner.kt`, `SuperUnclenchApp.kt`, `SettingsScreen.kt`, `StatsScreen.kt`, `res/drawable/`).

## Verdict: **ACCEPT**

The shell matches the design. Nothing here materially hurts usability or accessibility, or breaks the design system.

**What matches the spec**
- Brand scheme with dynamic color off. The light and dark token values QA sampled match §2.2/§2.3 of the design system.
- Home layout: slot order, 16dp gutter, 12dp card gap, `large` (20dp) cards on `surfaceContainer`, neutral `InfoBanner` on `surfaceContainerHigh` with `medium` corners, and a 56dp pill Start CTA with a leading play icon.
- Type roles: `headlineMedium` greeting, `bodyLarge` `onSurfaceVariant` subtitle, `labelMedium` uppercase overline, `titleLarge` placeholder.
- Copy is exact. "Level 1 · 0/3" is on screen and "Level 1, 0 of 3" is in the card's contentDescription.
- Accessibility: the title and the greeting are headings. The banner is a merged node with "Got it" as a separate 48dp target. The level card is merged. Nav labels are always shown and test tags are present.
- Motion: the disclaimer collapses with shrink + fade over 300ms, and tabs crossfade over 150ms.
- Stats placeholder: the empty state matches §2.2. Settings placeholder: the disabled rows match §2.3.
- Deferring the Today tiles to US-08 is allowed by the spec.

## Must-fix
None.

## Follow-ups (fix alongside the next story)

1. **Stats nav icon doesn't change when selected.** `ic_stats.xml` and `ic_stats_filled.xml` contain the same path. This is not an export mistake: Material Symbols `bar_chart` is solid bars, so its Fill 0 and Fill 1 versions are identical. Selection is still readable from the indicator pill and the label, so this isn't a blocker. For a consistent outlined-to-filled change across the three tabs, switch the Stats destination to **`insert_chart`**, which I'm adding to design-system §7 as the Nav icon for Stats. Keep `bar_chart` as the large glyph in the Stats empty state if you like. Path data below uses the Android-exported 960 viewport (y already shifted to 0..960), the same format as the current `ic_home` and `ic_stats`. I rasterised it to check it.

   `ic_stats.xml` (insert_chart, Fill 0):
   ```xml
   <vector xmlns:android="http://schemas.android.com/apk/res/android"
       android:width="24dp" android:height="24dp"
       android:viewportWidth="960" android:viewportHeight="960">
       <path android:fillColor="@android:color/white" android:fillType="evenOdd"
           android:pathData="M280,680h80v-280h-80v280Zm160,0h80v-400h-80v400Zm160,0h80v-160h-80v160ZM200,840q-33,0 -56.5,-23.5T120,760v-560q0,-33 23.5,-56.5T200,120h560q33,0 56.5,23.5T840,200v560q0,33 -23.5,56.5T760,840H200Zm0,-80h560v-560H200v560Z" />
   </vector>
   ```
   `ic_stats_filled.xml` (insert_chart, Fill 1, with the bars knocked out):
   ```xml
   <vector xmlns:android="http://schemas.android.com/apk/res/android"
       android:width="24dp" android:height="24dp"
       android:viewportWidth="960" android:viewportHeight="960">
       <path android:fillColor="@android:color/white" android:fillType="evenOdd"
           android:pathData="M280,680h80v-280h-80v280Zm160,0h80v-400h-80v400Zm160,0h80v-160h-80v160ZM200,840q-33,0 -56.5,-23.5T120,760v-560q0,-33 23.5,-56.5T200,120h560q33,0 56.5,23.5T840,200v560q0,33 -23.5,56.5T760,840H200Z" />
   </vector>
   ```
   (This is Outlined-style geometry with square bar ends, matching the current `ic_home`. When the full Rounded set is exported (item 3), use the Rounded `insert_chart` from fonts.google.com instead.)

2. **Disabled Settings placeholder rows are faint**, especially in dark mode. `onSurface` at 38% gives about 2.9:1 in dark and 2.5:1 in light. WCAG exempts disabled controls and US-09 replaces these rows, so this is not a blocker. If a placeholder row is still shipping when US-03 lands, add supporting text "Coming soon" in `onSurfaceVariant` so the row can still be read.

3. **Icon set is mixed.** `ic_home`, `ic_home_filled` and `ic_stats` come from Material Symbols (960 viewport). `ic_info`, `ic_person`, `ic_palette`, `ic_play`, `ic_settings`, `ic_settings_filled`, `ic_back`, `ic_chevron_right`, and the debug `ic_bug` and `ic_expand_more` are legacy Material Icons (24 viewport). Some of those are filled where design-system §7 asks for Fill 0 outlined: `ic_palette` and `ic_bug` render solid in 03/06. There are two visible effects:
   - The Settings rows mix outlined and filled glyphs.
   - `ic_settings` and `ic_settings_filled` only differ because the legacy "outlined" gear has a hole. Their weight doesn't match the Symbols home icon.

   Fix: re-export every icon in the §7 table from fonts.google.com/icons as **Material Symbols Rounded, weight 400, grade 0, optical size 24, Fill 0** (Fill 1 for the `_filled` nav variants and for play, pause and stop), using the "Android" download. Do this for the icons you touch in US-03, and for the Settings rows in US-09 at the latest. I'm not supplying hand-typed Rounded path data for these because the official export is exact and a typed copy risks errors. Keep `android:autoMirrored="true"` on back and the chevrons.

4. **Template drawables left in.** Delete `ic_favorite.xml` and `ic_account_box.xml` (US-01 spec §7). Nothing references them.

5. **Play icon in Start** reads slightly small at 18dp, because the legacy `M8,5v14l11,-7z` triangle has a lot of padding. Re-exporting Symbols Rounded `play_arrow` Fill 1 (item 3) fixes this. Keep the 18dp size.

## Not reviewed (no evidence yet)
- Medium and expanded layouts (rail, 600dp max width, 24dp gutter).
- 200% font scale (a preview exists but no screenshot).
- Launch without a white flash.

Please attach screenshots of these to the US-03 QA run.
