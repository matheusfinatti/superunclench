# Design sign-off: US-06 Next-alarm countdown, Pause & Resume

- **Reviewer:** Designer · **Date:** 2026-10-01
- **Inputs:** `docs/design/US-06-countdown-pause-resume.md`, QA `report.md`, and screenshots 02, 03, 06, 07, 09, 13, 14, 15, 17, 18, 23.

## Verdict: **ACCEPT**

**Session card, every state matches §2:**
- **Running:** green chip, NEXT CHECK-IN overline, "in" plus the `displaySmall` tabular countdown, "at 9:38 AM", then a tonal **Pause** and an outlined **Stop**.
- **Inline pause picker:** "Pause check-ins for…", the chips 15 min / 1 h / ❚❚ Until I resume, and Cancel. No bottom sheet, so it works by tap alone.
- **Timed pause:** PAUSED UNTIL with the time, "Check-ins resume automatically.", and a filled **Resume** next to Stop.
- **Pause until resume:** "Paused until you resume" / "Your level is kept."

The snackbar copy is exact. Light and dark are both correct (14, 15).

**Earlier follow-ups, all verified:**
- **Pill notification in a dark system shade** (17, 18): light green #8DD89F and salmon #FFB4AB fills with dark labels, the calm-face app icon, and the same order and shape as in light. This closes the US-05 "before release" item 1.
- **New app icon** in the notification and system pages (closes US-05 item 2).
- **Snackbar** with 8dp corners, `inverseSurface` (13, 17).
- **No outline on the answer confirmation row** (23).
- **QA jump chips** (02, 17) work, which closes B1b.

## Must-fix
None.

## QA question: minutes-only TalkBack countdown label
**Accepted.** The label only refreshes once a minute, so seconds would be wrong most of the time, and "at 9:38 AM" carries the precision. Two refinements, which I've updated in §7:
1. **Round to the nearest minute.** At 02:59 the label currently says "in 2 minutes", which under-reports. It should say "in 3 minutes".
2. **Under one minute, say "in less than a minute"**, not "in 3 seconds". The seconds value goes stale straight away. At one hour or more, say "in 1 hour 5 minutes".

## Follow-ups (fix alongside the next story)
1. **Two success messages at once** (23): "Nice — noted." on the pending card plus "Still unclenched. Nice." in the level card. It's redundant. When the answer comes **from the pending card** and the level card's message would be the L8 "Still unclenched", skip the level-card message. Keep it for notification answers, since there's no card confirmation then. Level up, reset and demote messages still show, because they carry new information.
2. **"of 8" rendered above the number** in 03, while it's correct in every other frame. This is probably the number transition caught mid-animation right after QA set L8 from the panel. Engineer: check that the "of 8" baseline alignment isn't lost during `AnimatedContent`. QA: one screenshot about 1 s after a level jump.
3. **"TODAY" overline is clipped on the left** (03, 07, 09: the "T" is cut off). This is in US-08's Today section, so it must be fixed in US-08. It looks like a negative offset or clipped `Text` padding: the label should start on the 16dp gutter, like the tiles.
4. **Pause picker has extra bottom space** under Cancel (06, about 24dp more than the other states). Use the 20dp card padding only.
5. **Heads-up covering the QA jump chips** (QA note 2): a debug-only problem. Suppressing heads-up while the QA panel is open is fine; there's no design opinion beyond that.

## Not reviewed
- 150–200% font scale (stacked buttons).
- Reduced motion.
