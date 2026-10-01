# Plan — A33 capture-chat: memory checks on the A28 format

- Status: Concluído
- Date: 01/10/2026
- Owning context: `android`
- Affected code: `tools/capture-chat.sh` only (client QA tooling). No app code, no `server/`, no fake server change.
- Prerequisites: none. No gold change.

## Authorization gate

This plan is documentation only. Implementation starts only after an explicit approval naming this file:

> Aprovo o plano `docs/android/plans/a33-capture-chat-memoria-v2.md`. Implemente o plano aprovado.

If implementation reveals an uncovered decision, stop, update the artifacts and ask for a new approval.

## Goal

`tools/capture-chat.sh dark` (full run, `SCENES` unset) stops failing two legacy A8/A8b checks and checks memory v2 (A28) instead, keeping the two original intents: `memory.bin` is never plaintext, and a crash mid-write never touches the stored memory.

## Root cause

Both failures are stale checks, not app bugs.

1. `✗ memory.bin missing (49 bytes)` (`tools/capture-chat.sh:130-133`). Since A28, Gravar applies only the routine updates the AI proposed (`FactMemory.apply` stores only when the memory changed). The first scene uses the plain fake estimate, which has no `memory_updates`, so no `memory.bin` is written. The 49 bytes are not a file: `adb exec-out run-as … cat` merges stderr, and `cat: files/memory.bin: No such file or directory\n` is exactly 49 bytes.
2. `✗ POST memory: ''` (`tools/capture-chat.sh:198-199`). `PromptBuilder` always sends `memory = ""` and the memory as `facts` (A28). The A8 line `… 380 kcal …` no longer exists.

## Current A28 format (confirmed in code)

- `AtomicMemoryFile`: `filesDir/memory.bin`; write goes to `memory.bin.new`, fsync, `ATOMIC_MOVE` over `memory.bin`. A stale `.new` is never read and is overwritten by the next write.
- `AesGcmMemoryCipher`: `"NM"` + version byte `0x01` + 12-byte IV + ciphertext + 16-byte GCM tag. Minimum sealed size 31 bytes.
- Plaintext inside the seal: `FactMemory.Stored` JSON `{"v":2,"next":…,"facts":[…]}`.
- Request: `memory: ""` and `facts: [...]`; the fake's `GET /__calls` echoes both (`memory`, `facts`).
- Fake `{"routine": true}`: `memory_updates` add a dynamic routine `cafe` and a permanent preference `leite` on the first call.

## Sources of truth

- [A28 memory v2](a28-memoria-v2.md), [memória e push spec](../../../produto/specifications/memoria-push.md).
- [A8b atomic write](a8b-memoria-gravacao-atomica.md) (intent of the crash check).
- Code: `core/memory/AtomicMemoryFile.kt`, `MemoryCipher.kt`, `FactMemory.kt`, `feature/chat/PromptBuilder.kt`; `tools/fake-chat-server.mjs`.

## Implementation scope

### 1. First scene (plain Gravar, `SCENES` unset)

Replace the A8/A8b block (lines ~130-138) with:

- Existence through `run-as $PKG ls files/` (never infer it from `cat` bytes): `✓ no memory.bin after a plain Gravar (no memory_updates)`; fail if `memory.bin` exists.
- Keep `✓ no A8 memory.txt`.
- Drop the crash-mid-write setup here (it moves to step 2, where there is a memory to protect).

Replace the POST check (lines ~198-199) with:

- `memory` is `""` and `facts` is `[]`: `✓ POST carries memory "" and facts [] (A28: nothing invented by a plain Gravar)`.

### 2. A29 routine block (runs in the full run and `SCENES=v2`)

After the day -3 Gravar (memory now holds `cafe` + `leite`):

- Pull `memory.bin` only if `ls files/` lists it. Check with `$PY`: size > 31, first 3 bytes `4e 4d 01` (`NM` + version 1): `✓ memory.bin sealed (N bytes, NM v1 header)`.
- Not plaintext: no `cafe`, `leite`, `semidesnatado`, `routine`, `"v":2` in the raw bytes.
- Crash mid-write: force-stop, write `lixo-de-crash` to `files/memory.bin.new`.

After the day -2 send (before its Gravar):

- `/__calls` `facts` still hold keys `cafe` and `leite`: `✓ stale memory.bin.new ignored, memory intact`.

After the day -2 Gravar:

- `memory.bin.new` gone from `ls files/` (the atomic write replaced it).

## Affected files

- `tools/capture-chat.sh`.
- `docs/android/README.md`, `docs/README.md` (plan index), this plan (lifecycle).

## Planned validation

1. `bash -n tools/capture-chat.sh`.
2. Full `tools/capture-chat.sh dark` on the gold-geometry AVD with `node tools/fake-chat-server.mjs` and the devDebug APK built with `-PAPI_PUBLIC_URL=http://10.0.2.2:8765`: the new checks pass, exit code 0 (or any remaining failure is reported and shown unrelated to A33).
3. `SCENES=v2 tools/capture-chat.sh dark`: the A29 memory checks pass on their own.
4. Captures are a by-product; no UI changed, so no gold diff is required by this plan.

## Out of scope

- App code (`apps/android/`), `server/`, `tools/fake-chat-server.mjs` (its header comment still says `memory` echoes A8; harmless).
- Any memory rule change, the A23 dev editor, `capture-push.sh` or other capture scripts.

## Risks and controls

- **Emulator Keystore key regenerated between runs:** `AtomicMemoryFile` deletes an unreadable file; the A29 block starts from `CLEAN` + routine add, so the check reads a file written in this run.
- **Fake echo races:** each check reads `/__calls` right after the send it depends on, as the script already does.

## Acceptance criteria

- No check in the full dark run expects an A8 line or an A8 `memory.bin` after a plain Gravar.
- Plaintext and crash-mid-write intents are checked on a real A28 `memory.bin`.
- Full `tools/capture-chat.sh dark` runs without the two reported failures.

## Closure

After implementation, record real results here and apply the lifecycle in `docs/sdd/README.md`.

## Results (01/10/2026)

Implemented in `tools/capture-chat.sh` as planned, on the emulator `emulator-5554` with `node tools/fake-chat-server.mjs` and the installed devDebug APK.

- Finding during implementation: `adb exec-out run-as … ls files/` prints columns, so a line match never hits. `files_has` uses `ls -1`; without it the absence checks would pass vacuously.
- `bash -n tools/capture-chat.sh`: OK.
- Full `tools/capture-chat.sh dark`: every A33 check passes — `no memory.bin after a plain Gravar`, `no A8 memory.txt`, `POST carries memory "" and facts []`, `memory.bin sealed (546 bytes, NM v1 header)`, `not plaintext`, `stale memory.bin.new ignored, memory intact (facts: cafe leite)`, `next write replaced memory.bin.new atomically`. Exit 1 from two checks outside A33:
  - A25 `fake got 1999 characters`: the composer itself held 1999 (adb typing dropped a character); flaky input, not the client.
  - A30 `Forçar: user message Pode estimar assim.`: reproduces with `SCENES=a30`; left for a separate follow-up.
- `SCENES=v2 tools/capture-chat.sh dark`: exit 0.
- No UI change; captures refreshed as a by-product and not committed.
