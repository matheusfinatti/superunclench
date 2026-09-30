# Design sign-off: US-02 Developer / QA panel (debug only)

- **Reviewer:** Designer · **Date:** 2026-09-30
- **Inputs:** `docs/design/design-system.md`, `docs/design/US-02-qa-panel.md`, QA report and screenshots 01–17 in this folder, and `QaPanelScreen.kt`.

## Verdict: **ACCEPT**

The panel meets the design intent: every action can be reached by tapping, the results show up in screenshots, and the controls are accessible.

**What matches the spec**
- Settings entry: a "DEVELOPER" header in `titleSmall` `primary`, the bug icon, "Developer / QA" with "Debug build only", and a chevron. It opens inside the Settings tab with a back arrow, and the nav bar stays visible.
- The state readout is a `surfaceContainerHigh` card with `large` corners, `bodySmall` `onSurfaceVariant` labels and monospace values. It updates in the same frame as each action.
- The accordion keeps one section open at a time. Section headers are `titleSmall` `primary` with an expand chevron.
- Buttons are `FilledTonalButton`s at ≥48dp, two per row with 8dp gaps. Danger-zone buttons are `OutlinedButton`s with `error` content and no confirmation.
- Snackbar copy matches §4 exactly. Theme cycling re-themes the whole app, status bar included.

## Must-fix
None.

## Follow-ups (fix alongside the next story)
1. **Centre wrapped button labels.** Two-line labels ("Show disclaimer again", "Reset progress (no confirm)") are left-aligned inside the pill (03, 13). Add `textAlign = TextAlign.Center` to the label `Text` for every panel button.
2. **Snackbar covers the bottom of the list** (13, 14: "Name set to Alex" sits over the area below Danger zone). Add bottom content padding to the section list equal to the snackbar height plus 8dp (about 64dp) while a snackbar is showing. The simpler option is a constant 72dp bottom padding. The last section's buttons must never sit under the snackbar once US-03 to US-10 fill the list.
3. **Snackbar shape.** It looks like M3's default 4dp. design-system §4 sets `small` = 8dp for snackbars. Pass `shape = MaterialTheme.shapes.small` in a custom `SnackbarHost`, and make the Home snackbar host in US-03 use the same shape.
4. **Readout pairing (optional).** One field per row is fine; I'm withdrawing the paired rows from the spec. Readable beats compact. Once the US-03 to US-10 sections exist, check that the pinned card plus one open section still fits a 412×915dp screen without scrolling. If it doesn't, do the fontScale ≥ 1.3 collapsible-state-card behaviour from §3 at 1.0 scale as well.
5. **Icons:** `ic_bug` and `ic_expand_more` are legacy Material Icons, and `ic_bug` is filled. Re-export them as Symbols Rounded Fill 0 along with the rest of the set (see US-01 sign-off, follow-up 3).

## Not reviewed
- Release build without the entry (QA static check only).
- Pinned state card while the list scrolls.
- Running, paused and next-alarm readout values (reachable from US-03 onward).
