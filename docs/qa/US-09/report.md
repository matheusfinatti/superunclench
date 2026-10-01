# QA report — US-09 Settings: name, theme, reset

- **Build:** the same debug build as the US-08 run (adb bridge install), `Pixel_9(AVD)`, API 37. Real typing used `adb shell input text`. Cold start was captured with on-device `screenrecord`.
- **Date:** 2026-10-01 · **Tester:** QA agent
- **Result:** 5 PASS · 0 FAIL. No bugs. US-03 B1 (white launch window under the in-app Dark theme) is **fixed**.

## Acceptance criteria

| # | Criterion | Result | Evidence | Notes |
|---|-----------|--------|----------|-------|
| 1 | Settings shows **Your name** (text field), **Theme** (Light / Dark / System), **Reset progress**, **About & disclaimer** | PASS | [01](01-settings-screen-light.png) | Sections are Profile ("Your name", helper "Shown in your greeting on Home."), Appearance (a segmented Light / Dark / System control), Progress ("Reset progress — Go back to Level 1. History is kept." in error colour), About ("About & disclaimer · Version 1.0"), and Developer (debug only). |
| 2 | Name "Alex" → Home "Hi, Alex"; no name → "Hi there" | PASS | [02](02-name-field-typed-alex.png) → [03](03-home-hi-alex.png); cleared [04](04-home-hi-there-cleared.png); cap [05](05-name-24-char-cap.png) | Typed for real into `settings_name_field` (`input text Alex`), and the greeting became **"Hi, Alex"**. Deleting the text brings back **"Hi there"**. Typing 30 characters keeps **24** ("ABCDEFGHIJKLMNOPQRSTUVWX") with a **24/24** counter. The field has a clear (✕) button. |
| 3 | **Dark** switches immediately and stays dark after relaunch regardless of the system setting; **System** follows the device | PASS | [06](06-theme-dark-immediate.png); cold start [07 (mp4)](07-cold-start-dark-theme-system-light.mp4), [08](08-cold-start-frames-no-white-flash.png), [09](09-after-relaunch-still-dark-settings.png); System [10](10-theme-system-device-light.png), [11](11-theme-system-device-dark.png), [12](12-theme-system-device-light-again.png); Light forced [13](13-theme-light-forced-device-dark.png) | Tapping Dark re-themes at once, with the system still light (`cmd uimode night` → no). Then force-stop → launcher → `am start` was recorded at about 26 fps. The splash is **dark with the calm-face icon**, followed by dark Home. **No white flash:** the peak mean brightness of the app area across all 70 frames was ≤ 64/255, the launcher's own level, and no frame was light. After the relaunch, Settings is still Dark. **System**: follows `cmd uimode night yes/no` live, dark then light. **Light** stays light while the device is dark. |
| 4 | **Reset progress** → confirmation; **Cancel** changes nothing; **Reset** → L1 0/3 and history is kept | PASS | [14](14-reset-confirm-dialog.png), [15](15-after-reset-snackbar.png) | QA set L3 1/4 first. The dialog reads "Reset your progress?" / "You'll go back to Level 1 (0 of 3). Your check-in history stays." with **Cancel** and **Reset** (red). After Cancel the readout still shows `3 · sub 1/4`. After Reset: snackbar "Progress reset to Level 1", readout `1 · sub 0/3`, and **History 309** is unchanged. |
| 5 | **About** shows the not-medical-advice text and the app version | PASS | [16](16-about-disclaimer-version.png) | The calm-face icon, "SuperUnclench", "**Version 1.0 (1)**", the disclaimer card "SuperUnclench is an awareness tool, not medical advice. If you have jaw pain, talk to a dentist or doctor.", and a "How it works" paragraph. A back arrow returns to Settings. |

## Notes
- The cold-start recording also shows the **new launcher icon** (calm face on light blue) in the home-screen dock ([08](08-cold-start-frames-no-white-flash.png), first frame).
- The name is saved as you type. There is no Save button, and pressing Back or Enter keeps the value.

## Final state (after US-08 + US-09)
App **Stopped** on **Home**, greeting "Hi there", Theme **System**, **Level 1 · 0/3**, sample history seeded (309 events), Short intervals ON, Auto-pause ON, system dark mode **off** ([17](17-final-home-L1-stopped-system.png)). Note: after the instrumented-test reinstall, **exact alarms are denied** on the emulator. Notifications were re-granted.
