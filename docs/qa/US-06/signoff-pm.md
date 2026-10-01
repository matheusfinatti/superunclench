# PM sign-off — US-06 Next-alarm countdown, Pause & Resume

- **Date:** 2026-10-01 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT** (5/5 met; 0 bugs; no must-fix)
- **Inputs:** `report.md`; unit-test list in `CheckInControllerTest`.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1 | Countdown "Next check-in in mm:ss", matching the readout | Met | Home and the readout agree and the countdown ticks. Layout is the accepted overline + display (Decisions log). |
| 2 | Pause 1 h → "Paused until HH:MM" + Resume, pending alarm cancelled | Met | Inline chip picker with Cancel. Readout shows `Next alarm —`. |
| 3 | Until I resume → Resume → full interval | Met | 03:00 at L8 in short mode. |
| 4 | Timed pause ends → auto-resume | Met | 15 min = 15 s, per the updated AC. "Check-ins resumed" snackbar shown. |
| 5 | Paused → Stop → Start shown | Met | Level kept. |

## Earlier follow-ups now closed
- **US-05 B1b, Kill app unreachable:** ✅ **closed.** The jump-chip row works.
- **US-05 AC4, real permission revoke:** ✅ **closed.** A real `pm revoke` showed the banner, Open settings re-enabled it, and the banner cleared on return.
- **US-05 AC3, real `adb reboot`:** ✅ **closed.** The boot receiver re-registered the exact alarm, and it fired on time with a single notification.
- **US-05: dark-shade notification pills:** ✅ **closed.** **US-01 caveat, system dark mode:** ✅ **closed** by the same run.
- **US-03: sound and vibration unverified:** ✅ **closed.** The channel is HIGH with the default sound and vibration pattern.
- **New icon in notifications:** ✅ confirmed.

## Notes (non-blocking)
- **TalkBack countdown label is minutes-only** ("in 2 minutes, at 9:38 AM"; seconds under 1 min). **Accepted.** The label updates at most every 60 s, so seconds would be stale anyway, and "at 9:38 AM" carries the exact time. The Designer should update §7 to match.
- **Heads-up covering the QA jump chips at a 5 s cadence:** QA tooling only. QA should keep Auto-pause ON and Short intervals OFF unless a test needs them. A "suppress heads-up while the QA panel is open" option is not needed now.
- "Paused until" uses minute resolution in short mode. That is expected.

## Must-fix
None.
