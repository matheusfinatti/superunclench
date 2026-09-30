# PM sign-off — US-02 Developer / QA panel (debug builds only)

- **Date:** 2026-09-30 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT** (7 PASS; 1 NOT TESTABLE accepted from a static check; 0 bugs)
- **Inputs:** `report.md`, screenshot 02 (readout) reviewed, `src/release/.../DebugToolsProvider.kt` read.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1a | Debug: Settings → "Developer / QA" opens the panel | Met | |
| 1b | Release: entry absent | Accepted (NOT TESTABLE) | Release `DebugToolsProvider.create()` returns `NoOpDebugTools`, and all panel code is under `src/debug`. The code path is trivial and structurally safe, and `assembleRelease` is green. QA should still do a one-off release install check (see follow-up 1). |
| 2 | Button list, each ≥48dp | Met | Accordion sections are fine. They are still "a scrollable list of buttons". |
| 3 | Set name "Alex" / Clear name update the greeting | Met | |
| 4 | Theme cycle System→Light→Dark→System, immediate, value shown | Met | |
| 5 | Reset all data | Met (partial) | Name, disclaimer, theme and session are verified. The level and history reset paths can't be exercised until US-04/US-08, so **QA must re-verify Reset all data in those stories**. |
| 6 | State readout | Met | Running, paused and next-alarm values get checked in US-03, US-05 and US-06. |
| 7 | Snackbar per action | Met | The "Opening shade" snackbar is hidden by the shade itself. Accepted. |

## Must-fix
None.

## Follow-ups (non-blocking)
1. **QA:** before the first public release, install a release build and screenshot Settings to confirm there's no "Developer / QA" entry. Tracked as a release-checklist item.
2. **QA:** re-run **Reset all data** in US-04 (level ≠ L1/0) and US-08 (seeded history) and confirm L1/0 and history 0.
3. **Designer:** the readout rows aren't paired, and the snackbar overlaps the "Danger zone" header. Both are cosmetic and debug-only, so no change is needed.
4. **"Sub-level +1" button** (designer question): approved as a QA convenience.
