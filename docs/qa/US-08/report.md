# QA report — US-08 Stats: Good/Bad charts with time frames

- **Build:** latest debug build installed via the adb bridge (`gradle :app:installDebug`, BUILD SUCCESSFUL) on `Pixel_9(AVD)`, API 37. Started with Danger → **Reset all data**, after which Short intervals and Auto-pause were both ON.
- **Data:** QA panel → History → **Seed sample history (30 days)** → snackbar "Seeded 30 days (309 events)" ([01](01-seed-history-snackbar.png)).
- **Date / time:** 2026-10-01, device clock about 10:16 BST. **Note:** the seed puts today's events at fixed times up to 17:35, so at test time 4 of today's events (12:20, 14:25, 15:30, 17:35) were **future-dated**. They appear in the Today chart and at the top of the recent list ([02](02-stats-today-light.png), [06](06-recent-checkins-list-a.png)). This is an artefact of the seed, not an app bug.
- **Tester:** QA agent
- **Result:** 7 PASS · 0 FAIL. There is 1 minor accessibility bug (B1) and a few notes.

## Acceptance criteria

| # | Criterion | Result | Evidence | Notes |
|---|-----------|--------|----------|-------|
| 1 | Stats has a **Today / 7 days / 30 days** selector, with Today selected by default | PASS | [02](02-stats-today-light.png), [18](18-stats-cold-open-today-default.png) | A segmented selector with a check on the selected item. On a cold start (force-stop + relaunch) Stats opens on **Today**. Within a session the last chosen frame is kept when switching tabs (it came back on "7 days"), which is fine. |
| 2 | Today = bars **per hour**; 7d / 30d = bars **per day**; tapping a chip updates the chart | PASS | [02](02-stats-today-light.png), [04](04-stats-7days-light.png), [05](05-stats-30days-light.png) | Today: title "Check-ins per hour", 24 bars. Bars at 08 (Bad), 09 and 10 (Good), 12 (Bad), 14, 15 and 17 (Good), plus 11:00 Missed only, which has no bar. 7 days: "Check-ins per day", **7 bars** Fri…Today, and **Sun Sep 27 (4 days ago) is empty**. 30 days: **30 bars** (`chart_bar_*` count = 30). |
| 3 | Summary tiles **Good / Bad / Missed / Good rate %** for the selected frame | PASS | [02](02-stats-today-light.png), [04](04-stats-7days-light.png), [05](05-stats-30days-light.png) | Today **5 / 2 / 1 / 71%**; 7 days **45 / 12 / 1 / 79%**; 30 days **188 / 98 / 23 / 66%**. All match the expected seed values. The good rate excludes Missed, per the Decisions log. |
| 4 | **Streak** tile with the known seeded value | PASS | [02](02-stats-today-light.png) | "STREAK 4 days — Days in a row with a Good rate of 50%+". The value is the same in every frame. |
| 5 | Empty history → empty state "No check-ins yet — tap Start on Home" | PASS | [17](17-stats-empty-state.png) | After History → **Clear history**: "No check-ins yet — tap Start on Home to begin." plus a **Go to Home** button, which navigates to Home. The Home TODAY row then reads 0/0/0. |
| 6 | Legible in light and dark; Good/Bad distinguishable without colour (legend + pattern or labels) | PASS | Light [02](02-stats-today-light.png), [04](04-stats-7days-light.png), [05](05-stats-30days-light.png); dark [08](08-stats-today-dark.png), [09](09-stats-7days-dark-bar-selected.png), [10](10-recent-list-dark.png) | There is a legend (■ Good, ▨ Bad), **Bad segments are hatched**, and tiles and list rows carry ✓/✕/clock icons plus words. Dark mode keeps contrast and the hatch. |
| 7 | **Recent history** list: the last 20 events with time, Good/Bad/Missed, and level at the time | PASS | [06](06-recent-checkins-list-a.png), [07](07-recent-checkins-list-end.png), dark [10](10-recent-list-dark.png) | "Recent check-ins": `history_row_0`…`history_row_19` = **20 rows**, newest first. Each row has an icon, the outcome word, "Level 6 · Grounded" (the level at that time) and the time ("5:35 PM", "Yesterday 2:45 PM" …). |

## Other checks

| Check | Result | Evidence |
|-------|--------|----------|
| Tap a bar / ‹ › shows exact counts | OK. A tooltip appears over the bar, and the readout under the chart shows e.g. "Mon, Sep 28 · 8 Good · 2 Bad · 0 Missed". ‹ › step one bar at a time. **But see B1** for the per-hour view. | [03](03-today-bar-tapped-readout.png), [09](09-stats-7days-dark-bar-selected.png) |
| Home **TODAY** row, tap → Stats | OK. The tiles read GOOD 5 / BAD 2 / MISSED 1, with the "TODAY" overline unclipped. The row's a11y label is "Today: 5 Good, 2 Bad, 1 Missed. Open stats." and tapping it opens Stats. | [11](11-home-today-tiles.png), [12](12-today-row-tap-opens-stats.png) |
| Designer: about 1 s after a level jump, is "of 8" aligned? | OK. In captures at roughly 0.3 s, 1 s and 2.5 s after the L1 → L2 promotion, "of 8" sits on the number's baseline throughout, and the "Level up!" message shows. | [13](13-level-jump-t0.png), [14](14-level-jump-t1s.png), [15](15-level-jump-t2s.png) |
| Designer: pause-picker spacing | The chips 15 min / 1 h / Until I resume are evenly spaced with Cancel right-aligned. I took the reference from the US-06 run on the same layout: [US-06/06](../US-06/06-pause-picker-chips.png), dark [US-06/15](../US-06/15-pause-picker-dark.png). A new capture here was beaten by the 10 s short-interval alarms, which auto-paused the session instead ([16](16-autopause-card-today-tiles-updated.png)). | — |
| Designer: TalkBack auto-pause announcement | **Partly verified.** `autopause_banner` exposes the merged label "Paused — you missed 3 check-ins. Resume? No worries — …" (uiautomator), and the code sets `liveRegion = Polite` on it (`SessionCard.kt` `AutoPausedBanner`). I didn't run TalkBack itself, so the spoken announcement is unverified. | US-07 [06](../US-07/06-home-autopause-banner.png) |

## Bugs

| # | Severity | Title | Steps | Expected | Actual |
|---|----------|-------|-------|----------|--------|
| B1 | Minor (a11y) | Per-hour chart: each bar's accessibility node is about one bar to the left of the drawn bar | Stats → Today. Dump the UI and tap the centre of the `chart_bar_12` node (12:00–13:00, "0 Good, 1 Bad"). | The readout shows 12:00 PM–1:00 PM. The TalkBack focus outline sits on the drawn 12 o'clock bar. | The readout shows **11:00 AM–12:00 PM**, and the same happens for every hour I tested (nodes 8, 9, 12, 13, 17 each select the previous hour, [03](03-today-bar-tapped-readout.png)). Comparing with the drawn bars: node `chart_bar_0` is centred at x≈139, while the drawn 00:00 bar is at x≈183. The semantics rectangles appear to ignore the y-axis label width. The last node (23) is wider and absorbs the difference. The 7- and 30-day views select the right day when tapping node centres (bars 0, 3, 6 checked), but they likely share the same offset. Fix: give the per-bar semantics nodes the drawn-bar x offset. |

## Notes
1. The chart a11y summary "Highest: 8:00 AM–9:00 AM with 0 Good and 1 Bad" picks the first of several equal hours. That reads a little oddly when another hour has a Good; consider "Busiest" or ranking by Good.
2. The readout under the per-hour chart wraps to two lines ("… · 0 Bad · 1 / Missed") on a 1080 px screen ([03](03-today-bar-tapped-readout.png)). Cosmetic.
3. **Environment change:** the app was reinstalled under a new UID (the instrumented-test run, probably uninstall + install), so **notification and exact-alarm grants were reset**. I re-granted notifications through the in-app rationale → system dialog (Allow). **Exact alarms are currently denied** on the emulator, so the exact-timing banner shows while running.
4. The seeded data also exercises Missed rows and the Missed tile (neutral grey clock), in line with US-07.

## Final state
History re-seeded (Today 5/2/1), session Stopped. Level and theme are reset at the end of the US-09 run (see `../US-09/report.md`).

---

## Re-check 2026-10-01 (afternoon build): B1 fix and follow-ups

Build: latest debug build via the adb bridge (reinstalled 14:30). Seeded with **Seed sample history** at 14:48.

| Item | Result | Evidence |
|------|--------|----------|
| **Touch** on a bar selects that bar (Today / 7d / 30d) | **FIXED.** I tapped the *drawn* bar centres, located from the screenshot pixels. Today: x=357 → "5 AM–6 AM · 0 Good · 1 Bad", x=426 → 7 AM, x=564 → 11 AM, x=667 → 2 PM, x=960 → 10 PM (empty). 30d: first bar → "Wed, Sep 2", middle → "Thu, Sep 17", last → "Thu, Oct 1". Every tooltip and readout matches the bar under the finger. | [19](19-b1-recheck-today.png), [22](22-b1-30d-last-bar-selected.png), [23](23-b1-today-11am-one-line-readout.png) |
| **Accessibility node bounds** match the drawn bars | **7 days: FIXED.** The node centres (223, 342, 460, 578, 696, 815, 937) match the drawn bar centres (226, 345, empty, 581, 699, 818, 936) to within 3 px. **Today and 30 days: STILL OFFSET.** `chart_plot` spans x 168–996 and the drawn 5 AM bar is centred at x≈357, but node `chart_bar_5` is `[295,330]`, about 45 px (≈1.3 bars) to the left, and tapping its centre selects 4 AM. 30d: `chart_bar_0` is `[119,147]` while the drawn first bar is centred at ≈182, an offset of about 49 px. The last node absorbs the difference (`chart_bar_23` `[916,1042]`, `chart_bar_29` `[919,1045]`). The 7-day nodes start at x=164, close to the plot's left edge, so the offset seems to grow with the bar count. That points to the per-bar slot being computed from a different width or origin than the drawn bars. **Impact:** TalkBack focus rectangles in Today and 30d sit about one bar left of the bar they describe. Double-tapping still runs the node's own action, so the right bar is probably selected; I didn't verify that with TalkBack. **B1 stays open (minor, a11y)** for Today/30d. | [19](19-b1-recheck-today.png), [20](20-b1-recheck-7d.png), [21](21-b1-recheck-30d.png) |
| "Busiest" wording | OK: "Busiest: 2 PM–3 PM with 1 Good and 0 Bad" (Today) / "Busiest: Wed, Sep 30 …" (7d) | uiautomator |
| One-line readout "11 AM–12 PM" | **Partly done.** The shorter hour format is in ("11 AM–12 PM"), but on Today the readout still wraps: "11 AM–12 PM · 1 Good · 0 Bad · 0 / Missed". Day frames fit on one line. Cosmetic. | [23](23-b1-today-11am-one-line-readout.png), [22](22-b1-30d-last-bar-selected.png) |
| No frame selector after Clear history | OK. The empty state shows only "No check-ins yet — tap Start on Home to begin." + **Go to Home**, and the Today/7d/30d selector is gone. | [24](24-b1-empty-state-no-frame-selector.png) |
| Seeded Today: no bars after the current hour, totals 5/2/1 | OK. At 14:48, today's seeded events fall between 05:00 and 14:59 (5 AM Bad, 6/7 AM Good, 8 AM Missed, 9 AM Bad, 11/12 AM/PM and 2 PM Good). Nothing is drawn after 3 PM. Tiles are 5 / 2 / 1 / 71%, the streak is 4 days, and 7d/30d totals are unchanged (45/12/1, 188/98/23). | [19](19-b1-recheck-today.png) |
| Selected bar highlight | A new nicety: the selected bar stays saturated and the others dim. › is disabled on the last bar. | [22](22-b1-30d-last-bar-selected.png), [23](23-b1-today-11am-one-line-readout.png) |

---

## B1 re-check #2 (2026-10-01, 15:56 build; fix: 48dp min touch target no longer enlarges bar a11y boxes)

| Check | Result | Evidence |
|-------|--------|----------|
| Today: `chart_bar_i` bounds vs drawn slots | **FIXED.** The nodes now tile `chart_plot` [168..996] exactly: `chart_bar_0` [168,203] … `chart_bar_23` [962,996] (34.5 px slots), so every node matches its slot to within ±1 px. Each drawn bar sits inside its own node (e.g. the 6 AM bar drawn at x 382–402 is inside `chart_bar_6` [375,410]), with no bar straddling two nodes. | [25](25-b1r2-today.png), [bounds](25-b1r2-today-bounds.txt) |
| Today: tap node centres | Bars 0, 6, 10, 15 and 23 each selected **their own** hour ("12 AM–1 AM", "6 AM–7 AM · 0 Good · 1 Bad", "10 AM–11 AM · 0 Good · 1 Bad · 1 Missed", "3 PM–4 PM · 1 Good", "11 PM–12 AM"). | [26](26-b1r2-today-bar15-selected-one-line.png) |
| 30 days | **FIXED.** 30 nodes from [168,196] to [968,996]; all 29 drawn bars sit inside their own slot. Taps on nodes 0, 3, 29 → "Wed, Sep 2", "Sat, Sep 5", "Thu, Oct 1". | [27-30d](27-b1r2-30d.png), [bounds](27-b1r2-30d-bounds.txt) |
| 7 days | Still aligned. 7 nodes from [168,286] to [878,996]; 6 drawn bars inside their slots (Sun empty). Taps on nodes 0, 3, 6 → Fri Sep 25, Mon Sep 28, Thu Oct 1. | [27-7d](27-b1r2-7d.png), [bounds](27-b1r2-7d-bounds.txt) |
| Today readout on one line | **FIXED.** `chart_readout` is a single line (bounds [210,2073][870,2126], 53 px tall) for "10 AM–11 AM · 0 Good · 1 Bad · 1 Missed". | [26](26-b1r2-today-bar15-selected-one-line.png) |

**B1 closed.**
