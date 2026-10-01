# QA report — US-07 Missed check-ins & auto-pause

- **Build:** the same debug build as the US-06 run (installed via the adb bridge, `Pixel_9(AVD)`, API 37). Driven with `adb shell input` and uiautomator test tags. Notification checks used `dumpsys notification`. The swipe-dismiss is a real swipe.
- **Setup:** Short intervals OFF during this run, so L3 = 15 min and no natural alarms got in the way. Auto-pause ON. QA set the level to L3 2/4.
- **Date:** 2026-10-01 · **Tester:** QA agent
- **Result:** 4 PASS · 0 FAIL. No bugs.

## Acceptance criteria

| # | Criterion | Result | Evidence | Notes |
|---|-----------|--------|----------|-------|
| 1 | L3 2/4 with a pending check-in; another alarm fires → the first is Missed, the level stays L3 2/4, and only one notification is visible | PASS | [01](01-second-fire-alarm-snackbar.png), [02](02-readout-misses1-L3-2of4-events.png), [03](03-home-pending-missed-hint.png) | Start, then **Fire alarm now** ×2. Readout: `3 · sub 2/4`, `Misses 1 consecutive`, `Pending yes (fired 09:51:27)`. Last events show `09:51:27 M replaced L3 2 → L3 2`. `dumpsys notification` lists exactly **one** NotificationRecord (id 1001, channel `checkins`). Home's pending card shows the optional hint "Missed the last one — that's fine." (`pending_missed_hint`). |
| 2 | 2 consecutive misses + Good → consecutive-miss count resets to 0 | PASS | [04](04-readout-misses-reset-after-good.png) | After another Fire alarm now the readout showed `Misses 2 consecutive` (uiautomator log). I answered **Good** on the Home card: `sub 3/4`, `Pending no`, **`Misses 0 consecutive`**. |
| 3 | 3 consecutive misses → auto-pause, Home shows "Paused — you missed 3 check-ins" with **Resume**, and no further alarms fire | PASS | [05](05-shade-empty-after-autopause.png), [06](06-home-autopause-banner.png), [07](07-fire-alarm-not-running.png), [08](08-resume-check-ins-resumed.png); dark [13](13-home-autopause-banner-dark.png) | Fire alarm now ×4 (one pending + 3 replacements). The readout goes to `Paused (auto)`, `Misses 3 consecutive`, `Next alarm —`, `Pending no`. **The notification is removed on auto-pause:** `dumpsys` shows 0 SuperUnclench notifications and the shade is empty. That covers the US-05 O2 concern and the Decisions-log item. Home: chip **Paused** and an inner banner (`autopause_banner`) reading "Paused — you missed 3 check-ins. Resume?" / "No worries — we stopped so we won't nag you while you're away.", with **Resume** and **Stop**. Fire alarm now → snackbar **"Not running"**. **Resume** → Running, countdown "14:57" (the full 15-min interval minus tap latency), snackbar **"Check-ins resumed"**, readout `Misses 0`, next alarm +15 min. |
| 4 | Swipe-dismiss a notification → counts as Missed | PASS | [09](09-shade-before-swipe.png) → [10](10-shade-after-swipe-dismiss.png), [11](11-readout-M-dismissed.png); QA path [12](12-qa-mark-missed-snackbar.png) | **Real swipe:** Fire alarm now, expand the shade, then `input swipe` the SuperUnclench row to the right. The notification is gone (`dumpsys` count 0), the readout shows `Pending no`, `Misses 1 consecutive`, and Last events shows **`09:56:45 M dismissed L3 3 → L3 3`** (level unchanged), so the `deleteIntent` works. The debug **Mark missed** path gives "Marked missed (2 in a row)", and pressing it again with nothing pending gives "No pending check-in" (copy matches §4). |

## Bugs
None.

## Visual check vs `docs/design/US-07-missed-and-auto-pause.md`
- The auto-pause card matches §2: a `tertiaryContainer` inner banner with the schedule icon, exact title and body copy, and a filled **Resume** + outlined **Stop** below. It sits inside the session card, not at the top of Home. Light [06](06-home-autopause-banner.png) and dark [13](13-home-autopause-banner-dark.png).
- Missed stays neutral throughout: no red, grey Missed tile (the "Today" tiles on Home belong to US-08 and were not reviewed here).
- No notification is posted for the auto-pause itself. Confirmed (shade empty).

## Final state (after US-06 + US-07)
Session **Stopped**, app on **Home**, Theme **System (light)**, **Level 1 · 0/3**, Auto-pause **ON**, Short intervals **ON** (debug default restored), previews OFF, **system dark mode OFF** (`cmd uimode night` → no), notifications + exact alarms granted ([14](14-final-home-stopped-L1-system.png)).
