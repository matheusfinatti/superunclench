# PM sign-off v2 — US-10 Quiet hours (reopened: Starts/Ends time pickers)

- **Date:** 2026-10-01 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT** (8/8 met; 0 bugs; no must-fix). This replaces the preset-version sign-off.
- **Inputs:** `report-v2.md` and the `v2/` evidence.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1 | Switch + Starts/Ends rows, defaults 22:00/07:00, presets gone | Met | The "Next day" caption and the "9 h of quiet" duration line are nice clarity additions. Approved. |
| 2 | M3 picker: dial + keyboard, OK/Cancel, any minute, 12/24 h | Met | Tested with the dial and real typing, and the 24 h device setting switches both the rows and the picker. |
| 3 | Overnight and same-day ranges | Met | 23:30–06:15 and 13:00–14:00 both behave correctly. |
| 4 | Start == end blocked | Met | Error copy is exact, OK is disabled, the range is kept. |
| 5 | *(regression)* Deferral, no Missed, Home state | Met | Verified against the real clock as well as the simulation. |
| 6 | *(regression)* Fires at the end time; switch OFF | Met | The firing itself was seen in the v1 run, and v2 shows the alarm scheduled at exactly the end time. Accepted. |
| 7 | Editing while running | Met | Real clock, both branches (inside → end time; outside → now + interval). |
| 8 | *(regression)* Start during quiet → snackbar | Met | |

## Engineer's question: confirmed
**If now is inside the newly edited window but the next check-in is already scheduled *after* the window ends, keep that later time.** Don't pull it forward to the window end.
- Rule: `next = max(already scheduled next, window end)`.
- Quiet hours only **defer** check-ins and never make them come **sooner** than the user's current interval.
- AC7's "deferred to the new end time" covers the usual case where the scheduled check-in falls inside the window. This is recorded in the Decisions log.

## Notes (non-blocking)
- Keyboard-mode picker input over adb races the IME. This is tooling only; a human typing is unaffected.

## Must-fix
None.
