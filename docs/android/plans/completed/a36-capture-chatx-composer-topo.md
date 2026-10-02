# Plan — A36 capture-chat: chatX shows the top of the composer

- Status: Concluído (02/10/2026)
- Date: 02/10/2026
- Owning context: `android`
- Affected code: `tools/capture-chat.sh` only (client QA tooling). No app code, no `server/`, no gold change, no `tools/diff-gold.mjs` threshold or region change.
- Prerequisites: none. No Stitch gate (the gold is unchanged and the layout already matches).

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a36-capture-chatx-composer-topo.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

`node tools/diff-gold.mjs dark/chatX` passes its composer region gate (`REGIONS.chatX = [32, 1344, 748, 1668]`, ≤ 2 %) on a fresh `tools/capture-chat.sh` capture. Today it fails at 4.6–4.8 %. The committed capture (`docs/qa/android/current/dark/chatX.png`, from A25) scores 4.77 %, so the failure predates A34.

## Root cause (measured 02/10/2026)

The failure comes from the capture scene, not the Compose layout and not the gold.

1. **The field shows the end of the text.** The A25 scene types 2100 characters with `adb shell input text`. The cursor ends at the last character, and `BasicTextField` (`maxLines = 5`) scrolls to keep the cursor visible. After `keyevent 4` hides the keyboard, the field keeps focus and stays scrolled to the end: "acebolado e de sobremesa um / pedacoxde pudim … / grande e uma x" plus the gold cursor. The gold (and `ChatFixtures.chatX`, used by the JVM `StitchGoldTest`) shows the **first** 5 lines, "Hoje no almoço comi arroz branco, / feijão carioca, …", with no cursor.
2. **Attribution of the 4.77 %** (blurred diff at the best offset, dy = −46 px, same metric as `diff-gold.mjs`):
   - text area (x 150–640, the 5 lines): **4.59 points**, about 96 % of the failing pixels;
   - "Texto muito longo" line: 0.20 points (the known A25 offset: the app starts the label at the text field, 68 dp; the gold at 64 dp; measured 8 px);
   - border, 24 dp radius, camera and send buttons, box height: 0.
3. **Geometry matches** (measured in px, gold vs app): border x 32–747 in both; box height 255 vs 253; text start x 163 vs 162; line pitch ~39 vs ~40 px; gap from the border to the error line 15 px in both.
4. **Why the JVM gate passes (0.82 %):** the fixture holds the gold's text, unfocused, scrolled to the top. Same layout, different content.
5. A25 recorded this as "content, not layout" (`a25-limite-texto-composer.md`, emulator results), but `diff-gold.mjs` still gates the dark region. Every full run therefore exits 1.

### Not a gold conflict (ADR-027)

[ADR-027](../../adrs/ADR-027-golds-divergentes.md) covers disagreements between golds (state vs base, dark vs light) and spec vs gold rendering. None applies here: the dark gold, the light gold and the spec agree with the app's layout. The fix belongs to the capture scene. Adding `dark/chatX` to `REGION_REPORT_ONLY` would hide a real gate and is rejected.

## Sources of truth

- Gold `docs/qa/stitch/dark/chatX.png` (ST5), [A25](a25-limite-texto-composer.md), [ADR-022](../../../produto/adrs/ADR-022-limite-texto-chat.md) (text limit).
- Code: `tools/capture-chat.sh` (chatX block, lines ~201–260), `tools/diff-gold.mjs` (`REGIONS.chatX`), `apps/android/.../feature/chat/ChatScreen.kt` (`Composer`, `ComposerRow`), `ChatFixtures.chatX`.

## Implementation scope

Only the chatX block of `tools/capture-chat.sh`.

1. **Scroll to the top before the shot, by touch (revision 1).** After `keyevent 4` (keyboard hidden, field still focused, cursor at the end), drag the text field's content down with `input swipe` inside the `chat-input` bounds (from its top edge + 10 px to its bottom edge − 10 px, ~150 ms), repeated until the text is at the top (~2800 px of content: about 16 swipes; the script repeats a fixed count with margin). A drag scrolls the field without moving the cursor or emitting key events, so the IME stays in soft-keyboard mode.
   - Before every swipe the script checks from a `uiautomator` dump that `chat-input` is present; if it is missing (the screen changed), it fails the scene with `✗` and does not send more input.
   - Check `chat-too-long` is still shown, then `shot chatX`.
2. **No cursor move after the shot.** The cursor never left the end, so the 100 backspaces still delete from the end. The existing checks (`2000: error gone`, `fake got 2000 characters`) confirm it. If the field scrolls back to the cursor on the first backspace, that is expected and harmless.
3. **Leave alone:** the typed text. `adb input text` cannot type accents. In the first 5 lines only "almoço" and "feijão" differ from the ASCII text, and the line breaks match. The extra `x` at positions 2000 and 2100 sits far below line 5.

The blinking cursor may appear at the start of line 1 in the capture (≤ 1 px wide gold bar). Its blurred footprint is estimated at under 0.1 % of the region; accepted.

## Affected files

- `tools/capture-chat.sh`.
- `docs/qa/android/current/{dark,light}/chatX.png` (fresh captures, evidence).
- `docs/android/README.md`, `docs/README.md` (plan index), this plan (lifecycle).

## Planned validation

1. `bash -n tools/capture-chat.sh`.
2. On the gold-geometry AVD (`wm size 780x1688`, `wm density 320`) with `node tools/fake-chat-server.mjs` and the devDebug APK built with `-PAPI_PUBLIC_URL=http://10.0.2.2:8765`, dark and light: the chatX block of `tools/capture-chat.sh` runs, and its existing checks pass (`over 2000: Texto muito longo`, `camera did nothing`, `over 2000: 0 POST`, `2000: error gone`, fake gets 2000 characters).
3. `node tools/diff-gold.mjs dark/chatX light/chatX`: dark region ≤ 2 % (gate), exit 0. The light region stays report-only (A25: the light gold paints the composer on the page colour); its new value is recorded.
4. Written diff list (capture vs gold) in the results: layout, tokens, type size, radius, CTA, error line.
5. No app code changes: `StitchGoldTest`, Roborazzi and unit tests are not affected and not required by this plan.

## Out of scope

- App code (`apps/android/`): the 4 dp start offset of "Texto muito longo" (68 vs 64 dp) stays as A25 recorded it (0.20 points, inside the gate).
- `tools/diff-gold.mjs` regions, thresholds and report-only sets; golds; Stitch.
- Typing accents through a third-party IME or clipboard tool (not needed for the gate; no new downloads).
- Other capture scenes and the A33 flaky `input text` drop (already topped up by the script).

## Risks and controls

- **Key events are out (attempt 1):** no `keycombination`, DPAD or Home/End key before the shot; they flip Gboard to physical-keyboard mode and leave the chat (see § Implementation attempt 1).
- **Drag does not scroll the field, or the field snaps back to the cursor:** stop and report; do not tap inside the field (a tap moves the cursor and reopens the keyboard). Remaining options then need an owner decision: a dev-only way to seed the composer text (app code, own plan) or a dated report-only exception for `dark/chatX` in `diff-gold.mjs`.
- **A swipe lands outside the field after a screen change:** every swipe is preceded by a dump that finds `chat-input`; otherwise the scene fails without more input.
- **Residual over 2 % after scrolling to the top:** stop, report the new attribution and propose the next step; do not loosen the gate.

## Acceptance criteria

- Fresh dark `chatX` capture shows the first 5 lines ("Hoje no almoco comi arroz branco, …"), red border and "Texto muito longo".
- `node tools/diff-gold.mjs dark/chatX` region ≤ 2 %.
- All existing A25 checks in the chatX block still pass.

## Implementation attempt 1 (02/10/2026)

Approved by the owner on 02/10/2026 ("Aprovo o plano `docs/android/plans/a36-capture-chatx-composer-topo.md`. Implemente o plano aprovado."). Stopped under § Risks and controls; nothing committed, `tools/capture-chat.sh` and the captures reverted.

Setup: AVD `Medium_Phone` (API 36) at 780x1688 / 320 dpi, `node tools/fake-chat-server.mjs`, devDebug APK built with `-PAPI_PUBLIC_URL=http://10.0.2.2:8765`.

- **Ctrl+Home (`input keycombination 113 122`)**: full `tools/capture-chat.sh dark` run. The A25 checks in the chatX block passed (`over 2000: Texto muito longo`, `camera did nothing`, `over 2000: 0 POST`, `2000: error gone`, `fake got 2000 characters`), but the cursor did not move: the field stayed at the end of the text. The key combination also switched Gboard to physical-keyboard mode, and its floating toolbar (mic, backspace, enter, emoji) covered the camera button and the left part of the composer. `diff-gold.mjs dark/chatX` region: **10.33 %** (was 4.77 %). Run stopped after the chatX block.
- **DPAD fallback (`KEYCODE_DPAD_UP` × 30 + `KEYCODE_MOVE_HOME`)**, by hand on a 9-line text: the cursor stopped at the first *visible* line and the field did not scroll further. Two more DPAD_UP presses moved focus out of the field, and the app left Chat for Home. With Gboard in physical-keyboard mode, Back no longer only hid a keyboard: it closed Chat. Stray input after that opened system screens (a Google sign-in prompt and Quick Settings, both dismissed with Back/Home, nothing entered). The emulator was shut down (`-no-snapshot-save`, so its next boot is clean).
- Conclusion: key events cannot move the composer to its first line reliably, and they change the IME mode for the rest of the run. Revision 1 replaces them with a touch drag (Implementation scope § 1–2). The drag itself is **not yet tested**.

## Results (02/10/2026, revision 1)

Revision 1 approved by the owner on 02/10/2026 ("Aprovo o plano `docs/android/plans/a36-capture-chatx-composer-topo.md`. Implemente o plano aprovado."). Same setup as attempt 1 (AVD `Medium_Phone`, API 36, 780x1688 / 320 dpi, fake server, devDebug APK on `http://10.0.2.2:8765`).

- `tools/capture-chat.sh`, chatX block: after Back, 24 swipes (`input swipe`, 150 ms) from the top to the bottom of `chat-input`; each swipe preceded by a dump that finds the field, or `✗ chat-input gone while scrolling to the top` and no more input. No key events. Cursor left at the end; no change after the shot.
- `bash -n tools/capture-chat.sh`: OK.
- Dark, full run: the drag reached the first line ("Hoje no almoco comi arroz branco, / feijao carioca, duas coxas de / … / uma colher de farofa, meio bife", same line breaks as the gold), no cursor visible, keyboard and Gboard toolbar absent. A25 checks: `over 2000: Texto muito longo`, `camera did nothing`, `over 2000: 0 POST`, `2000: error gone`, `2000 characters answered`, `fake got 2000 characters` ✓. Run exit 1 from four checks in the A32 block "camera closes the keyboard" (`keyboard still open with the sheet`, `photo sheet open`, `not found: chat-photo-cancel`, `keyboard reopened on close`). They reproduce with `SCENES=a32` alone, which never runs the chatX block: outside A36, left for a separate follow-up.
- Light, full run: chatX block and its A25 checks ✓ (same list). After it, the emulator and the fake server hit the agent's 30-minute background limit and were stopped; the A30 checks that followed failed for that reason only and are not evidence. `input text` dropped characters in this run ("coxade", "asss"; the script topped up to 2100 at the end), so the light lines differ from the gold's text.
- `node tools/diff-gold.mjs dark/chatX light/chatX`: exit 0.

| | before (A25 capture) | after |
| --- | --- | --- |
| `dark/chatX` region (gate ≤ 2 %) | ✗ 4.77 % | ✓ **0.89 %** |
| `dark/chatX` whole screen (report only) | 6.65 % | 5.88 % |
| `light/chatX` region (report only) | 7.24 % | 6.64 % |
| `light/chatX` whole screen (report only) | 6.95 % | 6.91 % |

### Diff list (capture vs gold `chatX`)

- Layout: 5-line box, 24 dp radius, 40 dp camera and send at the bottom, red 1.5 dp border, "Texto muito longo" under the box: same. Label starts at 68 dp, gold 64 dp (A25, out of scope).
- Tokens: border and label `bad`, box `card`, buttons `surf2`: same in dark. Light: the gold paints the box on the page colour (chat0 generation), the app on `card` (canonical chatE); report only since A25.
- Type: 14 sp / 20 body, 12 sp W500 label: same. Text without accents (adb): "almoco", "feijao".
- CTA, timeline, macros: not on this screen. Header and thread: chatX gold is a chat0 copy (other header), report only since A25.

Only `docs/qa/android/current/{dark,light}/chatX.png` are kept from the runs; the other captures they refreshed were reverted.
