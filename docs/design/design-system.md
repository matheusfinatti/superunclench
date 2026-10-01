# SuperUnclench — Design System (v1)

Owner: Designer · Last updated: 2026-09-30 · Status: **Approved for US-01..US-10**

This file is the **single source of truth** for visual tokens. Story docs (`US-XX-*.md`) reference tokens by name only (e.g. `colorScheme.primaryContainer`, `Good.container`, `space.md`). If a story doc and this file disagree, this file wins.

---

## 1. Brand direction

**Personality:** calm, modern, friendly — a polished habit app (think Headspace-meets-Streaks, but quieter). It should feel like a deep breath, not a dashboard.

| Do | Don't |
|----|-------|
| Soft dusk-blue brand, lots of surface, rounded shapes | Neon, saturated gradients, alarm-red UI chrome |
| Encouraging, short copy ("Nice — keep it loose") | Clinical / medical words (treat, cure, therapy, symptom, diagnose) |
| Numbers big and legible (level, countdown) | Dense tables, tiny labels |
| Red only for the **Bad** answer and destructive actions | Red for warnings/banners (use neutral/tertiary containers) |
| One primary action per screen | Competing filled buttons |

**Why dusk-blue and not teal/green?** Green is reserved for **Good**. A blue brand keeps Good (green) and Bad (red) unmistakable and never confused with "primary".

**Voice:** second person, sentence case, no exclamation marks except "Level up!". No emoji anywhere (app or notification).

---

## 2. Color

### 2.1 Theming policy
- **Brand scheme is the default** (`dynamicColor = false`). The template's `dynamicColor = true` must be changed.
- Dynamic color is **not** offered in v1 (keeps screenshots deterministic for QA and keeps Good/Bad harmonised with the brand). Possible v1.x Settings toggle "Use wallpaper colors" (Android 12+) — Good/Bad/Missed semantic colors stay fixed even then.
- Theme mode: **System** (default) / **Light** / **Dark**, chosen in Settings (US-09) and cycled by the QA panel (US-02).
- Edge-to-edge is on; status/navigation bar icons must follow the *app* theme (not only the system) — set `isAppearanceLightStatusBars = !dark` when the user forces a mode.
- XML theme (`themes.xml`): parent `android:Theme.Material.Light.NoActionBar` is fine; add `values-night/themes.xml` with `android:Theme.Material.NoActionBar` and set `android:windowBackground` to the `surface` value so the launch frame doesn't flash white in dark mode.

### 2.2 Light scheme (Material 3 roles)

| Role | Hex | | Role | Hex |
|------|-----|-|------|-----|
| primary | `#3D5F90` | | onPrimary | `#FFFFFF` |
| primaryContainer | `#D5E3FF` | | onPrimaryContainer | `#244776` |
| secondary | `#555F71` | | onSecondary | `#FFFFFF` |
| secondaryContainer | `#D9E3F8` | | onSecondaryContainer | `#3E4758` |
| tertiary | `#6E5676` | | onTertiary | `#FFFFFF` |
| tertiaryContainer | `#F7D8FF` | | onTertiaryContainer | `#553F5D` |
| error | `#BA1A1A` | | onError | `#FFFFFF` |
| errorContainer | `#FFDAD6` | | onErrorContainer | `#93000A` |
| background | `#F9F9FF` | | onBackground | `#191C20` |
| surface | `#F9F9FF` | | onSurface | `#191C20` |
| surfaceVariant | `#DFE2EB` | | onSurfaceVariant | `#43474E` |
| surfaceDim | `#D9D9E0` | | surfaceBright | `#F9F9FF` |
| surfaceContainerLowest | `#FFFFFF` | | surfaceContainerLow | `#F3F3FA` |
| surfaceContainer | `#EDEDF4` | | surfaceContainerHigh | `#E7E8EE` |
| surfaceContainerHighest | `#E2E2E9` | | surfaceTint | `#3D5F90` |
| outline | `#73777F` | | outlineVariant | `#C3C6CF` |
| inverseSurface | `#2E3035` | | inverseOnSurface | `#F0F0F7` |
| inversePrimary | `#A7C8FF` | | scrim | `#000000` |

### 2.3 Dark scheme (Material 3 roles)

| Role | Hex | | Role | Hex |
|------|-----|-|------|-----|
| primary | `#A7C8FF` | | onPrimary | `#07305F` |
| primaryContainer | `#244776` | | onPrimaryContainer | `#D5E3FF` |
| secondary | `#BDC7DC` | | onSecondary | `#273141` |
| secondaryContainer | `#3E4758` | | onSecondaryContainer | `#D9E3F8` |
| tertiary | `#DABDE2` | | onTertiary | `#3D2946` |
| tertiaryContainer | `#553F5D` | | onTertiaryContainer | `#F7D8FF` |
| error | `#FFB4AB` | | onError | `#690005` |
| errorContainer | `#93000A` | | onErrorContainer | `#FFDAD6` |
| background | `#111318` | | onBackground | `#E2E2E9` |
| surface | `#111318` | | onSurface | `#E2E2E9` |
| surfaceVariant | `#43474E` | | onSurfaceVariant | `#C3C6CF` |
| surfaceDim | `#111318` | | surfaceBright | `#37393E` |
| surfaceContainerLowest | `#0C0E13` | | surfaceContainerLow | `#191C20` |
| surfaceContainer | `#1D2024` | | surfaceContainerHigh | `#282A2F` |
| surfaceContainerHighest | `#33353A` | | surfaceTint | `#A7C8FF` |
| outline | `#8D9199` | | outlineVariant | `#43474E` |
| inverseSurface | `#E2E2E9` | | inverseOnSurface | `#2E3035` |
| inversePrimary | `#3D5F90` | | scrim | `#000000` |

### 2.4 Semantic colors (not part of M3 `ColorScheme`)
Expose as an `ExtendedColors` data class via a `staticCompositionLocalOf` (`LocalExtendedColors`) provided inside `SuperUnclenchTheme`, accessed as `SuperUnclenchTheme.extendedColors.good` etc. Each semantic has 4 roles mirroring M3: `color`, `onColor`, `container`, `onContainer`.

| Token | Light | Dark | Use |
|-------|-------|------|-----|
| `good` | `#276C3E` | `#8DD89F` | Good button fill, Good chart bars, Good icons/text on surfaces |
| `onGood` | `#FFFFFF` | `#00391A` | Text/icon on `good` |
| `goodContainer` | `#B8F0C3` | `#0D5228` | Good tile / history badge background, level-up-free success chips |
| `onGoodContainer` | `#0D5228` | `#B8F0C3` | Text on `goodContainer` |
| `bad` | `#B3261E` | `#FFB4AB` | Bad button fill, Bad chart bars |
| `onBad` | `#FFFFFF` | `#690005` | Text/icon on `bad` |
| `badContainer` | `#FFDAD6` | `#8C1D18` | Bad tile / history badge background |
| `onBadContainer` | `#8C1D18` | `#FFDAD6` | Text on `badContainer` |
| `missed` | `#5D616C` | `#C3C6CF` | Missed icon/text on surfaces (neutral, deliberately *not* amber/red) |
| `onMissed` | `#FFFFFF` | `#2E3035` | Text on `missed` |
| `missedContainer` | `#E1E2EC` | `#454A55` | Missed tile / history badge background |
| `onMissedContainer` | `#454A55` | `#E1E2EC` | Text on `missedContainer` |

Notification-only (resolved at runtime from the *system* night mode, not the app's forced mode, because the shade follows the system):
| Token | Light shade | Dark shade |
|-------|-------------|------------|
| `notifGood` | `#276C3E` | `#8DD89F` |
| `notifBad`  | `#B3261E` | `#FFB4AB` |
| `notifAccent` (`setColor`) | `#3D5F90` | `#3D5F90` (system adapts) |

Put these in `res/values/colors.xml` and `res/values-night/colors.xml` as `notif_good`, `notif_bad`, `notif_accent` so `ContextCompat.getColor` resolves them per system mode.

### 2.5 Contrast verification (WCAG 2.1, computed)
| Pair | Light | Dark | Req. |
|------|------:|-----:|-----:|
| onGood on good | 6.36 | 7.78 | 4.5 |
| onGoodContainer on goodContainer | 7.23 | 7.23 | 4.5 |
| onBad on bad | 6.54 | 7.72 | 4.5 |
| onBadContainer on badContainer | 7.05 | 7.05 | 4.5 |
| onMissedContainer on missedContainer | 6.89 | 6.89 | 4.5 |
| good on surface (text/icons/bars) | 6.07 | 11.02 | 3.0 (graphics) / 4.5 (text) |
| bad on surface | 6.23 | 10.94 | 3.0 / 4.5 |
| good on surfaceContainer (chart card) | 5.46 | 9.70 | 3.0 |
| bad on surfaceContainer (chart card) | 5.61 | 9.63 | 3.0 |
| missed on surface | 5.90 | 10.88 | 4.5 |
| primary on surface | 6.18 | 10.93 | 4.5 |
| onSurfaceVariant on surfaceContainer | 8.01 | 9.58 | 4.5 |
| notifGood / notifBad on shade (#FFFFFF / #202124) | 6.36 / 6.54 | 9.55 / 9.48 | 4.5 |

**Important:** `good` vs `bad` have almost identical luminance (1.03:1). They are distinguishable by hue for most users but **not** for red-green colour-blind users or in grayscale. Therefore Good/Bad must **always** also be encoded by: an icon (check vs close), a text label, and — in charts — position (Good bottom, Bad top) plus a hatch pattern on Bad. Never rely on colour alone.

---

## 3. Typography

System font only (**Roboto** via `FontFamily.Default`) — no downloadable fonts, no network dependency. Use the M3 default type scale with these overrides/additions:

| Style | Size / line | Weight | Tracking | Used for |
|-------|-------------|--------|----------|----------|
| displayLarge | 57/64 | 400 | -0.25 | Level number on expanded width |
| displayMedium | 45/52 | **500** | 0 | Level number (Home level card) |
| displaySmall | 36/44 | 400 | 0 | Countdown `12:34` (tabular) |
| headlineMedium | 28/36 | 400 | 0 | Home greeting "Hi, Alex" |
| headlineSmall | 24/32 | 400 | 0 | Stats tile numbers, dialog titles |
| titleLarge | 22/28 | 400 | 0 | Top app bar titles |
| titleMedium | 16/24 | **500** | 0.15 | Card titles, list headlines emphasised |
| titleSmall | 14/20 | 500 | 0.1 | Section headers in Settings / QA |
| bodyLarge | 16/24 | 400 | 0.5 | Body copy, list headlines |
| bodyMedium | 14/20 | 400 | 0.25 | Supporting text |
| bodySmall | 12/16 | 400 | 0.4 | Captions, chart tooltips |
| labelLarge | 14/20 | 500 | 0.1 | Buttons, chips, segmented buttons |
| labelMedium | 12/16 | 500 | 0.5 | Overline labels ("LEVEL", "NEXT CHECK-IN") — uppercase |
| labelSmall | 11/16 | 500 | 0.5 | Chart axis labels |

Additional style **`numericTabular`**: any style above + `fontFeatureSettings = "tnum"` — mandatory for countdowns, counts, times, axis labels so digits don't jitter.

Font scaling: all text in `sp`; layouts must survive 200% (see §10).

---

## 4. Shape

| Token | Radius | Used for |
|-------|-------:|----------|
| extraSmall | 4dp | Chart bar top corners (2dp for bars < 8dp wide), progress-segment corners use `full` |
| small | 8dp | Chips, snackbars, tooltips |
| medium | 12dp | Summary tiles, banners, text fields |
| large | **20dp** (M3 default 16 → 20 for friendlier cards) | Cards (level, session, pending check-in, chart) |
| extraLarge | 28dp | Bottom sheets, dialogs |
| full | 50% | Buttons (M3 default pill), progress segments, status chips, icon badges |

---

## 5. Spacing & layout

4dp base grid.

| Token | dp |
|-------|---:|
| `space.xxs` | 2 |
| `space.xs` | 4 |
| `space.sm` | 8 |
| `space.md` | 12 |
| `space.lg` | 16 |
| `space.xl` | 24 |
| `space.xxl` | 32 |
| `space.xxxl` | 48 |

- **Screen gutter:** 16dp (compact), 24dp (medium & expanded).
- **Max content width:** 600dp, centred horizontally, on medium/expanded. Stats on expanded may use two columns (tiles+chart | history), each max 480dp, 24dp gap.
- **Card padding:** 20dp internal (all sides). Between cards: 12dp. Between sections with headers: 24dp.
- **Min touch target:** 48×48dp everywhere (incl. chart bars — see chart spec).
- **Buttons:** height 48dp (M3 default 40dp → set `Modifier.heightIn(min = 48.dp)`); primary CTA on Home is 56dp high.
- Scrollable screens end with 24dp bottom padding above the nav bar/insets.

---

## 6. Elevation

Tonal elevation only, no drop shadows except FAB-like surfaces (none in v1).

| Surface | Color role | Shadow |
|---------|-----------|--------|
| Screen background | `surface` | 0 |
| Cards (level, session, chart, settings groups) | `surfaceContainer` | 0 |
| Summary tiles | semantic container or `surfaceContainerHigh` | 0 |
| Pending check-in card | `primaryContainer` + 1dp `primary` border | 0 |
| Banners | see §9 banner variants | 0 |
| Navigation bar / rail | `surfaceContainer` (M3 default) | 0 |
| Dialogs, bottom sheets | `surfaceContainerHigh` | M3 default |
| Snackbar | `inverseSurface` | M3 default |
| Chart tooltip | `inverseSurface` / `inverseOnSurface` | 2dp shadow |

Cards are `Card` (filled) with `CardDefaults.cardColors(containerColor = surfaceContainer)` — not `ElevatedCard`.

---

## 7. Iconography

**No `material-icons-core`/`-extended` dependency** is present — do not add it. All icons are **vector drawables** in `res/drawable/`, exported from **Material Symbols Rounded**, weight 400, grade 0, optical size 24, from fonts.google.com/icons ("Android" download). Fill 0 (outlined) unless noted; nav selected state uses Fill 1 variant.

| Drawable | Material Symbol | Where |
|----------|-----------------|-------|
| `ic_home` / `ic_home_filled` | home | Nav |
| `ic_stats` / `ic_stats_filled` | insert_chart (fill 0 / fill 1) — `bar_chart` has no distinct fill variant | Nav; Stats empty state may use `bar_chart` |
| `ic_settings` / `ic_settings_filled` | settings | Nav |
| `ic_play` | play_arrow (fill 1) | Start / Resume |
| `ic_pause` | pause (fill 1) | Pause |
| `ic_stop` | stop (fill 1) | Stop |
| `ic_good` | check | Good button, Good tile, history badge, legend |
| `ic_bad` | close | Bad button, Bad tile, history badge, legend |
| `ic_missed` | schedule | Missed tile, history badge |
| `ic_rate` | percent | Good rate tile |
| `ic_streak` | local_fire_department | Streak tile |
| `ic_level_up` | trending_up | Level-up message |
| `ic_level_down` | trending_down | Level-down message |
| `ic_timer` | timer | Interval line |
| `ic_notifications` | notifications | Rationale sheet |
| `ic_notifications_off` | notifications_off | Notifications-off banner |
| `ic_alarm` | alarm | Exact-timing banner |
| `ic_bedtime` | bedtime | Quiet hours banner / setting |
| `ic_info` | info | Disclaimer banner, About |
| `ic_person` | person | Settings: name |
| `ic_palette` | palette | Settings: theme |
| `ic_restart` | restart_alt | Reset progress |
| `ic_bug` | bug_report | Developer / QA entry (debug only; lives in `src/debug/res`) |
| `ic_chevron_right` / `ic_chevron_left` | chevron_right / chevron_left | List affordance, chart prev/next (auto-mirrored) |
| `ic_back` | arrow_back | Top app bar navigation (auto-mirrored: `android:autoMirrored="true"`) |
| `ic_stat_checkin` | sentiment_calm | **Notification small icon** — white-only, 24dp, no alpha gradients |

Icon size 24dp default; 20dp inside buttons (`ButtonDefaults.IconSize` 18dp is OK too — use 18dp consistently in buttons); 18dp inside chips; 20dp in history badges (badge 40dp circle).

Remove template drawables `ic_favorite.xml` and `ic_account_box.xml` in US-01.

**Launcher icon** (out of scope for v1 detail but noted): adaptive icon, background `#D5E3FF`, foreground `sentiment_calm` glyph in `#3D5F90` at 50% safe zone, plus monochrome layer for themed icons.

---

## 8. Motion

Principles: **soft, short, never startling.** Motion confirms state change; it never demands attention.

| Token | Duration | Easing (M3) | Use |
|-------|---------:|-------------|-----|
| `motion.short` | 150ms | `EmphasizedDecelerate` | Button/chip state, badge appear |
| `motion.medium` | 300ms | `Emphasized` (`FastOutSlowInEasing` acceptable) | Card expand/collapse (`AnimatedVisibility` + `expandVertically` + `fadeIn`), segment fill |
| `motion.long` | 500ms | `EmphasizedDecelerate` | Level change number roll, chart bar grow |
| `motion.celebrate` | 700ms | spring(dampingRatio 0.6, stiffness Low) | Level-up segment "pop" (scale 1.0→1.08→1.0) |

- Countdown text updates once per second **without** animation (no rolling digits — it would be distracting and hurts screenshots).
- Chart bars grow from baseline (0→height) on frame change, 500ms, 20ms stagger capped at 300ms total.
- Honour **Remove animations** (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`): all transitions become instant; celebration becomes a static tint.
- QA screenshots: animations must settle within 1s of the triggering tap.

---

## 9. Reusable components (app-level)

These are named composites the engineer builds once in `ui/components/` and reuses:

### 9.1 `SegmentedProgressBar`
- N segments (3–5) in a row, equal width, **height 12dp**, gap 6dp, shape `full`.
- Filled segment: `primary`. Empty segment: `outlineVariant` (changed 2026-09-30 from `surfaceContainerHighest`, which was nearly invisible on the `surfaceContainer` card in light mode), no border.
- Max level: all segments filled with `tertiary`.
- Newly-filled segment animates fill width 0→100% over `motion.medium`; on level-up the whole bar pulses (`motion.celebrate`) then resets to empty segments of the new level.
- Semantics: `progressBarRangeInfo(current/total)`, contentDescription e.g. "Level progress: 2 of 4".

### 9.2 `AnswerButtons` (Good / Bad pair)
- Row, two equal-width `Button`s, gap 12dp, height **56dp**, shape `full`.
- **Good:** container `good`, content `onGood`, leading `ic_good`, label "Good". Order: Good **left**, Bad **right** (in RTL mirrored naturally).
- **Bad:** container `bad`, content `onBad`, leading `ic_bad`, label "Bad".
- Supporting captions under each button (bodySmall, `onSurfaceVariant`, centered): "I was relaxed" / "I was clenching".
- Pressed: M3 state layer. Disabled (while submitting): 38% content alpha.

### 9.3 `StatusChip`
- `AssistChip`-look but non-interactive (use a `Surface` shape `full`, height 32dp, horizontal padding 12dp, leading 8dp dot).
- Running: container `goodContainer`, content `onGoodContainer`, dot `good` — text "Running".
- Paused: container `secondaryContainer`, content `onSecondaryContainer`, dot `secondary` — text "Paused".
- Stopped: container `surfaceContainerHighest`, content `onSurfaceVariant`, dot `outline` — text "Stopped".
- Quiet hours: container `tertiaryContainer`, content `onTertiaryContainer`, leading `ic_bedtime` 16dp — "Quiet hours".

### 9.4 `InfoBanner`
Card, shape `medium`, padding 16dp, row: leading icon 24dp, column (title `titleSmall` optional + body `bodyMedium`), actions row right-aligned below body (`TextButton`s). Variants:
| Variant | Container | Content | Used for |
|---------|-----------|---------|----------|
| `neutral` | `surfaceContainerHigh` | `onSurface` / icon `onSurfaceVariant` | Disclaimer |
| `attention` | `tertiaryContainer` | `onTertiaryContainer` | Notifications off, exact timing, auto-pause |
| `calm` | `secondaryContainer` | `onSecondaryContainer` | Quiet hours |
Never use `errorContainer` for banners (keeps the app calm).

### 9.5 `StatTile`
- Shape `medium`, min height 88dp, padding 16dp.
- Top row: icon 20dp + label `labelMedium` (uppercase). Bottom: value `headlineSmall` tabular.
- Colours per semantic: Good → `goodContainer/onGoodContainer`; Bad → `badContainer/onBadContainer`; Missed → `missedContainer/onMissedContainer`; Rate & Streak → `surfaceContainerHigh/onSurface` with icon `primary`.
- Whole tile is one semantics node: "Good: 12".

### 9.6 `HistoryRow`
M3 `ListItem`: leading 40dp circle badge (semantic container + icon 20dp in semantic on-container), headline (bodyLarge) "Good"/"Bad"/"Missed", supporting (bodyMedium, `onSurfaceVariant`) "Level 3 · Aware", trailing (labelLarge tabular, `onSurfaceVariant`) time. Min height 64dp. Dividers: none (use 0dp spacing; list lives in a `surfaceContainer` card).


### 9.7 `AlarmAnswerTile` (US-11 alarm screen)
- Large Good/Bad target: ≥ 128dp tall (≥ 96dp in landscape), half width (≥ 160dp), shape `extraLarge`, gap 12dp. Good left, Bad right.
- Good: `good`/`onGood`, `ic_good` 36dp; Bad: `bad`/`onBad`, `ic_bad` 36dp. Label `headlineSmall` 500, caption `bodyMedium` at 85%.
- Accidental-touch guard: ignore input for 800 ms after show (fade-in), single pointer only, cancel on > 24dp drag, `filterTouchesWhenObscured`. Accessibility clicks bypass the guard.
- Stacks vertically (Good on top, full width, ≥ 112dp) at fontScale ≥ 1.5 or width < 160dp.
---

## 10. Accessibility baseline (applies to every screen)
- Touch targets ≥ 48dp; spacing between adjacent targets ≥ 8dp. **Exception:** dense chart bars (e.g. 24 per-hour slots) may be narrower than 48dp *only* when an equivalent full-size control exists (the Stats ‹ › readout, US-08 §4.7); their semantics nodes must match the drawn slots exactly.
- Contrast per §2.5; never colour-only meaning.
- Every icon-only element has a `contentDescription`; decorative icons next to text use `null`.
- TalkBack order = visual order top→bottom, left→right; each card merges into logical groups (`Modifier.semantics(mergeDescendants = true)`) except interactive children.
- Live regions: countdown is **not** a live region (would spam); level changes, snackbars and pending check-in appearance are `LiveRegionMode.Polite`.
- Font scale 200%: no truncation of essential text; rows wrap to columns (e.g. Pause/Stop stack vertically when width per button < 120dp or fontScale ≥ 1.5); tiles 2×2 → 1 column at fontScale ≥ 1.5.
- Reduced motion honoured (§8).
- Test tags: every interactive element gets a stable `Modifier.testTag("...")` as listed in each story doc — QA relies on them for screenshot scripts.

## 11. Chart styling (Compose Canvas; no library)
Full spec in `US-08-stats.md`. Summary tokens:
- Plot area background: none (sits on `surfaceContainer` card).
- Gridlines: `outlineVariant`, 1dp, dashed 4dp on / 4dp off; 3 lines (0, mid, max). Baseline (0): solid `outline` 1dp.
- Axis labels: `labelSmall`, `onSurfaceVariant`, tabular.
- Good bars: `good`, solid. Bad bars: `bad` + hatch (45° lines, 1.5dp stroke, 5dp spacing, colour `onBad` at 35% alpha) — stacked **Good bottom, Bad top**, 1dp gap between segments (drawn as `surfaceContainer`).
- Bar top radius `extraSmall` (4dp; 2dp if bar width < 8dp); bottoms square.
- Selected bar: others dim to 40% alpha; tooltip `inverseSurface` with `bodySmall` `inverseOnSurface`.
- Legend: two `LegendItem`s (12dp swatch with same fill/pattern + `labelMedium` text) above the chart, left-aligned.
