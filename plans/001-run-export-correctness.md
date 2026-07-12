# Plan 001: Make example run/export paths cancellation-safe and cache-safe

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 89a4151..HEAD -- example/src/hooks/useVisionRun.ts example/src/utils/ensureLocalImagePath.ts example/src/utils/persistMedia.ts example/src/PlaygroundScreen.tsx`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `89a4151`, 2026-07-12

## Why this matters

The example is the library’s only consumer. Three correctness holes make it look
broken under normal playground use: `clearResult` can freeze `loading` forever,
a failed Metro sample download permanently poisons the on-disk cache, and export
is excluded from the shared busy gate so late export completions can stick a
failed/saved strip after the cutout is gone. Plan 002 will rewrite the UI; these
invariants must be fixed first (and preserved afterward) so the thin harness is
prod-safe.

## Current state

- `example/src/hooks/useVisionRun.ts` — VisionKit run hook with generation token
- `example/src/utils/ensureLocalImagePath.ts` — downloads `http(s)` sample URIs into cache
- `example/src/utils/persistMedia.ts` — durable copies + CameraRoll export
- `example/src/PlaygroundScreen.tsx` — screen; owns export FSM and `busy`

**`clearResult` bumps gen but never clears loading** (`useVisionRun.ts:58-62`):

```ts
function clearResult() {
  gen.current += 1
  setResult(null)
  setError(null)
  setProgress(null)
}
```

**`finally` only clears loading when gen still matches** (`useVisionRun.ts:193-198`):

```ts
} finally {
  segmentation?.dispose()
  if (gen.current === myGen) {
    setLoading(false)
    setProgress(null)
  }
}
```

**Poisoned cache** (`ensureLocalImagePath.ts:30-45`): downloads to `dest` only when
`!(await exists(dest))`. On `statusCode >= 400` it throws but leaves `dest` on
disk; the next call sees `exists(dest)` and returns the bad file.

**Export not in busy** (`PlaygroundScreen.tsx:77`):

```ts
const busy = picking || loading
```

Export uses a separate `exportLock` / `exportStatus` (`PlaygroundScreen.tsx:172-191`)
with no generation check after `await exportImage`.

**Silent Android mirror** (`persistMedia.ts:58-68`): empty `catch { }` after
CameraRoll save.

**Temp leak**: `useVisionRun.ts:127-132` calls `saveToTemporaryFile` then
`keepDurableCopy` (copy). `segmentation.dispose()` clears native buffers only
(see `ios/HybridSegmentationResult.swift:47-50`); the temp PNG is never unlinked.

**Conventions**: TypeScript, React function components, no test runner in
`example/` yet. Verification is `cd example && npm run typecheck`. Error messages
are plain `Error` strings shown in the UI — keep that style. Commit style in
this repo is conventional commits (`fix:`, `chore:`, `feat:`).

## Commands you will need

| Purpose   | Command                         | Expected on success      |
|-----------|---------------------------------|--------------------------|
| Typecheck | `cd example && npm run typecheck` | exit 0, no errors      |
| Grep busy | `rg -n "const busy" example/src/PlaygroundScreen.tsx` | shows export in busy expression |
| Grep cache| `rg -n "unlink|partial" example/src/utils/ensureLocalImagePath.ts` | shows delete-on-failure or rename pattern |

## Scope

**In scope** (the only files you should modify):
- `example/src/hooks/useVisionRun.ts`
- `example/src/utils/ensureLocalImagePath.ts`
- `example/src/utils/persistMedia.ts`
- `example/src/PlaygroundScreen.tsx`
- `plans/README.md` (status row only)

**Out of scope**:
- Deleting UI components / recipes / thinning the screen (plan 002)
- Changing native iOS/Android library code
- Adding a test runner (plan 003 only adds CI typecheck)
- Changing photo picker / permissions

## Git workflow

- Branch: `advisor/001-run-export-correctness`
- Commit style: conventional commits, e.g. `fix(example): clear loading on cancel and harden export/cache`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Fix `clearResult` to cancel loading

In `example/src/hooks/useVisionRun.ts`, update `clearResult` so that after
bumping `gen` it also calls `setLoading(false)` and `setProgress(null)` (progress
is already cleared — keep it). Target shape:

```ts
function clearResult() {
  gen.current += 1
  setResult(null)
  setError(null)
  setProgress(null)
  setLoading(false)
}
```

Leave the `finally` gen-guard as-is (it correctly ignores stale completions).

**Verify**: `rg -n -A6 "function clearResult" example/src/hooks/useVisionRun.ts` →
body includes `setLoading(false)`.

### Step 2: Atomic / fail-safe sample download

In `example/src/utils/ensureLocalImagePath.ts`, stop writing a permanent bad
cache file. Preferred pattern:

1. Download to `${dest}.partial` (or `${dest}.tmp`).
2. If status ≥ 400 (or download throws): `unlink` the partial if it exists, then throw.
3. On success: rename/move partial → `dest` (if the FS helper has no rename,
   `copyFile` then `unlink` partial is fine — use APIs already imported from
   `@dr.pogodin/react-native-fs`; add `unlink` / `moveFile` to the import list as
   needed).
4. If `exists(dest)` already, still return `dest` (happy path unchanged).

Also: if `exists(dest)` but you want belt-and-suspenders, do **not** add content
validation in this plan (STOP if you think you need image probing).

**Verify**: `cd example && npm run typecheck` → exit 0.  
`rg -n "partial|unlink|moveFile" example/src/utils/ensureLocalImagePath.ts` →
matches showing fail-safe write path.

### Step 3: Unlink temp cutout after durable copy

In `useVisionRun.ts` cutout branch, after a successful `keepDurableCopy` of the
temp PNG, best-effort `unlink` the bare temp path from `saveToTemporaryFile`
(strip `file://` if present). Wrap unlink in try/catch so unlink failure does
not fail the run. Import `unlink` from `@dr.pogodin/react-native-fs`.

If `gen.current !== myGen` after creating the temp file but before setting
result, still attempt unlink of that temp path before returning null.

**Verify**: `rg -n "unlink" example/src/hooks/useVisionRun.ts` → at least one
call after the cutout `keepDurableCopy`. `npm run typecheck` → exit 0.

### Step 4: Fold export into busy + ignore stale export completions

In `PlaygroundScreen.tsx`:

1. Change `busy` to include export saving, e.g.
   `const busy = picking || loading || exportStatus.kind === 'saving'`.
2. Add an `exportGen` ref (number). In `handleExport`, bump it, capture
   `const myExport = exportGen.current` and the `cutoutUri` being exported.
3. After `await exportImage(...)`, only call `setExportStatus` if
   `exportGen.current === myExport` **and** the current cutout still equals the
   URI that was exported (read from a ref updated whenever `cutoutUri` changes,
   or compare against the captured URI and ensure `clearResult` / `setMode` /
   `resetPhoto` bump `exportGen`).
4. On `setMode` / `resetPhoto` / `clearResult` paths that invalidate the cutout,
   bump `exportGen` so an in-flight export cannot write UI state.

Do **not** redesign the export FSM beyond this.

**Verify**: `rg -n "const busy" example/src/PlaygroundScreen.tsx` → includes
export saving. `cd example && npm run typecheck` → exit 0.

### Step 5: Stop swallowing Android Downloads-mirror failures

In `persistMedia.ts` `exportImage`: remove the empty `catch { }`. Choose **one**:

- **A (preferred for a light demo)**: delete the entire Android
  `DownloadDirectoryPath` mirror block; CameraRoll save alone is the product
  path. Clean up unused `DOWNLOAD_DIR` / imports if they become dead.
- **B**: keep the mirror, but on failure rethrow a clear Error, e.g.
  `Could not copy cutout to Downloads after saving to the gallery.`

Do not leave an empty catch.

**Verify**: `rg -n "catch" example/src/utils/persistMedia.ts` → no empty
`catch { }` / `catch {\n    }`. `npm run typecheck` → exit 0.

## Test plan

- No Jest in `example/` yet. Characterization is typecheck + grep gates above.
- Manual (operator, not required for Done): start a Lift run, change frame mid-run
  if UI allows — loading must clear; force a bad sample URL once and confirm a
  retry re-downloads after deleting partial.

## Done criteria

- [ ] `cd example && npm run typecheck` exits 0
- [ ] `clearResult` calls `setLoading(false)`
- [ ] `ensureLocalImagePath` does not leave a successful `exists(dest)` after a
      failed download (partial + unlink/rename pattern present)
- [ ] Temp cutout path is unlinked after successful durable copy (best-effort)
- [ ] `busy` includes export saving; export completion is gen/URI guarded
- [ ] No empty catch in `persistMedia.ts` export path
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 001 set to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The excerpts above no longer match live code (drift).
- `@dr.pogodin/react-native-fs` does not expose `unlink` / a rename equivalent —
  report available APIs instead of inventing a native module.
- Fixing export busy appears to require rewriting HeroStage or recipes.
- Typecheck fails twice after a reasonable fix attempt.

## Maintenance notes

- Plan 002 will simplify or remove `keepDurableCopy`. Keep the
  `clearResult` → `setLoading(false)` contract and the cache fail-safe when
  rewriting `useVisionRun` / `ensureLocalImagePath`.
- Reviewers: watch for races where `exportGen` is not bumped on every path that
  clears `cutoutUri`.
- Deferred: PhotoLibraryAdd permission matrix (device-dependent); full unit tests.
