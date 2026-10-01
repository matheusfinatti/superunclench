# Final regression smoke + hero screenshots — SuperUnclench v1.0 (1)

- **Build:** latest debug build (installed 2026-10-01 14:30 via the adb bridge), `Pixel_9(AVD)`, API 37. Run 14:53–15:07 BST, after US-10 and the US-08 B1 re-check.
- **Tester:** QA agent · **Result:** the end-to-end flow **passes**. No new defects. Open item: US-08 B1 (a11y node offset in the Today/30d charts, minor). See `../US-08/report.md`.

## End-to-end smoke

| Step | Result | Evidence |
|------|--------|----------|
| Start with Short intervals ON (L1 = 5 s) → a **real scheduled alarm** posts a heads-up with the green/red pills, and Home shows the pending card | PASS | [01](01-e2e-real-alarm-headsup.png) |
| Tap **Good** on the heads-up pill → `G notif L1 0 → L1 1` (readout Last events) | PASS | [03](03-e2e-after-pill-good.png) |
| Auto-pause after 3 unanswered 5 s alarms while I was busy in the QA panel: shade empty, as designed | PASS (expected) | [02](02-e2e-autopaused-shade-empty-tooling.png) |
| Resume → next real alarm → tap **Bad** on the expanded shade pill → `B notif L1 1 → L1 0` | PASS | [04](04-notification-shade-pills-light.png) |
| Pending card shows the hint "Missed the last one — that's fine." after misses | PASS | [06](06-e2e-missed-hint-pending.png) |
| Answer **Good** on the Home card three times, against real 5 s alarms → **Level up!** L2 "Noticing", "(short: 10 s)", "Nice — noted." | PASS | [07](07-e2e-level-up-L2.png) |
| **Pause → 1 h** → "PAUSED UNTIL 4:01 PM" + snackbar; **Resume** → countdown "10:00" (the full L2 interval, Short OFF) | PASS | [08](08-e2e-paused-1h.png), [09](09-e2e-resumed-countdown.png) |
| **Stats** (seeded) light/dark, 7-day chart, tiles 45/12/1/79%, streak 4 days | PASS | [hero-02](hero-02-stats-light.png), [hero-06](hero-06-stats-dark.png) |
| **Settings** theme **Dark** → app dark immediately; **Light** → light; **System** restored | PASS | [10](10-e2e-settings-theme-dark-applied.png), [11](11-e2e-settings-theme-light-applied.png) |
| Stop → Stopped, no SuperUnclench notification left (`dumpsys` count 0) | PASS | [12](12-final-state-home-stopped.png) |

Tooling note: [05](05-e2e-qa-answer-attempt.png) is an unsuccessful attempt to drive QA "Answer Good" while 5 s heads-ups were re-alerting. The heads-up window makes `uiautomator dump` unreliable, so I used the card instead. This is not an app issue.

## Hero screenshots

| Screen | Light | Dark (system dark mode) |
|--------|-------|------|
| Home (running, countdown) | [hero-01](hero-01-home-light.png) | [hero-05](hero-05-home-dark.png) (+ pending card [hero-05b](hero-05b-home-dark-pending-card.png)) |
| Stats (7 days, seeded) | [hero-02](hero-02-stats-light.png) | [hero-06](hero-06-stats-dark.png) |
| Settings (Profile, Theme, Check-ins/Quiet hours) | [hero-03](hero-03-settings-light.png) | [hero-07](hero-07-settings-dark.png) |
| Notification (expanded, custom pills, calm-face icon) | [hero-04](hero-04-notification-light.png) | [hero-08](hero-08-notification-dark.png) |

## Story status summary (QA view)
US-01 … US-10 all verified. Open minor items, none blocking:
- **US-08 B1:** chart a11y node bounds offset in Today/30d (touch selection is correct).
- **US-08:** the per-hour readout still wraps to two lines.
- **US-06:** the countdown a11y label is minutes-only (designer to confirm).
- Seed data dates "today" events relative to the current time now; the earlier future-dated rows are gone.

## Final emulator state
App **Stopped**, on **Home**, Theme **System**, system dark mode **off**, Short intervals ON and Auto-pause ON (debug defaults), Quiet hours ON 22:00–07:00 (simulation OFF), sample history seeded, Level 2 · 2/3 from the smoke run, notifications + exact alarms granted.
