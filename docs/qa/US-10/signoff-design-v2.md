# Design sign-off v2: US-10 Quiet hours (Starts/Ends + M3 time pickers), with the US-08 B1 re-check

- **Reviewer:** Designer · **Date:** 2026-10-01
- **Inputs:** `docs/design/US-10-quiet-hours.md` §2 (revised), `report-v2.md`, screenshots `v2/` 02, 07, 11, 13, 14, 20, and US-08 `report.md` "B1 re-check #2" with screenshots 26 and 27.

## Verdict: **ACCEPT**

**Settings rows match §2** (07 light, 14 dark, 20 off):
- A bedtime switch row with "No check-ins while you rest.", a divider, and indented **Starts / Ends** rows with the times right-aligned in `primary`.
- The **"Next day"** caption appears under the Ends time only for overnight windows. Under 1:00 → 2:00 PM it's absent, which is correct.
- The duration line reads "6 h 45 min of quiet" or "1 h of quiet".
- With the switch OFF, the rows dim but keep their values and aren't clickable.
- The 24 h device format is respected everywhere (08, 09).

**Picker matches §2.1** (02, 11, 13):
- An M3 dial titled "Quiet hours start" / "Quiet hours end", opening on the current value.
- The keyboard ↔ clock toggle sits bottom-left, with Cancel/OK.
- When start equals end, the error "Start and end can't be the same" shows in `error` and OK is greyed and disabled.
- Keyboard mode shows Hour/Minute labels. Correct in light and dark.

**Snackbar copy is exact:** "Quiet hours 11:30 PM – 6:15 AM", "Quiet hours off".

**Home quiet state:** the regression check passed with a real clock window (16).

## Must-fix
None.

## Follow-ups (polish, optional)
1. **AM/PM selector colour.** The M3 default `tertiaryContainer` (lilac) differs from the `primaryContainer` hour field next to it. It's acceptable as the M3 default. If it's a one-line `TimePickerDefaults.colors` change, use `periodSelectorSelectedContainerColor = secondaryContainer` for a calmer look. Optional.
2. Alert style (US-11) will sit above Quiet hours in this card, per US-11 §2. Keep the divider pattern.

## US-08 B1 re-check #2: **closed**
- The Today, 7-day and 30-day semantics nodes now tile the plot exactly, and every drawn bar sits inside its own node. Tapping a node centre selects its own bucket (26: "11 PM–12 AM"; 27: 30d nodes → Sep 2 / Sep 5 / Oct 1).
- The Today readout fits on one line ("10 AM–11 AM · 0 Good · 1 Bad · 1 Missed").
- The tooltip is clamped inside the plot at the right edge (26).
- **Note on target size:** per-hour slots are about 13dp wide, below the 48dp touch-target rule in design-system §10. This is accepted, because the **‹ › readout buttons (48dp)** give every bucket an equivalent, full-size control, which the WCAG 2.5.8 alternative exception covers. Don't enlarge the slots again; the earlier enlargement is what caused the B1 offset.

US-08 has no remaining design items.
