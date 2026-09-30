# QA report — US-01 App shell, Home skeleton & theming

- **Build:** debug, installed on `Pixel 9` AVD (API 37.2), driven by taps in Android Studio "Running Devices". Build gate (assembleDebug, assembleRelease, testDebugUnitTest) was already green.
- **Date:** 2026-09-30 · **Tester:** QA agent
- **Result:** 4/4 PASS (2 of them with caveats) · 0 FAIL. No functional bugs found.

## Acceptance criteria

| # | Criterion | Result | Evidence | Notes |
|---|-----------|--------|----------|-------|
| 1 | Fresh install → Home shows app name, greeting "Hi there", level card placeholder "Level 1 · 0/3" and a **Start** button | PASS | [01-home-light-first-launch.png](01-home-light-first-launch.png), [../US-02/15-home-after-reset.png](../US-02/15-home-after-reset.png) | Title "SuperUnclench", "Hi there", "Ready when you are.", LEVEL / "Level 1 · 0/3", "Stopped" chip, full-width Start. Tapping Start does nothing and nothing crashes, which this story allows. The app was already installed, not freshly installed. I checked the first-launch state again after **Reset all data**. |
| 2 | Bottom nav shows **Home / Stats / Settings**; each tab opens its placeholder screen | PASS | [02-stats-light.png](02-stats-light.png), [03-settings-light.png](03-settings-light.png), [05-stats-dark.png](05-stats-dark.png), [06-settings-dark.png](06-settings-dark.png) | The template Favorites/Profile tabs are gone. Stats shows the empty state ("No check-ins yet" / "Tap Start on Home to begin."). Settings shows the disabled placeholder rows (Your name, Theme, About & disclaimer). Tab state is kept: the QA panel was still open when I came back to Settings. |
| 3 | Dark mode → every screen uses the dark theme with readable contrast; same for light | PASS (caveat) | Light: 01, 02, 03 · Dark: [04-home-dark.png](04-home-dark.png), 05, 06, [07-disclaimer-dismissed-dark.png](07-disclaimer-dismissed-dark.png) | I tested dark mode with the in-app override (QA panel Theme → Dark) because a click-only session can't switch the system dark setting. **System** mode picked up the light system theme correctly ("System (light)" in the readout). Sampled pixels match the tokens: light surface ≈#F9F9FF, cards ≈#EDEDF4, banner ≈#E7E8EE, primary ≈#3D5F90; dark surface ≈#111318, cards ≈#1D2024, banner ≈#282A2F, primary ≈#A7C8FF. Status-bar icons follow the app theme. `Color.kt` matches design-system §2.2/§2.3 exactly. |
| 4 | First launch → the one-line disclaimer shows on Home, "Got it" dismisses it and it stays dismissed | PASS (caveat) | 01 (visible), 07 (after Got it), [../US-02/10-readout-dark-disclaimer-dismissed.png](../US-02/10-readout-dark-disclaimer-dismissed.png) | Disclaimer copy is exact. After "Got it" the banner is gone and stays gone across tab switches, and the QA readout shows the persisted flag `Disclaimer: dismissed`. **Not tested:** a cold app restart (process kill), because it can't be done with taps alone. Reset all data brings the banner back, as designed. |

## Bugs
None found.

## Visual deviations from `docs/design/US-01-app-shell.md` (minor, for designer review)
1. **No Today summary tiles (slot E).** Allowed: the design says they "may be deferred to US-08".
2. **Stats nav icon looks the same selected and unselected.** Home and Settings clearly switch to their `_filled` icons. The Stats glyph looks almost identical in both states, although `ic_stats_filled` exists; only the indicator pill tells them apart. Low priority.
3. **Settings disabled placeholder rows** are hard to read in dark mode (06-settings-dark.png). This is expected for disabled rows and will go away once US-09 makes them real rows.
4. Layout measured against the 412dp-wide screenshot: gutter ≈16dp, card gap ≈12–13dp, Start button ≈56dp tall, pill shape, cards have rounded "large" corners, banner uses `surfaceContainerHigh`. **No deviation.**

## Not verified
- Behaviour after a cold restart (disclaimer persistence, no white flash at launch, splash hold).
- System back from Stats or Settings to Home.
- The TalkBack/semantics details in design §7.
- Medium and expanded layouts (navigation rail).
