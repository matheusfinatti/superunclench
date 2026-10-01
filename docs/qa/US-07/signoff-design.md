# Design sign-off: US-07 Missed check-ins & auto-pause

- **Reviewer:** Designer · **Date:** 2026-10-01
- **Inputs:** `docs/design/US-07-missed-and-auto-pause.md`, QA `report.md`, and screenshots 03, 06, 13.

## Verdict: **ACCEPT**

- **Auto-paused session card** (06 light, 13 dark) matches §2:
  - The Paused chip in `secondaryContainer`.
  - An inner `tertiaryContainer` banner inside the session card, not at the top of Home, with the schedule icon, the exact title "Paused — you missed 3 check-ins. Resume?" and the body "No worries — we stopped so we won't nag you while you're away."
  - A filled **Resume** next to an outlined **Stop**.
- **Tone is right:** no red, no guilt, and no notification is posted for the auto-pause. The shade is empty, which also confirms the US-05 O2 concern is fixed.
- **Missed hint on the pending card** (03): "Missed the last one — that's fine." in `bodySmall` at reduced emphasis, under the body. It matches §3.
- **Missed tile** (US-08 preview, 06/13) uses the neutral grey `missedContainer` with the schedule icon.
- Swipe-dismiss counts as Missed with no visible penalty. QA copy matches §4.

## Must-fix
None.

## Follow-ups (fix alongside the next story)
1. **The auto-pause banner's TalkBack label and live-region announcement were not verified** (§7: "Paused. You missed 3 check-ins. Resume?", polite). QA: check with TalkBack, or dump the semantics, during US-08.
2. The Today tiles and the clipped "TODAY" overline belong to US-08 (see US-06 sign-off, follow-up 3).

## Not reviewed
- Live-region announcement.
- Large font scale for the inner banner wrapping.
