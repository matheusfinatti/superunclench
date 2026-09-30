# PM sign-off — US-04 SRS progression: level counter & sub-level progress bar

- **Date:** 2026-09-30 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT, with one condition.** O1 must be root-caused; if the cause is an app bug in answer handling, this story reopens.
- **Inputs:** `report.md` (9/9 PASS); design-note screenshots; quick code check of the notification PendingIntents.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1 | L1 0 + Good → 1/3 | Met | |
| 2 | L1 2/3 + Good → L2 0/3 + "Level up!" | Met | Tested in light (L1→L2) and dark (L7→L8). |
| 3 | L3 2/4 + Bad → L3 0/4 | Met | The reset message is encouraging, as intended. |
| 4 | L3 0/4 + Bad → L2 0/3 | Met | Level-down isn't shown in red. |
| 5 | L1 0/3 + Bad → stays L1 | Met | |
| 6 | L8 + Good → "Level 8 · Max level", full bar | Met | The Decisions-log rule "Bad at L8 → L7 0/5" was also verified. |
| 7 | Interval matches the §2 table | Met | All 8 levels: names, segment counts and intervals are correct. |
| 8 | Notification and in-app card give identical results | Met | Level, next alarm (answer + 10:00), misses and history match exactly. |
| 9 | State survives force-stop and relaunch | Met (workaround accepted) | The Android Studio Stop plus a relaunch from Recents is a real process kill: the app came back on Home, not the panel it was left on. The readout was identical before and after. |

## O1: unexplained L2 2/3 → L3 0/4 during a 3 h idle. **Does not block US-04; resolution required (see below).**
- **Why not blocking:** every answer path passed deterministically, both notification and card. The SRS engine is unit-tested. A quick code check found no obvious path that turns a miss into a Good: the Good and Bad broadcasts use distinct actions, there is no `deleteIntent` yet, and `Missed` is neutral in code. The 6 events fit the pattern **Miss, Miss, Good, Miss, Miss, Miss** (misses reset by one Good, then 3 misses → auto-pause). That is consistent with **one stray Good**, for example an accidental tap on a heads-up while the emulator was shared.
- **Why it still matters:** if a Good can be recorded without the user tapping it, our core success metric (Good share) and the user's level both become untrustworthy.
- **Required, owned by Eng now, in parallel with US-05:**
  1. Add a **history dump** (last 10 events: time, type, source such as notification, card, QA or miss-by-next-alarm, and the level before and after) to the QA readout or a QA button. This is also needed for US-07 and US-08 QA.
  2. Pull the history from the device (it still holds the 22 events) and explain the Good.
- **Gate:** O1 must be explained **before US-07 is signed off** (misses and auto-pause). If the root cause is an app bug in answer or miss handling, **US-04 reopens** as a must-fix.

## B1: Kill app button unreachable (the QA panel doesn't scroll to it)
- **Not blocking for US-04.** It is a **must-fix before US-05 QA starts**, because the reboot and persistence checks there need a tap-only kill. Fix: make the whole panel one scroll container, or move the Danger zone and Kill app up.

## Design notes (Designer's call, non-blocking)
- Dropping the notification sub-text so the title doesn't truncate: **PM agrees.** Designer to update US-03 §4.2.
- New launcher icon: re-check the notification large icon after a clean install (release checklist).

## Must-fix for this story
None, subject to the O1 condition above.
