# Design sign-off: US-09 Settings (name, theme, reset, About)

- **Reviewer:** Designer · **Date:** 2026-10-01
- **Inputs:** `docs/design/US-09-settings.md`, QA `report.md`, screenshots 01, 05, 06, 08 (cold-start frames from the mp4), 14, 16.

## Verdict: **ACCEPT**

- **Settings** (01 light, 06 dark):
  - Group headers in `titleSmall` `primary`, over `surfaceContainer` cards with `large` corners.
  - Outlined name field with the person icon, supporting text and a clear button. The counter only appears near the limit (24/24, 05).
  - A full-width Light / Dark / System segmented control with a check on the selected item.
  - "Reset progress" in the `error` colour with its supporting line; About with the version and a chevron; Developer (debug).
  - This replaces the faint disabled placeholder rows from US-01 (US-01 follow-up 2 closed).
- **Reset dialog** (14): restart icon, centred title, exact body copy, **Cancel** (primary) and **Reset** (`error`). The snackbar copy is exact.
- **About** (16): calm face in the `primaryContainer` circle, "SuperUnclench", "Version 1.0 (1)", the neutral disclaimer banner, and "How it works". It matches §4.
- **Theme, cold start** (08): with in-app Dark on a light system, the splash is dark with the calm-face icon and Home appears with **no white flash**. US-03 B1 is closed and design-system §2.1 is satisfied. System follows the device live, and Light stays light on a dark device.

## Must-fix
None.

## Follow-ups (polish)
1. **The name field's IME action and capitalisation** (`Words`, `Done`) weren't verifiable because QA typed through adb. Engineer: confirm in code, since there's no visual change.
2. The Check-ins group (US-10, quiet hours) will sit between Appearance and Progress, per §2. Keep the same card and header pattern.

## Not reviewed
- TalkBack on the segmented control (it should announce "Theme, Dark, selected, 2 of 3").
- 200% font scale.
