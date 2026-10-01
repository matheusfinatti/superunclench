# QA report — US-06 Next-alarm countdown, Pause & Resume

- **Build:** debug, `gradle :app:installDebug` via the adb bridge ("BUILD SUCCESSFUL", installed on `Pixel_9(AVD)`, API 37). Short intervals ON (1 min = 1 s). The device was driven through `adb shell input` and uiautomator test tags. Screenshots come from `screencap` at 1080×2424.
- **Date:** 2026-10-01 · **Tester:** QA agent
- **Result:** 5 PASS · 0 FAIL. All extra checks pass. There are 2 minor notes (a11y label precision, and a tooling hazard).

## Acceptance criteria

| # | Criterion | Result | Evidence | Notes |
|---|-----------|--------|----------|-------|
| 1 | Running → Home shows "Next check-in in mm:ss" counting down, and it matches the readout | PASS | [03](03-L8-running-countdown-light.png), [04](04-readout-next-alarm-L8.png), [05](05-countdown-ticking-later.png); dark [14](14-running-countdown-dark.png) | Auto-pause was OFF and the level set to 8 (short interval 180 s). Home showed overline **NEXT CHECK-IN**, "in **02:59**", "at 9:38 AM". About 13 s later the readout showed `Next alarm 09:38:25 (in 02:46)`, and a few seconds after that Home showed "in 02:40". All three agree and the countdown ticks. At L1 the first dump read "Next check-in in 3 seconds, at 9:32 AM", so it counts down from 00:05 as expected. |
| 2 | Pause → **1 h** → "Paused until HH:MM" + **Resume**, and the pending alarm is cancelled | PASS | [06](06-pause-picker-chips.png) → [07](07-paused-1h-snackbar.png), [08](08-readout-paused-next-dash.png); dark [15](15-pause-picker-dark.png), [16](16-paused-until-dark.png) | The inline picker "Pause check-ins for…" shows the chips **15 min / 1 h / Until I resume** and a **Cancel** button. After 1 h: chip **Paused**, **PAUSED UNTIL 9:37 AM** (60 s ahead in short mode), "Check-ins resume automatically.", **Resume** (filled) + **Stop**, and the snackbar "Paused until 9:37 AM". The a11y label is "Paused until 9:37 AM". The readout shows `Paused · Next alarm — · Pause until 09:37`. |
| 3 | Paused "Until I resume" → **Resume** → countdown shows the full current interval | PASS | [09](09-paused-until-you-resume.png) → [10](10-resume-full-interval-03-00.png) | "Paused until you resume" / "Your level is kept." and the snackbar "Paused until you resume". After Resume: Running, "in **03:00**" (the full L8 interval of 180 s), then 02:58. |
| 4 | Timed pause ends (15 min = 15 s) → resumes automatically and the countdown reappears | PASS | [11](11-paused-15min-15s.png) → [12](12-auto-resumed-snackbar.png), [12b](12b-auto-resumed-snackbar.png) | The pause showed "PAUSED UNTIL 9:38 AM". About 15 s later the card was Running with "in 03:00", then 02:58, and the snackbar "Check-ins resumed". A uiautomator log at +16 s also showed `Running` + "Check-ins resumed". |
| 5 | Paused → **Stop** → the session ends and Start is shown | PASS | [13](13-stop-from-paused-snackbar.png) | From "Paused until you resume" → Stop: chip **Stopped**, **Start** button, and the snackbar "Check-ins stopped. Your level is saved." The level card is unchanged (L8). |

## Extra checks requested

| Check | Result | Evidence |
|-------|--------|----------|
| **B1b (US-05):** Kill app reachable by tap | **FIXED.** The pinned jump-chip row (State · Profile · Session · Level · History · Perms · Danger) works. Danger → "Kill app (test persistence)" is on screen and tappable. | [02](02-qa-jump-chips-danger-kill-visible.png) |
| Pill notification in a **dark system shade** (`cmd uimode night yes`) | OK. The heads-up and the expanded shade both render the custom pills with dark-theme colours (lighter green/red fills, dark text), and the new calm-face icon shows. System dark mode was switched back off afterwards. | [17](17-headsup-pills-dark-system.png), [18](18-shade-pills-dark-system.png) |
| **Real** notification-permission revoke → banner → re-grant | OK. `pm revoke … POST_NOTIFICATIONS` (granted=false) → app opened → banner "Notifications are off — check-ins can't reach you." → **Open settings** opened the system SuperUnclench notification page (toggle off) → toggled on → Back: banner gone, and `granted=true` in dumpsys. The `checkins` channel kept importance 4 (HIGH), the default sound and vibration `[0, 250, 150, 250]`. That also covers the sound/vibration item left open since US-03. | [19](19-real-revoke-banner.png), [20](20-open-settings-system-notif-page.png), [21](21-system-notifications-reenabled.png), [22](22-back-banner-gone.png) |
| Snackbar corner radius 8dp | OK. Measured about 21 px corner radius at 1080 px width (density 2.625), which is 8dp. | [13](13-stop-from-paused-snackbar.png) |
| No outline on the answer-confirmation row | OK. "Nice — noted." on a green container, no border. | [23](23-answer-confirmation-row-light.png) |
| **Real `adb reboot`** (US-05 follow-up) | OK. Before the reboot: Running, next 09:47:01. After the reboot, before the app was launched, `dumpsys alarm` shows an `RTC_WAKEUP` `FIRE_CHECK_IN` for 09:47:01 with `exactAllowReason=permission`, so the boot receiver rescheduled the exact alarm. After launch the readout showed Running with the next alarm still 09:47:01. At 09:47 the check-in fired (pending card "CHECK-IN · 9:47 AM"), and only one notification was posted. | [24](24-readout-before-real-reboot.png), [25](25-dumpsys-alarm-after-reboot.txt), [26](26-readout-after-real-reboot.png), [27](27-alarm-fired-after-reboot.png) |
| New launcher/notification icon | The calm face on light blue now also shows in notifications, system settings and the Alarms page. | [17](17-headsup-pills-dark-system.png), [20](20-open-settings-system-notif-page.png) |

## Bugs
None found against US-06.

## Notes / minor deviations
1. **Countdown a11y label precision.** The label reads "Next check-in in 2 minutes, at 9:38 AM" (and "in 3 seconds" under a minute). Design §7 shows "in 12 minutes 34 seconds", but the label is updated at most every 60 s, so minutes-only is a reasonable reading. The Decisions log only fixes the phrase "Next check-in in …", so this is acceptable. Designer to confirm.
2. **Tooling hazard, not an app bug.** With Auto-pause OFF and 5 s intervals, a new heads-up re-alerts every 5 s and covers the top ~470 px, which includes the pinned jump chips ([28](28-headsup-covers-jump-chips.png)). Two of my chip taps landed on the heads-up's **Bad** pill and recorded real Bad answers. Without Auto-pause, misses pile up quickly ([29](29-autopause-off-misses-accumulate.png): 9 consecutive). This only matters at seconds scale. A future QA option could be to suppress heads-up while the QA panel is open.
3. The 15-min pause in short mode shows "Paused until 9:38 AM" at minute resolution, even though it actually lasts 15 s. Expected, given the format.

## State left for US-07
Running at L8 with a pending check-in (it fired after the reboot), Auto-pause OFF, system dark mode OFF, app theme System. US-07 starts from here.
