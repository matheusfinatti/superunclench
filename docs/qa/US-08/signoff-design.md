# Design sign-off: US-08 Stats (Good/Bad charts with time frames)

- **Reviewer:** Designer · **Date:** 2026-10-01
- **Inputs:** `docs/design/US-08-stats.md`, QA `report.md`, and screenshots 02, 03, 05, 06, 08, 09, 10, 11, 17.

## Verdict: **ACCEPT**, with B1 to be fixed in the next build (before release)

The Stats screen matches the spec closely:
- **Frame selector:** a full-width segmented Today / 7 days / 30 days with a check on the selected item.
- **Tiles:** a 2×2 grid with the right semantic containers and icons (Good ✓, Bad ✕, Missed clock in neutral grey, Good rate % with a `primary` icon). The Streak tile is full width with its supporting line.
- **Chart card:**
  - Legend with a hatched Bad swatch.
  - Dashed `outlineVariant` grid with a solid baseline.
  - Stacked bars, Good at the bottom and Bad hatched on top. Correct in light and dark (02, 05, 08, 09).
  - The selected bar stays at full strength and the others dim. The `inverseSurface` tooltip and the readout row with ‹ › work.
- **Recent check-ins:** 20 rows with 40dp semantic badges, the outcome word, "Level n · Name", and a trailing time ("Yesterday 2:45 PM").
- **Empty state** with Go to Home.
- **Home TODAY row:** tiles and the "TODAY" overline are now correctly aligned, which closes US-06 follow-up 3.

Good/Bad is never shown by colour alone (legend, hatch, stacking order, icons, words), so AC6 is met.

## Must-fix (next build, together with US-10; not a re-review blocker)
**B1: per-hour chart semantics nodes are offset by one slot.** On the Today chart each node sits about one bar to the left. TalkBack's focus outline lands on the wrong bar, and a TalkBack double-tap (which clicks the node's centre) selects the **previous hour**. The readout then contradicts what was announced.

Fix: lay the semantics slots out over the **plot area only**, starting after the y-axis label gutter and using the same slot width and x offset as the drawn bars. Give each slot its own `onClick` that selects its bucket, instead of relying on the canvas hit test underneath. Then check 7 and 30 days the same way (QA suspects they share the offset).

I'm not rejecting the story over this because:
- Each node's **label** announces the right counts.
- The ‹ › readout gives screen-reader users a correct path to every bucket.
- Sighted taps hit the right bar.

QA: re-check `chart_bar_12` → "12 PM–1 PM" in the next run.

## Follow-ups (fix alongside US-10)
1. **The readout wraps to two lines** on a 412dp-wide screen (03). Use `bodyMedium`, which is what §4.7 specifies; the build appears to use `bodyLarge`. Use the compact hour range "11 AM–12 PM" (no ":00"), in the tooltip as well. That fits one line at 1.0 font scale. Two lines are still allowed at large font scales. I've updated §4.7.
2. **Chart a11y summary wording:** change "Highest" to **"Busiest"** (most answers). Break ties with the **most recent** bucket. Updated in §8.
3. **Empty state still shows the frame selector** (17). With no history at all, the selector has nothing to switch between. Hide it, so the screen shows only the icon, text and Go to Home, as in §6.
4. **The tooltip covers neighbouring bars** in the dense per-hour view (03). It's acceptable because the readout never overlaps. Optionally anchor the tooltip above the plot's top gridline for the Today frame.
5. The seed puts future-dated events today, which is a QA artefact only. If it's cheap, clamp the seeded "today" events to before now so screenshots look natural.

## Not reviewed
- Bar-grow animation timing.
- Expanded-width two-column layout.
- 200% font scale (tiles go to one column).
