# QA report — US-02 Developer / QA panel (debug builds only)

- **Build:** debug, installed on `Pixel 9` AVD (API 37.2). Build gate already green (assembleDebug, assembleRelease, testDebugUnitTest).
- **Date:** 2026-09-30 · **Tester:** QA agent
- **Result:** 7 PASS · 1 NOT TESTABLE (1b release build; static check OK) · 0 FAIL. No functional bugs found.

## Acceptance criteria

| # | Criterion | Result | Evidence | Notes |
|---|-----------|--------|----------|-------|
| 1a | Debug build: Settings has a **"Developer / QA"** entry and tapping it opens the panel | PASS | [01-settings-entry-debug.png](01-settings-entry-debug.png), [02-panel-readout-light.png](02-panel-readout-light.png) | "DEVELOPER" header, bug icon, "Developer / QA" with "Debug build only" and a chevron. The panel opens inside the Settings tab with a back arrow, and the nav bar stays visible. |
| 1b | Release build: the entry does not exist | NOT TESTABLE (static check OK) | — | No release APK is installed, and none is in `app/build/outputs`. **Code review:** `src/release/.../DebugToolsProvider.kt` returns `NoOpDebugTools` (`isAvailable = false`, empty `SettingsEntry`/`Panel`), and all panel code is under `src/debug`. Installing a release build and checking Settings is still needed. |
| 2 | Panel is a list of buttons, each ≥48dp | PASS | [03-profile-section-light.png](03-profile-section-light.png), [11-session-section-dark.png](11-session-section-dark.png), [13-danger-zone-before-reset-dark.png](13-danger-zone-before-reset-dark.png) | The accordion sections are Profile & appearance, Session & alarms and Danger zone, and only one section is open at a time. Buttons measure about 48dp tall and are laid out 2 per row. |
| 3 | **Set name: "Alex"** / **Clear name** update the Home greeting | PASS | [04-set-name-snackbar.png](04-set-name-snackbar.png), [05-home-hi-alex.png](05-home-hi-alex.png), [06-clear-name-snackbar.png](06-clear-name-snackbar.png), [09-home-name-cleared-dark.png](09-home-name-cleared-dark.png) | The Home greeting changes to "Hi, Alex" and back to "Hi there". The readout Name field updates at the same time. |
| 4 | **Toggle dark mode** cycles System→Light→Dark→System, re-themes the whole app immediately and shows the current value | PASS | [07-theme-light-snackbar.png](07-theme-light-snackbar.png), [08-theme-dark-snackbar.png](08-theme-dark-snackbar.png), [17-theme-cycle-back-to-system.png](17-theme-cycle-back-to-system.png) | The button label ("Theme: X") and the readout ("System (light)" / "Light" / "Dark") both update. The app re-themes immediately, status bar included. |
| 5 | **Reset all data** → L1/0, no history, not running, name cleared, disclaimer shown again | PASS | [13-danger-zone-before-reset-dark.png](13-danger-zone-before-reset-dark.png) → [14-reset-all-data-snackbar.png](14-reset-all-data-snackbar.png), [15-home-after-reset.png](15-home-after-reset.png) | Before: Name Alex, Disclaimer dismissed, Theme Dark. After: Name —, Disclaimer shown, Theme System, Session Stopped, Level 1 · sub 0/3, History 0, and Home shows "Hi there" with the disclaimer back. **Caveat:** level and history were already L1/0 and 0 before the reset, because no action that changes them exists yet (US-04/US-08). Their reset path is untested and has to be re-checked when those stories land. |
| 6 | **State readout** at the top shows running/paused/stopped, level/sub-level, next alarm time and consecutive misses | PASS | [02-panel-readout-light.png](02-panel-readout-light.png), [10-readout-dark-disclaimer-dismissed.png](10-readout-dark-disclaimer-dismissed.png) | The readout shows Session, Level, Interval, Next alarm, Pending, Misses, Pause, Short int., Quiet hours, Notif perm / Exact, Theme, Name, Disclaimer and History, with monospace values. Only the stopped state can be reached today, so the running, paused and next-alarm values are untested. |
| 7 | Every action shows a brief snackbar confirming it ran | PASS | 04, 06, 07, 08, [14](14-reset-all-data-snackbar.png), [16-show-disclaimer-again-snackbar.png](16-show-disclaimer-again-snackbar.png) | Confirmed copy: "Name set to Alex", "Name cleared", "Theme: Light", "Theme: Dark", "Theme: System", "All data reset", "Disclaimer will show on Home". The copy matches design §4 exactly. **Open notification shade** opens the real shade ([12-notification-shade-opened.png](12-notification-shade-opened.png)), which hides its "Opening shade" snackbar, so I couldn't see it. I closed the shade with the emulator Back button. "Reset progress (no confirm)" was not tapped because it belongs to US-09. |

Extra check: **Show disclaimer again** works. The readout goes back to `Disclaimer: shown` and the snackbar appears (16).

## Bugs
None found.

## Visual deviations from `docs/design/US-02-qa-panel.md` (minor)
1. **Readout layout.** The design pairs some fields on one line ("Theme System (dark) Name: Alex", "Disclaimer dismissed History: 214"). The build puts each field on its own row, which makes the card about 2 rows taller. It still fits at 1.0 font scale. Cosmetic.
2. **Session & alarms section.** It currently holds only "Open notification shade", as one full-width button. That is fine for now; the other buttons belong to later stories.
3. **Snackbar position.** The snackbar sits just above the nav bar and briefly covers the last section header ("Danger zone"). It doesn't block buttons in any state I captured. Cosmetic.
4. Section headers use `titleSmall` in `primary` with a trailing chevron. Danger-zone buttons are outlined with `error` content colour. Other buttons are `FilledTonalButton` in `secondaryContainer`. **Matches the design.**

## Not verified
- That the state card stays pinned while the list scrolls. I didn't try it, because scrolling is not a tap and the panel fits on one screen.
- The accordion's `rememberSaveable` state. It survives tab switches, but resets when leaving the panel with its own back arrow, which is expected behaviour.
- Release build: the entry is absent (criterion 1b).

## Tooling note
The first two background taps on "Stats" did not register, because the emulator canvas ignores AX-synthesized clicks. Every tap after full-screen control was granted worked. This was a tooling issue, not an app bug.
