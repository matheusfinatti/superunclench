# PM sign-off — US-08 Stats: Good/Bad charts with time frames

- **Date:** 2026-10-01 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT** (7/7 met). B1 must be fixed **before v1 release** but does not block this story.
- **Inputs:** `report.md`; screenshots 02 (Today, light) and 09 (7 days, dark, bar selected) reviewed.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1 | Today / 7 days / 30 days selector, Today by default | Met | Keeping the last frame within a session is fine. A cold start opens on Today. |
| 2 | Per-hour bars for Today, per-day bars for 7/30 days, chips update the chart | Met | 24 / 7 / 30 bars. An empty day is shown as a gap, not hidden, which is good. |
| 3 | Good / Bad / Missed / Good rate tiles | Met | Every seeded number matches. Good rate excludes Missed (Decisions log). |
| 4 | Streak with the known seeded value | Met | 4 days, consistent with the decided rule: Sun 27 is empty, so the streak is Mon–Wed plus today (71%). |
| 5 | Empty state | Met | Includes a "Go to Home" button, which is a nice touch. |
| 6 | Legible in light and dark, distinguishable without colour | Met | Legend, hatched Bad bars, and ✓/✕/clock icons plus words. Contrast is good in dark mode. |
| 7 | Recent list: last 20 events with time, outcome and level | Met | |

## Findings: triage

| # | Finding | PM call |
|---|---------|---------|
| B1 | Per-hour chart: each bar's accessibility node sits about one bar to the left, so TalkBack, or a tap at a node's centre, selects the **previous hour** | **Must-fix before v1 release (not blocking US-08).** Screen-reader users would hear the wrong hour's numbers, which is wrong data, not just cosmetic. Eng: apply the drawn-bar x offset to the semantics nodes in **all three** frames, and add a test. QA: re-verify that tapping a drawn bar and the node centre select the same bar. |
| N1 | a11y summary "Highest: …" picks the first of tied hours | **Decision:** rename it to **"Busiest"**: the hour or day with the most answers (Good + Bad). Ties go to the **most recent**. Small copy change; do it with the B1 fix. |
| N2 | The readout under the per-hour chart wraps to two lines | Cosmetic. Designer's call (for example, leave out "0 Missed" when it is zero). Not required. |
| N3 | Seed puts today's events after "now" (future-dated) | **Decision:** the seed should shift today's events so the last one is **at or before the current time**, keeping the counts (5/2/1) the same. Screenshots stay comparable and the list never shows the future. Should-fix, debug only. |
| — | A reinstall reset the notification and exact-alarm grants on the emulator | Environment only. Noted for QA setup. |

## Must-fix for this story
None. **Release gate:** B1 (with N1).
