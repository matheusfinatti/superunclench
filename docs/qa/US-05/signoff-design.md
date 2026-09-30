# Design sign-off: US-05 Reliable scheduling (exact alarms, reboot, permissions) + Option B notification

- **Reviewer:** Designer · **Date:** 2026-09-30
- **Inputs:** `docs/design/US-05-reliable-scheduling.md`, the US-03 sign-off (Option B spec), QA `report.md`, and screenshots 01, 04, 05, 13, 18, 27, 28, 34.

## Verdict: **ACCEPT**

- **Exact-timing banner** (05) matches §2: `tertiaryContainer`, the alarm icon, a `titleSmall` title with body, and right-aligned **Not now** / **Allow exact timing** in `onTertiaryContainer`.
  - When notifications are also off, that banner comes first (18). Max 2 banners are shown and the disclaimer waits.
  - The session-card caption "Check-ins can't be shown until notifications are on." is exact.
- **Option B pill notification, light shade** (28, expanded) and **heads-up** (34):
  - Green **✓ Good** and red **✕ Bad** pills of equal width, in Good-left order, with correct `notif_good` / `notif_bad` fills and white labels.
  - Title and one body line, with no standard action row duplicated under them.
  - This meets the founder's green/red request. The in-app card and the notification now match.
- **US-04 follow-ups are done** (01, 34): empty segments are visible in `outlineVariant`, and "of 8" sits on the number's baseline right after it. The US-03 B2 label is fixed.
- **Short-interval suffix at the new scale** (1 min → 1 s, founder request): "Check-ins every 5 min (short: 5 s)" in `bodySmall`. **Approved.** The copy key format `(short: %1$d s)` is unchanged. I've updated the examples in US-05 §5 and US-02 §4 (L8 shows "(short: 180 s)", which is fine).

## Must-fix
None for this story.

## Before release (not a US-05 blocker)
1. **Pills in a dark system shade are still unverified.** Only the heads-up over a dark *app* was checked (34), and the shade follows the *system* theme. `values-night` has `notif_on_good` #00391A and `notif_on_bad` #690005 on the dark fills #8DD89F / #FFB4AB, which is correct per spec. QA needs to switch the emulator's **system** dark theme once (Settings → Display → Dark theme) and capture a heads-up and the expanded shade. I recommend the coordinator authorise this for the US-06 QA run.
2. **The notification app icon still shows the green robot** (28, 34), while the system Alarms & reminders page already shows the new icon (07). That points to a SystemUI cache. Check after an **uninstall and reinstall**. If it persists, look for a stale `ic_launcher` in `mipmap-*` webp files (see US-03 sign-off, Decision 2).

## B1b: tap-only QA can't reach the lower QA-panel sections
Root cause: the state card grew to about 520dp (the 10-row "Last events" block, 13/27), and the panel can only be moved by scrolling. The fix below applies to **US-02 §3**, which I've updated:

1. **Jump chips.** Pin a row of `AssistChip`s directly under the top app bar, outside the scrolling list: **State · Profile · Session · Level · Perms · Danger**, plus later sections as they land.
   - Use a `FlowRow` that wraps to two lines rather than scrolling sideways; 48dp touch targets; 8dp gaps.
   - Tapping a chip opens that section (accordion rule) and calls `animateScrollToItem` so its header sits at the top. **State** scrolls back to the readout.
   - Test tags: `qa_jump_state`, `qa_jump_danger`, and so on.
2. **Expanding a section by its header also scrolls it to the top.** Every button is then reachable in two taps with no scrolling, which was the original US-02 principle.
3. **Collapse "Last events" by default** into its own sub-block ("Last events (10)"). Keep the one-line "Last 5" row visible. The card goes back to about 360dp or less.
4. **The panel always opens at the top with sections collapsed.** Opening already scrolled after a fresh process (04) is a bug: don't restore the scroll offset on first entry.

With items 1 and 2, Danger zone → **Kill app** is reachable in two taps from any state. QA should re-test B1b with US-06.

## Follow-ups (fix alongside the next story)
1. **Two `tertiaryContainer` banners stacked** (18) read loud. Keep them: this is the rare worst case, and both need action. No change needed.
2. Engineering observations O2 and O3 (auto-pause during short-interval QA, and the answer/alarm race) have no design impact. For O2 I support a debug toggle **"Auto-pause: ON/OFF"** in Session & alarms (tag `qa_auto_pause`, snackbar "Auto-pause OFF", readout row "Auto-pause"). QA-only; release behaviour is unchanged.
3. Carried over: Material Symbols Rounded icon re-export; template drawables; 8dp snackbar shape and bottom padding; removing the border from the answer confirmation row (US-03 follow-up 2). Resume is required in US-06 (US-04 follow-up 3).

## Not reviewed
- A real reboot.
- A real permission revoke (preview only).
- Dark system shade (see above).
- TalkBack on the RemoteViews pills. Please confirm they announce "Good, I was relaxed" / "Bad, I was clenching".
