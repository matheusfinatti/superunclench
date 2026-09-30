# PM sign-off — US-05 Reliable scheduling (exact alarms, reboot, permissions)

- **Date:** 2026-09-30 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT** (5/5 met; nothing blocks this story). Must-fix items are gated to the next stories, listed below.
- **Inputs:** `report.md`; screenshots 02, 05, 09 and 28 reviewed.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1 | No exact-alarm permission → session starts, banner shown, Allow exact timing opens Alarms & reminders | Met | Tested against the real denied state. Banner copy is correct. Not now is accepted (designer proposal). |
| 2 | Grant, then return → banner disappears | Met | Real grant. |
| 3 | Reboot → still running, next alarm in the future | Met | Simulate reboot plus two real process kills (Run, and force-stop + relaunch). A future alarm is kept rather than re-based, which is correct. A real `adb reboot` goes on the release checklist. |
| 4 | Notification permission revoked while running → banner | Met (preview) | Accepted for this story. Banner priority and the session-card caption are correct. **Follow-up (US-06 QA):** revoke the real permission once through Open settings, confirm the banner appears on return, then re-grant. This checks the on-resume detection path, not just the UI. |
| 5 | Short intervals ON → real alarm arrives within 30 s; readout shows ON | Met | Scale is **1 min = 1 s**, not the AC's 1 min = 5 s. **The founder asked for this, so the AC text is updated** (see below). Real alarms were delivered and answered from the heads-up, the shade and the card, and the sources were logged. |

## AC text updated (founder request)
- US-05 AC5 now reads **1 min → 1 s** (L1 = 5 s … L8 = 180 s), **ON by default in debug**, and never in release. The US-02 action list and the US-06 timed-pause AC are updated to match, so a 15 min pause lasts 15 s in short mode.
- **Designer:** update the `(short: 25 s)` example in `US-05-reliable-scheduling.md` to `(short: 5 s)`.

## Findings: triage

| # | Finding | PM call | Gate |
|---|---------|---------|------|
| B1b | QA panel can't be scrolled with taps, so Kill app and the top of the readout can't be reached | **Must-fix.** The workaround (Android Studio Stop/Run) works but slows every story. Add a tappable "Jump to" chip row, **or** move Kill app into Session & alarms next to Simulate reboot and add a "Scroll to top" button. | **Before US-06 QA starts** |
| O2a | After auto-pause, the heads-up can stay visible and tapping **Good** on it does nothing | **Must-fix (user-facing).** A user who taps Good and sees nothing happen loses trust. Auto-pause must **cancel the notification**, like Stop does (US-03 AC7). Any stale action that still arrives is ignored silently (§2). | **US-07 AC** (added as an explicit check) |
| O2b | Auto-pause after about 15 s at a 5 s cadence makes hands-free QA hard | **Approved:** add a debug toggle **"Auto-pause: ON/OFF"** (default ON), shown in the readout. | With US-07 |
| O3 | Possible race: a card answer at :14, then "M replaced" at :19 | **Must investigate.** Eng: add a unit test that an answer cancels and re-schedules the pending alarm and clears `pending`, so the next alarm can't record a miss for an already-answered check-in. If it's a real bug, fix it. | **Before US-07 sign-off** |
| O1 | US-04 unexplained promotion | **Closed.** History shows a real Good at 20:30:44. It was recorded before source tracking existed, so it's most likely a heads-up tap. The new source column (`card` / `notif` / `qa` / `replaced`) makes any repeat traceable. QA: flag any Good whose source doesn't match what you tapped. | — |
| — | Dark-mode notification pills unverified (the emulator system theme is light) | **QA is allowed to switch the emulator's system dark mode.** Verify the pills in a dark shade during US-06 QA. The same run also closes the US-01 caveat about system dark mode. | US-06 QA |
| — | Notification icon still shows the green robot | SystemUI cache after an incremental install. Re-check after a clean install (release checklist). | Release |

## Must-fix for this story
None. Gated must-fixes: **B1b before US-06 QA**; **O2a and O3 before US-07 sign-off**.
