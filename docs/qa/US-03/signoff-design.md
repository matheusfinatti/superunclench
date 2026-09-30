# Design sign-off: US-03 Start a session & receive a check-in notification

- **Reviewer:** Designer · **Date:** 2026-09-30
- **Inputs:** `docs/design/US-03-start-and-checkin.md`, `design-system.md`, QA `report.md`, and screenshots 02, 03, 06, 10, 13, 14, 18, 24, 25, 29, 31, 32, 36 and 38.

## Verdict: **ACCEPT**

The in-app experience matches the spec, in light and dark:
- **Rationale sheet:** drag handle, 72dp `primaryContainer` circle with the bell, centred headline and body, 56dp Continue, and Not now.
- **Notifications-off banner:** attention variant, exact copy, Open settings.
- **Session card:** the Stopped helper line; the Running chip on `goodContainer`; a full-width outlined Stop (Pause is correctly hidden until US-06).
- **Pending card:** `primaryContainer` with a 1dp `primary` border, the CHECK-IN overline with the time, the title and body, 56dp green/red AnswerButtons with check/close icons and captions, and the in-place confirmation rows.
- The `sentiment_calm` small icon appears in the status bar.
- The Stats tab now uses `insert_chart` with a visible selected state (US-01 follow-up 1 done).

Good and Bad are never shown by colour alone: every place pairs the colour with an icon and a word.

## Must-fix
None for this story.

## Decision 1: notification Good/Bad colours (Option A vs Option B)

**Decision: build Option B, but not as part of US-03. It is required before the v1 release.** I'm flagging this to PM because it differs from PM's sign-off ("Option B spike not needed for v1").

Reasons:
- The founder's brief (`user-stories.md` §1 and §2) literally specifies the notification actions as **Good (green) · Bad (red)**.
- The notification is the main surface of this app. Most answers happen from the shade or heads-up, not from Home.
- Option A is stripped on API 31+ (13, 14 show system-accent blue), and API 31+ is most active devices. So as built, the founder's request is unmet for most users.
- It isn't a blocker today because meaning is carried by the word labels, and the in-app card is fully coloured.

Scheduling: time-box Option B to **1 day**, together with **US-05** (reliable scheduling), which touches the same `AndroidCheckInNotifier`. QA takes screenshots on API 33 and 37, light and dark shade. If the spike fails on any tested API, keep Option A there (per-API fallback) and report it to me.

**Option B spec** (it replaces §4.3 B's details where they differ):
- `NotificationCompat.DecoratedCustomViewStyle()`. The system draws the header (small icon, app name, time) and the expand affordance.
- **Collapsed** (`setCustomContentView`, about 48dp tall): one line, the title "Check-in: jaw relaxed?", in the system's `TextAppearance.Compat.Notification.Title`. No buttons here, because they don't fit reliably.
- **Heads-up** (`setCustomHeadsUpContentView`) and **expanded** (`setCustomBigContentView`) use the same layout:
  1. The title line (as collapsed).
  2. The body "Were you clenching just now?" in `TextAppearance.Compat.Notification`, max 1 line.
  3. 8dp gap, then a row of two equal-width pills with an 8dp gap between them.
- **Pills:** 40dp tall, 20dp corner radius (a `<shape>` drawable). Fill is `@color/notif_good` / `@color/notif_bad`. Text and icon use a new `@color/notif_on_good` / `@color/notif_on_bad` (light: #FFFFFF / #FFFFFF, night: #00391A / #690005). Content is an 18dp `ic_good` / `ic_bad` plus "Good" / "Bad" in 14sp medium. Colours come from `values` / `values-night`, so they follow the **system** shade theme, as in design-system §2.4.
- **Wiring:** `setOnClickPendingIntent` on each pill, using the same PendingIntents as today's actions. Set `contentDescription` to "Good, I was relaxed" / "Bad, I was clenching".
- **Do not** call `addAction` on the phone notification (it would duplicate the buttons). Add the two actions through `NotificationCompat.WearableExtender().addAction(...)` instead, so watches and Auto still get them. Keep the Option A spans on those actions.
- Keep `setContentTitle` / `setContentText`, because the system uses them for the lock screen, accessibility and bridged devices.

## Decision 2: app icon and notification icons

The **notification small icon** is already correct: `ic_stat_checkin` (sentiment_calm, white). The green Android glyph in 13 and 14 is the **launcher icon**, which the API 31+ template shows as the app icon, so fixing the launcher icon fixes the notification too. Replace the template adaptive icon as follows. Please take these as specs, not production code. I rasterised the result: the glyph fits well inside the 66dp safe zone and the whole-face circle reads at small sizes.

`res/drawable/ic_launcher_background.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#D5E3FF" android:pathData="M0,0h108v108h-108z" />
</vector>
```

`res/drawable/ic_launcher_foreground.xml` is the Rounded `sentiment_calm` glyph, the same path as `ic_stat_checkin`, scaled to a 60dp box centred in 108dp and coloured `#3D5F90`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:translateX="24" android:translateY="24"
        android:scaleX="0.0625" android:scaleY="0.0625">
        <path android:fillColor="#3D5F90"
            android:pathData="M480,700Q514,700 544,687.5Q574,675 600,652Q609,644 609,631Q609,618 600,610Q591,602 578.5,602Q566,602 556,610Q539,623 520,631Q501,639 480,639Q459,639 440,631Q421,623 404,610Q394,602 381.5,602Q369,602 360,610Q351,618 351,631Q351,644 360,652Q386,674 416,687Q446,700 480,700ZM480,880Q397,880 324,848.5Q251,817 197,763Q143,709 111.5,636Q80,563 80,480Q80,397 111.5,324Q143,251 197,197Q251,143 324,111.5Q397,80 480,80Q563,80 636,111.5Q709,143 763,197Q817,251 848.5,324Q880,397 880,480Q880,563 848.5,636Q817,709 763,763Q709,817 636,848.5Q563,880 480,880ZM480,480Q480,480 480,480Q480,480 480,480Q480,480 480,480Q480,480 480,480Q480,480 480,480Q480,480 480,480Q480,480 480,480Q480,480 480,480ZM480,800Q614,800 707,707Q800,614 800,480Q800,346 707,253Q614,160 480,160Q346,160 253,253Q160,346 160,480Q160,614 253,707Q346,800 480,800ZM340,480Q372,480 397.5,460Q423,440 437,411Q442,398 437.5,385.5Q433,373 420,370Q409,367 398,372.5Q387,378 382,389Q375,402 364.5,411Q354,420 340,420Q326,420 315,410.5Q304,401 298,388Q293,377 282,372Q271,367 260,370Q247,373 242.5,385.5Q238,398 244,411Q256,441 282,460.5Q308,480 340,480ZM620,480Q652,480 677.5,460Q703,440 717,411Q722,398 717.5,385.5Q713,373 700,370Q689,367 678,372.5Q667,378 662,389Q655,402 644.5,411Q634,420 620,420Q606,420 595,410.5Q584,401 578,388Q573,377 562,372Q551,367 540,370Q527,373 522.5,385.5Q518,398 524,411Q536,441 562,460.5Q588,480 620,480Z" />
    </group>
</vector>
```
(0.0625 = 60/960.)

`res/drawable/ic_launcher_monochrome.xml` is identical to the foreground but with `android:fillColor="#FFFFFF"`, because the system tints it for themed icons.

`mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` use background, foreground and `<monochrome android:drawable="@drawable/ic_launcher_monochrome"/>`.

Regenerate the `mipmap-*/ic_launcher*.webp` legacy PNGs (API 24–25) from these with Android Studio's Image Asset tool, or delete them if minSdk is 26 or higher. Don't call `setLargeIcon`: the app icon is enough.

## Decision 3: B1 white flash at cold start (in-app Dark, system light)

This isn't user-reachable until US-09 ships the theme setting, so I agree with PM that it **must be fixed before US-09 is accepted**. Design requirement: the launch window, including the splash, uses the **resolved app theme**. That means `surface` #F9F9FF light or #111318 dark, with the new launcher icon, never white under a dark app.

Recommended approach (feasibility is Eng's call): on API 31+, call `UiModeManager.setApplicationNightMode(MODE_NIGHT_YES / MODE_NIGHT_NO / MODE_NIGHT_AUTO)` whenever the theme preference changes, and once at startup to migrate. The system then resolves `values-night` for **our** process before the first frame, so the existing `window_background` colours and the SplashScreen are correct on the next cold start. On API 24–30 without AppCompat, accept the brief mismatch, or set `android:windowBackground` to `@android:color/transparent` only if that doesn't cause a transparent-launch artefact. Test before choosing.

Splash spec (API 31+ defaults are fine): `windowSplashScreenBackground` = `@color/window_background`, icon = the adaptive launcher icon, no branding image.

## Follow-ups (fix alongside the next story)
1. **Heads-up title truncates** ("Check-in: jaw rel…", 13). On API 31+ the collapsed header shares a line with the sub-text. **Remove `setSubText`** (level and interval aren't needed at a glance, and Home shows them). I've updated US-03 §4.2.
2. **Confirmation row keeps the pending card's `primary` border** (24: a lilac outline on red). While the confirmation shows, animate the border to transparent, or set the border to the semantic container colour.
3. **Notifications-off banner** (`tertiaryContainer`, 06) is loud next to the neutral disclaimer, but it is intentionally the attention colour and it's the priority-1 banner. **Keep it as is.** No change.
4. **B2** (debug): shorten the label to "Preview: API ≤32: ON/OFF", and centre wrapped labels (US-02 follow-up 1).
5. **B3**: create the `checkins` channel in `Application.onCreate` (PM has already assigned this to US-05). There is no design change.
6. Still open from US-01/US-02: re-export icons as Material Symbols Rounded (the new `ic_good`, `ic_bad`, `ic_pause`, `ic_stop`, `ic_notifications` and `ic_notifications_off` included; check that they are Rounded), delete the template drawables, add snackbar bottom padding and use 8dp snackbar shape.

## Not reviewed
- Tapping the notification body, and focus on the pending card.
- TalkBack announcements.
- A real API ≤32 device.
- Sound and vibration.
