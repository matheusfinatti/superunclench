# PM sign-off — US-09 Settings: name, theme, reset

- **Date:** 2026-10-01 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT** (5/5 met; 0 bugs; no must-fix)
- **Inputs:** `report.md`; screenshot 16 (About) reviewed.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1 | Settings shows the name field, Theme, Reset progress and About | Met | The Developer section appears only in debug builds. |
| 2 | Name "Alex" → "Hi, Alex"; empty → "Hi there" | Met | **Real typing** this time, so the US-09 note about verifying through the debug Set name action no longer applies. The 24-character cap works. |
| 3 | Dark is applied immediately and persists across relaunch whatever the system setting; System follows the device | Met | Cold-start screen recording: dark splash, **no white flash** in any of the 70 frames. Light forced on a dark device was also verified. |
| 4 | Reset progress: confirmation, Cancel does nothing, Reset → L1 0/3 and history kept | Met | History stayed at 309. The copy is clear and non-alarming. |
| 5 | About shows the disclaimer and the version | Met | Disclaimer wording is exact. "How it works" makes no health claims. |

## Earlier items closed
- **US-03 B1, white launch flash:** ✅ **closed** (that was the gate for US-09).
- Confirmed the new launcher icon on the home-screen dock.

## Decisions confirmed (designer questions)
- A **24-character name limit** is OK.
- **Reset progress while running:** the session **keeps running** at L1, and the next alarm uses the L1 interval from the moment of the reset. It is not stopped. This wasn't exercised in this run; QA should cover it in the release regression pass.

## Must-fix
None.
