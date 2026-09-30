# Design sign-off: US-04 SRS progression (level card, segmented bar, feedback)

- **Reviewer:** Designer · **Date:** 2026-09-30
- **Inputs:** `docs/design/US-04-srs-progression.md`, `design-system.md`, QA `report.md`, and screenshots 04, 09, 21, 23, 31, 39, 41, 45, 46.

## Verdict: **ACCEPT**

The level card follows §2:
- "LEVEL" overline and the `primary` level name.
- `displayMedium` number with "of 8".
- Segmented bar with the right segment counts, and `tertiary` segments at max level.
- Progress caption, divider, and the timer + interval line.
- At Level 8, the caption reads "Level 8 · Max level" / "Goods still count".

Feedback messages match §3 exactly, in light and dark: copy, icons and containers are right, "Level up!" is emphasised, and level-down is on `secondaryContainer`, never red. The segment drain animation is visible (07). The new launcher icon (the calm face) shows in the Recents chip (46).

## Must-fix
None.

## Confirmations
- **Notification level line removed:** confirmed. US-03 §4.2 already said "Sub-text: none". I've now also updated the §4.2 heads-up mockup and marked the `notif_subtext` copy key as unused. The shade in 31 ("SuperUnclench • now", full title) is correct.
- **Notification app icon** still shows the green robot in 31. SystemUI caches icons across incremental installs, so re-check after an uninstall and reinstall at the next QA run. If it still shows the robot, it's a bug in the icon resources.

## Follow-ups (fix alongside the next story)
1. **Empty segments are nearly invisible in light mode** (04, 09): `surfaceContainerHighest` on the `surfaceContainer` card is about 1.1:1. The caption still gives "0 of 3", so this isn't blocking, but the bar should show how many steps are left at a glance. **Token change:** empty segment is now `outlineVariant` (#C3C6CF light / #43474E dark). I've updated design-system §9.1 and US-04 §2.1.
2. **"of 8" floats far from the number** (right edge of the card). It reads as a separate label. Put it directly after the number with an 8dp gap, on the number's baseline (`Modifier.alignByBaseline()`), in `bodyMedium` `onSurfaceVariant`. At fontScale ≥ 1.5 it still moves under the number (§8).
3. **Paused state has no way to resume** (39: "Paused" chip, Stop only). This belongs to US-06/US-07 and isn't a US-04 defect. But auto-pause already happens in the engine, so **US-06 must not ship without Resume** (and US-07 without the auto-pause explanation in the session card). A user who is auto-paused today can only Stop and then Start.
4. **QA panel Danger zone is unreachable** (B1, 41). Make the whole panel one `LazyColumn` with the state card as its first item. It stays visible at the top, and every section can be scrolled to. QA can still take a readout screenshot with every section collapsed. This replaces the "pinned" rule in US-02 §3. Also add the bottom padding from US-02 follow-up 2.
5. **O1 (unexplained promotion while idle):** no design action. It needs a way to show history. Proposal for Eng: a QA readout row "Last 5: G B M M M" (letters, newest last), so QA can check sequences by tapping.
6. Carried over and still open: Material Symbols Rounded icon re-export; template drawables; snackbar shape/padding; confirmation border (US-03 follow-up 2).

## Not reviewed
- The promotion pulse and number slide animation (too fast to capture).
- Expanded-width `displayLarge`.
- 150–200% font scale.
- TalkBack label.
