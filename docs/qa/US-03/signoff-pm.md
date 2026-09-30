# PM sign-off — US-03 Start a session & receive a check-in notification

- **Date:** 2026-09-30 · **Reviewer:** PM
- **Verdict:** ✅ **ACCEPT** (7/7 criteria met; 0 blocking bugs; follow-ups below)
- **Inputs:** `report.md`; screenshots 03, 06, 13 and 18 reviewed.

## Per criterion

| # | Criterion | PM | Notes |
|---|-----------|----|-------|
| 1 | Rationale sheet → Continue → system dialog | Met | The copy is "Check-ins arrive as notifications", not the AC's "We need notifications to send check-ins". **The design copy is accepted.** It explains the same thing more warmly. "Not now" keeps the session stopped, as intended. |
| 2 | Allow → Running, and Stop replaces Start | Met | |
| 3 | Don't allow → banner + Open settings | Met | |
| 4 | Android ≤12 → starts without a dialog | Met (simulated) | Accepted on the preview toggle. The permission check is a simple SDK-version branch, so the risk is low. A real API ≤32 run goes on the release checklist. |
| 5 | Fire alarm now → heads-up with Good/Bad, not full-screen | Met | Sound and vibration can't be screenshotted. The alerting marker and the HIGH channel are enough for now. |
| 6 | Pending card with Good/Bad dismisses the card and the notification | Met | The card reads clearly in light and dark mode, and the green/red buttons have word captions. |
| 7 | Stop removes the notification, Start comes back, "Not running" | Met | |

## Bugs: triage

| # | PM call | Owner / when |
|---|---------|--------------|
| B1 White launch flash (in-app Dark, system light) | **Not blocking here.** It **must be fixed before US-09 is accepted**, because US-09's AC says the app "stays dark after relaunch regardless of system setting". Applying the stored theme before the first frame, or theming the splash screen, is the fix. | Eng, US-09 |
| B2 Truncated "Preview: Android 12…" label | Not blocking (debug only). Shorten it whenever convenient. | Eng, any story |
| B3 First fire after the permission grant showed no heads-up | **Must investigate before US-05 sign-off.** The first check-in a new user gets matters most. Eng: make sure the `checkins` channel is created at app start (or before the first `notify()`). QA: re-test on a fresh install with the permission granted for the first time. | Eng + QA, US-05 |

## Other findings
- **Notification action colours:** Good/Bad show in the system accent colour on API 37, and the word labels carry the meaning. Accepted. The Option B spike is not needed for v1.
- **Placeholder launcher/notification icon:** outside US-03's scope. A real icon is needed before release (release checklist).
- **Heads-up title truncated** ("Check-in: jaw rel…") when the subtext is shown: cosmetic, and the full text shows when expanded. The Designer may want to shorten the subtext.

## Must-fix for this story
None.

## Answers to open questions
- **New user advances through sub-levels (Level 1 · 1/3, 2/3) while intervals stay 5 min until Level 2: confirmed OK.** This is the SRS table in §2: every L1 sub-level uses the 5 min interval, and the interval changes only on promotion. It is fine that sub-level movement arrived early in US-03. US-04 formally verifies the full rules.
- **Fire alarm now while a check-in is pending marks the earlier one as Missed: confirmed correct.** This matches §2 ("not answered before the next alarm fires" = Missed, neutral, no level change). US-07 formally verifies misses and auto-pause.
