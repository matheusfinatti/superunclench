# SuperUnclench — Product Spec & User Stories (v1)

Owner: PM · Last updated: 2026-09-30 · Package: `com.mfinatti.noclenchingsrs` (minSdk 24, targetSdk 37)

## 1. Product summary

SuperUnclench is an **"Anki for jaw clenching"**: a spaced-repetition awareness trainer. The user taps **Start**, and the app sends periodic check-in alarms (regular notifications, *not* full-screen). Each asks one question — *"Were you clenching just now?"* — answered with **Good** (green, "I wasn't clenching") or **Bad** (red, "I was clenching"). Good answers move the user up SRS sub-levels and levels, so check-ins get rarer as awareness improves; Bad answers move them back down.

**Target user:** adults who notice (or have been told) they clench their jaw during the day — desk workers, students, gamers — and want a lightweight habit/awareness nudge.

**Success signals (v1):** % of alarms answered (vs missed); share of Good answers trending up over 7/30 days; users reaching Level 4+ within a week.

### Principle: not medical advice
- SuperUnclench is a **habit & self-awareness tool**. It does not diagnose, treat or prevent any condition (incl. bruxism/TMD) and makes **no health claims**.
- Copy never uses words like "treat", "cure", "therapy", "symptom", "diagnose".
- A short one-line disclaimer is shown on first launch and in Settings/About: *"SuperUnclench is an awareness tool, not medical advice. If you have jaw pain, talk to a dentist or doctor."*

## 2. SRS spec

### Levels & intervals
Interval = time from an answer (or from Start/Resume) to the next alarm. Sub-levels = number of Good answers needed at that level to promote.

| Level | Name (working) | Interval | Sub-levels (Goods to promote) |
|------:|----------------|---------:|------------------------------:|
| 1 | Warm-up   | 5 min   | 3 |
| 2 | Noticing  | 10 min  | 3 |
| 3 | Aware     | 15 min  | 4 |
| 4 | Steady    | 30 min  | 4 |
| 5 | Relaxed   | 45 min  | 4 |
| 6 | Grounded  | 1 h     | 5 |
| 7 | Loose     | 2 h     | 5 |
| 8 | Unclenched (max) | 3 h | 5 (progress bar shows "Max level"; Goods still counted) |

Total Goods from L1 to L8 with no Bads: 28.

### Rules
- **State:** `level` (1–8) and `subLevel` (0 … sub-levels−1). New users start at **L1, sub-level 0**. State persists across sessions and app restarts.
- **Good:** `subLevel += 1`. If `subLevel` reaches the level's sub-level count → **promote**: `level += 1`, `subLevel = 0`. At L8, Good only records history.
- **Bad:**
  - If `subLevel > 0` → `subLevel = 0` (lose progress in current level, keep level).
  - If `subLevel == 0` → **demote**: `level -= 1` (floor L1), `subLevel = 0`.
  - I.e. two Bads in a row from mid-level = drop one level. Forgiving, but clenching persistently pulls intervals back down.
- **Next alarm** is always scheduled from the moment of the answer, using the interval of the *resulting* level.
- **Only one pending alarm at a time.** A new alarm replaces the previous notification.

### Missed / ignored alarms
- An alarm is **Missed** if it is dismissed (swiped away) or not answered before the next alarm fires.
- Missed = **neutral**: no level/sub-level change; recorded in history as *Missed*; next alarm is scheduled at the **current** interval from the time of the miss.
- **Auto-pause:** after **3 consecutive Missed** alarms the session auto-pauses (no more alarms) and the Home screen shows "Paused — you missed 3 check-ins. Resume?". Prevents nagging a user who's away.
- An answer on an old notification is ignored if a newer alarm has already replaced it (notification is removed anyway).

### Session controls
- **Start:** begins alarms; first alarm at current level's interval (5 min for new users).
- **Pause:** chips *15 min / 1 h / Until I resume*. No alarms while paused; level kept. On resume, next alarm = now + current interval.
- **Stop:** ends the session, cancels pending alarm & notification. Level, sub-level and history are kept.
- **Reset progress** (Settings, with confirmation dialog): back to L1/0; history kept.

### Quiet hours (Should — US-10)
- Default **ON, 22:00–07:00** (preset choices, see US-10). An alarm due inside quiet hours is deferred to quiet-hours end. No alarm, no Missed. Level kept.

### Alarm notification
- Regular high-importance notification (channel "Check-ins"): sound + vibration, heads-up, **not full-screen**.
- Title: "Check-in: jaw relaxed?" · Actions: **Good** (green) · **Bad** (red). Tapping the body opens the app to Home, which shows the pending check-in card with the same two buttons.

## 3. Backlog (ordered)

> QA constraint: automation can **only tap and take screenshots** (no typing, no swiping). All acceptance criteria below are verifiable that way; anything needing time to pass or text entry uses the debug **Developer / QA panel** (US-02). Each story that adds behaviour must also add its matching debug actions to that panel.

---

### US-01 — App shell, Home skeleton & theming
**Status: DONE** · **Priority: Must**

As a user, I want a clean, modern app that follows my light/dark preference, so that it feels pleasant to open many times a day.

**Acceptance criteria**
- Given a fresh install, When I launch the app, Then I see the Home screen with the app name, a greeting placeholder ("Hi there"), a level card placeholder ("Level 1 · 0/3") and a **Start** button (non-functional OK in this story).
- Given the app is running, Then bottom navigation shows **Home**, **Stats**, **Settings** (replacing template destinations); tapping each shows its (placeholder) screen.
- Given system dark mode is on, When I launch the app, Then all screens render in the dark theme with readable contrast; same for light.
- Given first launch, Then the one-line "awareness tool, not medical advice" disclaimer is visible on Home (dismissible by tapping "Got it"; stays dismissed).

**Notes / open questions**
- Design: define design system in `docs/design/design-system.md` (M3, Good/Bad semantic colours that work in both themes and aren't alarming). Dynamic colour yes/no?
- Eng: keep `material3-adaptive-navigation-suite`; replace template Favorites/Profile.

---

### US-02 — Developer / QA panel (debug builds only)
**Status: DONE** · **Priority: Must**

As the QA/automation team, I want a debug-only panel with one-tap shortcuts, so that we can verify every flow by tapping without typing or waiting minutes.

**Acceptance criteria**
- Given a **debug** build, When I tap Settings, Then I see a **"Developer / QA"** entry; tapping it opens the panel. Given a **release** build, Then the entry does not exist.
- The panel (scrollable list of buttons, each ≥48dp) contains at least:
  - **Set name: "Alex"** / **Clear name** → Home greeting updates.
  - **Toggle dark mode** (Light / Dark / System cycle) → whole app re-themes immediately; current value shown.
  - **Reset all data** → level L1/0, no history, not running, name cleared, disclaimer shown again.
  - A **state readout** at the top: running/paused/stopped, level/sub-level, next alarm time, consecutive misses.
- Later stories add (and must list in their criteria): **Fire alarm now**, **Answer Good**, **Answer Bad**, **Mark missed**, **Set level N (1–8)**, **Seed sample history (30 days)**, **Short intervals mode (1 min = 1 s; ON by default in debug builds)**, **Simulate quiet hours now**, **Revoke-state previews** for permissions.
- Every action shows a brief snackbar confirming it ran (e.g. "Name set to Alex").

**Notes / open questions**
- Eng: implement in `src/debug` source set so none of it ships in release.
- QA: notification shade cannot be opened by swiping — Eng to confirm whether QA can use `adb shell cmd statusbar expand-notifications`; if not, add **"Open notification shade"** to the panel (debug only).

---

### US-03 — Start a session & receive a check-in notification
**Status: DONE** · **Priority: Must**

As a user, I want to tap Start and get a check-in notification with Good/Bad buttons, so that I get nudged to notice my jaw.

**Acceptance criteria**
- Given Android 13+ and notification permission not granted, When I tap **Start**, Then a short rationale sheet appears ("We need notifications to send check-ins") with **Continue**; tapping it shows the system permission dialog.
- Given I tap **Allow**, Then the session starts: Home shows "Running" and **Stop** replaces Start.
- Given I tap **Don't allow**, Then the session does not start and Home shows a banner "Notifications are off — check-ins can't reach you" with **Open settings** (opens the app's system notification settings).
- Given Android ≤12, When I tap Start, Then the session starts without a dialog.
- Given a running session, When QA taps **Fire alarm now** (debug panel), Then a heads-up notification appears with title "Check-in: jaw relaxed?", sound/vibration, and **Good** and **Bad** actions; it is not full-screen.
- Given a pending check-in, When I open the app, Then Home shows a **pending check-in card** with Good (green) and Bad (red) buttons; tapping either dismisses the card and the notification.
- Given a running session, When I tap **Stop**, Then the pending notification is removed, Home shows Start again, and no further alarms fire (Fire alarm now shows "Not running").

**Notes / open questions**
- In this story every alarm uses a fixed 5 min interval; SRS arrives in US-04.
- Eng: notification actions must work with the app killed (BroadcastReceiver). Channel "Check-ins", importance HIGH.
- Design: rationale sheet copy; pending card layout.

---

### US-04 — SRS progression: level counter & sub-level progress bar
**Status: DONE** · **Priority: Must**

As a user, I want my answers to move me up or down levels, so that check-ins get rarer as I get better at staying relaxed.

**Acceptance criteria** (use debug **Answer Good / Answer Bad / Set level N**)
- Given L1 sub-level 0, When I answer Good, Then Home shows "Level 1" and the progress bar has 3 segments with 1 filled.
- Given L1 with 2/3, When I answer Good, Then Home shows "Level 2", 0/3 filled, and a brief "Level up!" message.
- Given L3 with 2/4, When I answer Bad, Then Home shows Level 3, 0/4 filled.
- Given L3 with 0/4, When I answer Bad, Then Home shows Level 2, 0/3 filled.
- Given L1 with 0/3, When I answer Bad, Then it stays Level 1, 0/3.
- Given L8, When I answer Good, Then Home shows "Level 8 · Max level" and the bar is full.
- Given any level, Then Home shows the **current interval** (e.g. "Check-ins every 15 min") matching the SRS table.
- Answers from the notification action and from the in-app card produce identical results (verify: answer via notification, open app, screenshot).
- State survives force-stop & relaunch (debug panel state readout unchanged).

**Notes / open questions**
- Eng: SRS engine as a pure, unit-tested Kotlin class driven by the table in §2; persist with DataStore/Room.
- Design: progress bar with discrete sub-level segments; level-up/down feedback that is encouraging, not punishing.

---

### US-05 — Reliable scheduling (exact alarms, reboot, permissions)
**Status: DONE** · **Priority: Must**

As a user, I want check-ins to arrive on time even when my phone is idle or restarts, so that I can trust the app.

**Acceptance criteria**
- Given Android 12+ and exact-alarm permission **not** available, When I tap Start, Then the session still starts and Home shows a banner "Check-ins may be a few minutes late" with **Allow exact timing** that opens the system "Alarms & reminders" screen for the app.
- Given I grant exact alarms and return, Then the banner disappears.
- Given a running session, When the device reboots (QA: `adb reboot` or debug **Simulate reboot** which re-runs the boot path), Then after relaunch the state readout shows the session still running with a next alarm time in the future.
- Given notification permission is revoked while running, When I open the app, Then Home shows the "Notifications are off" banner from US-03.
- Given debug **Short intervals mode** is ON (the default in debug builds), Then every interval is scaled **1 min → 1 s** (L1 = 5 s, L2 = 10 s … L8 = 180 s), so QA can see a real scheduled alarm arrive by waiting ≤30 s at L1–L3. The state readout shows "Short int. ON" and Home shows the real interval with a "(short: N s)" suffix. Release builds always use real minutes.

**Notes / open questions**
- Eng: `setExactAndAllowWhileIdle` when `canScheduleExactAlarms()`, else inexact window; `RECEIVE_BOOT_COMPLETED` rescheduling. Evaluate `USE_EXACT_ALARM` vs `SCHEDULE_EXACT_ALARM` (Play policy: this is not an alarm-clock app → likely `SCHEDULE_EXACT_ALARM`). Effort/risk estimate requested.

---

### US-06 — Next-alarm countdown, Pause & Resume
**Status: TODO** · **Priority: Must**

As a user, I want to see when the next check-in is and pause for a meeting, so that I stay in control.

**Acceptance criteria**
- Given a running session, Then Home shows "Next check-in in mm:ss" (or "h:mm") counting down, and it matches the state readout.
- Given running, When I tap **Pause** and choose **1 h**, Then Home shows "Paused until HH:MM" with **Resume**, and the pending alarm is cancelled.
- Given paused "Until I resume", When I tap **Resume**, Then countdown shows the full current interval (e.g. L1 → ~5:00).
- Given a timed pause ends (QA: pause **15 min** with Short intervals ON, where it lasts 15 s, then wait), Then the session resumes automatically and the countdown reappears.
- Given paused, When I tap **Stop**, Then the session ends and Start is shown.

**Notes / open questions**
- Design: Start / Pause / Stop control placement; pause duration picker as chips (tap-only).

---

### US-07 — Missed check-ins & auto-pause
**Status: TODO** · **Priority: Should**

As a user, I want unanswered check-ins not to punish me, and the app to stop nagging if I'm away, so that it stays respectful.

**Acceptance criteria** (use debug **Mark missed** / **Fire alarm now**)
- Given L3 2/4 with a pending check-in, When another alarm fires before I answer, Then the first is recorded as Missed, level stays L3 2/4, and only one notification is visible.
- Given 2 consecutive misses, When I answer Good, Then the consecutive-miss count resets to 0 (state readout).
- Given 3 consecutive misses, Then the session auto-pauses and Home shows "Paused — you missed 3 check-ins" with **Resume**; no further alarms fire.
- Given I swipe-dismiss a notification (QA: debug **Mark missed**), Then it counts as Missed.

**Notes / open questions**
- Eng: detect dismissal via notification `deleteIntent`.
- PM: should the 3-miss threshold be configurable? Not for v1.

---

### US-08 — Stats: Good/Bad charts with time frames
**Status: TODO** · **Priority: Must**

As a user, I want to see my Good vs Bad answers over time, so that I can notice patterns and feel progress.

**Acceptance criteria** (use debug **Seed sample history (30 days)**)
- Given seeded history, When I open **Stats**, Then I see a time-frame selector **Today / 7 days / 30 days** (Today selected by default).
- Given **Today**, Then a bar chart shows Good (green) and Bad (red) counts **per hour**; Given **7 days** / **30 days**, Then bars are **per day**. Tapping each chip updates the chart.
- Above the chart, summary tiles for the selected frame: **Good**, **Bad**, **Missed**, **Good rate %**.
- A **streak** tile shows consecutive days with ≥1 answer and Good rate ≥ 50% (seeded data yields a known value documented in the seed).
- Given no history (after Reset), Then Stats shows an empty state "No check-ins yet — tap Start on Home".
- Charts are legible in light and dark mode; Good/Bad are distinguishable without colour (legend + pattern or labels).
- Below the chart, a **recent history** list (last 20 events: time, Good/Bad/Missed, level at that time).

**Notes / open questions**
- Eng: charting library choice (e.g. Vico) vs custom Canvas — estimate please.
- Design: tapping a bar shows its exact counts (tap-verifiable).
- Seed data must be deterministic so screenshots are comparable.

---

### US-09 — Settings: name, theme, reset
**Status: TODO** · **Priority: Should**

As a user, I want to set my name and appearance, so that the app feels like mine.

**Acceptance criteria**
- Given Settings, Then I see **Your name** (text field), **Theme** (Light / Dark / System, tap-selectable), **Reset progress**, **About & disclaimer**.
- Given name "Alex" (QA: debug **Set name**), Then Home greets "Hi, Alex"; Given no name, Then "Hi there".
- Given I tap **Dark**, Then the app switches to dark immediately and stays dark after relaunch, regardless of system setting; **System** follows the device.
- Given I tap **Reset progress**, Then a confirmation dialog appears; **Cancel** changes nothing; **Reset** sets L1 0/3 and keeps history.
- **About** shows the not-medical-advice text and app version.

**Notes / open questions**
- Name field needs typing → QA verifies via debug Set name; Eng adds a unit/UI test for the field itself.

---

### US-10 — Quiet hours
**Status: TODO** · **Priority: Should**

As a user, I want no check-ins at night, so that the app never wakes me up.

**Acceptance criteria**
- Given Settings, Then **Quiet hours** shows a switch (default ON) and a preset selector: **22:00–07:00**, **23:00–08:00**, **21:00–06:00**.
- Given quiet hours active (QA: debug **Simulate quiet hours now**), When an alarm becomes due, Then no notification fires, nothing is recorded as Missed, and Home shows "Quiet hours — next check-in at 07:00".
- Given quiet hours end, Then the next check-in is scheduled at the end time and the level is unchanged.
- Given the switch is OFF, Then alarms fire at any time.

**Notes / open questions**
- Custom start/end via M3 time picker is **Could** (dial picker is tappable) — later.
- PM open question: should Start during quiet hours warn the user? Proposed: yes, one-line notice.

---

### Later / Won't (v1)
- **Could:** interval jitter (±10%) to reduce predictability; custom quiet-hour times; home-screen widget; per-level names/badges; Wear OS.
- **Won't:** medical content, sleep/night bruxism detection, account/cloud sync.

## 4. Definition of Done (per story)
1. **Design doc** exists: `docs/design/<story-id>-<slug>.md` (and design-system updates if any).
2. **Implemented** on the main branch per acceptance criteria, incl. the story's debug-panel actions.
3. **Builds**: `./gradlew assembleDebug assembleRelease` succeed; unit tests pass (SRS engine has unit tests from US-04 onward).
4. **QA evidence**: screenshots for every acceptance criterion (light **and** dark where UI changed) in `docs/qa/<story-id>/`, named `<nn>-<criterion>-<light|dark>.png`, plus a short `README.md` mapping screenshots to criteria (pass/fail).
5. **Sign-off**: PM (criteria met) and Designer (UI matches design) recorded in the story's QA README.
6. **Status** in this file changed from `TODO` to `DONE`.

## 5. Decisions log

| Date | Topic | Decision | Rationale |
|------|-------|----------|-----------|
| 2026-09-30 | Debug **Answer Good / Answer Bad** with no pending check-in (US-02/US-04) | **Allowed.** The SRS rule is applied and an event is recorded in history like a real answer. If a session is running, the next alarm is rescheduled from now. If it isn't running, only level and history change. The action is debug only, so no user-facing path answers without a pending check-in. | Lets QA test the SRS criteria by tapping alone, without waiting for alarms. |
| 2026-09-30 | **Bad at Level 8** (US-04) | L8 has no sub-level progress (Goods only record history, and `subLevel` stays 0), so the normal rule applies: **Bad at L8 → demote to L7, 0/5.** The next alarm uses the L7 interval (2 h). Home shows the standard, non-punishing level-down feedback. | Matches the rules in §2 as written and needs no special case in the engine. A clench at 3 h intervals is a fair signal to tighten back to 2 h, and 5 Goods restore L8. |
| 2026-09-30 | **Streak when today has no answers yet** (US-08) | Today is **pending, not a break.** If today has no answers, the streak counts back from yesterday. Today joins the streak once it has ≥1 answer with Good rate ≥50%. If today already has answers but Good rate <50%, the streak shows 0 until the rate recovers. | A user shouldn't open Stats in the morning and see "0". |
| 2026-09-30 | **Good rate excludes Missed** (US-08) | **Confirmed.** Good rate = Good / (Good + Bad). Missed is shown in its own tile. If Good + Bad = 0, the tile shows "—". | Missed is neutral by design (§2). Counting it would punish people for being busy. |
| 2026-09-30 | **App name** | **"SuperUnclench"**: one word, camel-case, everywhere (launcher label, `app_name`, title, disclaimer, store listing). | Already in `strings.xml` and the disclaimer copy. Avoids mixing both spellings. |
| 2026-09-30 | **Countdown label format** (US-06) | Accept the designer's **overline "NEXT CHECK-IN" + display "in 12:34"**. The accessibility label carries the exact phrase "Next check-in in …". Format: `mm:ss` under 1 h, `h:mm:ss` at 1 h or more, never negative. QA verifies the value against the readout. The same two-line approach is accepted for "Quiet hours — next check-in at 07:00" (US-10). | Keeps the AC's meaning with a better visual hierarchy. The a11y label is the literal contract. |
| 2026-09-30 | Dynamic colour (design Q, US-01) | **Off for v1.** | Keeps Good/Bad semantic colours predictable. Can revisit later. |
| 2026-09-30 | Debug **Sub-level +1** button (US-02) | **Approved** as a QA convenience (debug only). | No user impact. |
| 2026-09-30 | QA evidence file name (DoD §4) | `docs/qa/<id>/report.md` is accepted in place of `README.md`. PM/Designer sign-offs go in `signoff-pm.md` / `signoff-design.md` next to it. | Matches how QA already works. |
| 2026-09-30 | **Sub-levels within a level** (US-03/US-04) | Confirmed: a new user moves L1 0/3 → 1/3 → 2/3 → L2 0/3, and **all L1 check-ins stay at 5 min**. The interval changes only when the level changes. | This is the SRS table in §2. |
| 2026-09-30 | **Alarm fires while a check-in is pending** (US-03/US-07) | The earlier check-in is recorded as **Missed** (neutral, consecutive misses +1), its notification is replaced, and only one stays visible. | Follows the Missed rule in §2. US-07 is the formal test. |
| 2026-09-30 | Rationale sheet copy (US-03) | Designer copy "Check-ins arrive as notifications" replaces the AC's example wording. | Same meaning, warmer tone. |
| 2026-09-30 | Launch theme flash (US-03 B1) | Must be fixed before US-09 is accepted, because the stored theme has to apply from the first frame. | US-09 AC: "stays dark after relaunch". |
| 2026-09-30 | **US-04 O1** (unexplained promotion during idle) | Doesn't block US-04. Eng adds a QA history dump and root-causes it; this must be **resolved before US-07 is signed off**. If it is an app bug, US-04 reopens. | Every answer path passed deterministically; the data pattern suggests one stray Good, but trust in history is core. |
| 2026-09-30 | **QA panel Kill app unreachable** (US-04 B1) | Must be fixed **before US-05 QA**. | US-05 persistence and reboot checks need a tap-only kill. |
| 2026-09-30 | Notification sub-text "Level n · every …" (US-03 §4.2) | Dropped; the title matters more. | It truncates the title on API 31+. |
| 2026-09-30 | **Short intervals scale** (founder request) | Debug builds use **1 min = 1 s** (L1 5 s … L8 180 s), with Short intervals ON by default in debug; release builds always use real minutes. Pause durations scale the same way (15 min → 15 s). Quiet-hours clock times are **not** scaled. US-02, US-05 and US-06 AC text updated. | Faster QA loops. Every level stays reachable within 3 min. |
| 2026-09-30 | **Auto-pause cancels the pending notification** (US-05 O2a) | Required in US-07. A stale Good/Bad arriving after auto-pause is ignored silently. | A button that does nothing erodes trust. |
| 2026-09-30 | Debug **Auto-pause ON/OFF** toggle | Approved, debug only, default ON. | Hands-free QA at seconds scale. |
| 2026-09-30 | US-04 O1 | **Closed.** Real Good event found in history, most likely a heads-up tap. Source tracking now covers future cases. | — |
| 2026-09-30 | QA panel scrolling (US-05 B1b) | Must be fixed **before US-06 QA**, with jump chips or Kill app moved into Session & alarms. | Tap-only QA can't scroll. |
