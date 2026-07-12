# Plan 002: Collapse the example into a thin VisionKit harness

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 89a4151..HEAD -- example/src example/assets example/README.md example/package.json`
> If plan 001 has landed, expect diffs in `useVisionRun.ts`, `ensureLocalImagePath.ts`,
> `persistMedia.ts`, and `PlaygroundScreen.tsx` — preserve those correctness
> contracts while thinning. If other in-scope files changed beyond 001, compare
> "Current state" / target shape and STOP on mismatch you cannot reconcile.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED
- **Depends on**: plans/001-run-export-correctness.md (land or fold its contracts in)
- **Category**: tech-debt
- **Planned at**: commit `89a4151`, 2026-07-12

## Why this matters

`example/src` is ~2.6k LOC across 9 UI components for two VisionKit calls. It
behaves like a designed product (zoom canvas, rAF exposing overlay, edge-swipe
mode changes, recipe registry, TypeLadder option chrome, durable keep-dir,
Selective UI that is a no-op on Lift). Maintainers and adopters learn the
playground, not the library. After this plan the example should be a light,
honest harness: pick a photo → Lift or Classify → run → see cutout or labels →
optional Export — roughly **≲800 LOC** under `example/src`, with no dead UX.

## Current state

**Size / fan-in**

- `example/src/PlaygroundScreen.tsx` (~441 LOC) — god screen; wires everything
- Components (~1437 LOC): `ContextControls`, `ExposingOverlay`, `FrameRail`,
  `HeroStage`, `LabelsTape`, `ModeLine`, `RegionSelector`, `WorkbenchEdges`,
  `ZoomCanvas`
- `example/src/recipes/index.ts` — registry for two hardcoded modes
- `example/src/types.ts` — presets (`PIXEL_PRESETS`, `RESULT_PRESETS`,
  `CONFIDENCE_PRESETS`) feeding TypeLadder
- Samples: seven assets under `example/assets/images/` (~12.8MB); unused
  `example/assets/banner.png` (~1.2MB, zero references under `example/`)

**Selective is a lie on Lift** — `HeroStage.tsx:101-127` always shows
Whole photo / Selective; `useVisionRun.ts` cutout branch never passes `region`:

```ts
segmentation = await VisionKit.removeBackground(localPath, {
  trim: options.trim,
  retainMask: false,
  maxPixels: options.maxPixels,
})
```

Region is only applied in the classify branch.

**Duplicate mode UX** — `ModeLine` + `WorkbenchEdges` both change recipe
(`PlaygroundScreen.tsx:215-220` and `:297-305`).

**Persistence weight** — every success runs `keepDurableCopy` into
`DocumentDirectoryPath/nitro-vision-kept` with no cleanup; export also mirrors
to Android Downloads (plan 001 may already remove the mirror).

**Conventions to match after thinning**

- Palette tokens in `example/src/palette.ts` (`ink`, `chalk`, `ember`, `mute`,
  `display`, `danger`, `teal`) — keep using these; do not invent a new theme.
- `useCapabilities` pattern is good — keep it.
- Errors: plain string banners, Pressable to dismiss (see current screen).
- Commit style: conventional commits (`refactor(example): …`).

## Target architecture (the product of this plan)

```
example/src/
  PlaygroundScreen.tsx     # ≤ ~280 LOC: state + layout only
  palette.ts               # keep
  types.ts                 # slim: Mode, RunResult, defaults — no preset ladders
  pickPhoto.ts             # keep; set cropping: false (see steps)
  samplePhotos.ts          # 2–3 small bundled samples
  hooks/
    useCapabilities.ts     # keep as-is
    useVisionRun.ts        # thin: prepare → VisionKit → result; no keep-dir phase
  components/
    FrameRail.tsx          # keep (simplify labels if unused)
    ModeLine.tsx           # keep as the only mode switcher
    HeroStage.tsx          # Image + Before/Cutout toggle + ActivityIndicator
    LabelsTape.tsx         # keep; stop importing recipes/
  utils/
    ensureLocalImagePath.ts  # keep (with 001 fail-safe)
    format.ts                # keep
    persistMedia.ts          # exportImage via CameraRoll only
```

**Delete entirely** (files must not remain):

- `example/src/components/WorkbenchEdges.tsx`
- `example/src/components/ZoomCanvas.tsx`
- `example/src/components/ExposingOverlay.tsx`
- `example/src/components/ContextControls.tsx`
- `example/src/components/RegionSelector.tsx` — whole-image classify only
- `example/src/recipes/index.ts` and the `recipes/` directory
- `example/src/utils/imageCoords.ts`
- `example/src/utils/containRect.ts`
- `example/assets/banner.png`
- At least four of the seven sample images (keep 2–3; see Step 6)

**Hardcoded run options** (no UI ladders):

```ts
const DEFAULTS = {
  trim: true,
  maxPixels: 6_000_000,
  maxResults: 6,
  minConfidence: 0.35,
} as const
```

**Modes**: `'cutout' | 'classify'` only (analyzeImage is plan 005).

**Progress UI**: `ActivityIndicator` + short label text. No rAF clock, no sweep
blade, no multi-phase step list required in the UI (hook may keep a single
`loading` boolean; dropping `RunProgress` is fine if you update call sites).

**Persistence**:

- After `saveToTemporaryFile`, display `toFileUri(tempPath)` directly.
- Do **not** call `keepDurableCopy` / do not write `nitro-vision-kept`.
- `exportImage(uri)` = `CameraRoll.saveAsset` only.
- Best-effort `unlink` temp files when clearing result / on successful export
  if still owned by the session (preserve plan 001 unlink discipline).
- Keep `@dr.pogodin/react-native-fs` only if still needed by
  `ensureLocalImagePath` (Metro `http://` sample URIs). Do not remove
  camera-roll or fs from `package.json` in this plan unless a dep becomes
  completely unreferenced — then remove it and note it in the commit message.
  **Do not** replace the photo picker library in this plan beyond
  `cropping: false` (STOP if you want to swap to another picker).

## Commands you will need

| Purpose    | Command                            | Expected on success |
|------------|------------------------------------|---------------------|
| Typecheck  | `cd example && npm run typecheck`  | exit 0              |
| LOC budget | `find example/src -name '*.ts' -o -name '*.tsx' \| xargs wc -l \| tail -1` | total ≤ 800 (prefer ≤ 700) |
| Dead files | `test ! -e example/src/components/ZoomCanvas.tsx && test ! -e example/src/recipes/index.ts` | exit 0 |
| No region on cutout | `rg -n "useRegion|RegionSelector|WorkbenchEdges|ZoomCanvas|ExposingOverlay|ContextControls|keepDurableCopy|PIXEL_PRESETS" example/src` | no matches |
| Samples    | `du -ch example/assets/images/* \| tail -1` | total ≤ ~2.5M (prefer ≤ 1.5M) |

## Scope

**In scope**:
- Everything under `example/src/`
- `example/assets/` (delete/replace images; delete banner)
- `example/README.md` (match the thinned UX — Lift / Classify / Export only)
- `example/package.json` — only if a dependency becomes unused
- `plans/README.md` (status)

**Out of scope**:
- Root library `src/`, native iOS/Android package code
- `.github/workflows/ci.yml` (plan 003)
- Root `README.md` Example blurb beyond a one-line consistency fix if you
  already touch docs — prefer leaving root README to plan 004
- Adding `analyzeImage` mode (plan 005)
- Swapping `react-native-image-crop-picker` for another library
- Redesigning visual brand (keep `palette.ts`)

## Git workflow

- Branch: `advisor/002-thin-api-harness` (or continue 001 branch if stacking)
- Commits: prefer 2–4 logical commits (`refactor(example): remove theatrical UI`,
  `refactor(example): simplify vision run persistence`,
  `chore(example): shrink sample assets`, …)
- Do NOT push or open a PR unless asked.

## Steps

### Step 0: Confirm plan 001 contracts

If 001 is not DONE, either execute 001 first or fold these into the rewrite:

1. `clearResult` sets `setLoading(false)`
2. `ensureLocalImagePath` fail-safe download (no poisoned cache)
3. Export included in `busy` + stale-completion guard
4. No empty catch in `exportImage`

**Verify**: `rg -n "setLoading\\(false\\)" example/src/hooks/useVisionRun.ts` →
appears inside `clearResult`. If missing and 001 not done → STOP and run 001.

### Step 1: Slim types and delete recipes

1. Rewrite `example/src/types.ts` to only what the thin harness needs, e.g.
   `Mode` (`'cutout' | 'classify'`), `RunResult`, optional slim options type or
   just inline `DEFAULTS` in the hook. Remove `PIXEL_PRESETS`, `RESULT_PRESETS`,
   `CONFIDENCE_PRESETS`, `RecipeDefinition`, `RunProgress` / phase lists if unused.
2. Delete `example/src/recipes/` entirely.
3. Update every import that referenced recipes (expect: `PlaygroundScreen`,
   `ModeLine`, `LabelsTape`, `HeroStage`, `ExposingOverlay` if still present).

For mode metadata, inline in `ModeLine` / screen:

```ts
const MODES = [
  { id: 'cutout' as const, label: 'Lift', runLabel: 'Remove background' },
  { id: 'classify' as const, label: 'Classify', runLabel: 'Classify image' },
]
```

Capability gate: `mode === 'cutout' ? canSegment : canClassify`.

**Verify**: `test ! -e example/src/recipes/index.ts`.  
`cd example && npm run typecheck` — may fail until later steps; if it fails only
on deleted-symbol imports, proceed to Step 2. If unrelated errors → STOP.

### Step 2: Delete theatrical / duplicate components

Delete files listed in Target architecture. Remove all imports from
`PlaygroundScreen` and `HeroStage`.

Rewrite `HeroStage` to approximately:

- Props: `photoUri`, `cutoutUri`, `stageHeight`, `stageView`, `onStageViewChange`,
  `processing`, `disabled` (and nothing for region/zoom/progress steps)
- Body: `Image` with `resizeMode="contain"`, optional checker behind cutout,
  `ActivityIndicator` overlay while `processing`, Before/Cutout row when
  `cutoutUri` exists
- **No** Selective / Whole photo controls

Rewrite `ModeLine` to take a simple modes list + `canSegment` / `canClassify`
without an injected `isAvailable` function.

Keep `FrameRail` and `LabelsTape` (update LabelsTape header to use mode label
string, not `getRecipe`).

**Verify**:

```sh
test ! -f example/src/components/ZoomCanvas.tsx \
  && test ! -f example/src/components/WorkbenchEdges.tsx \
  && test ! -f example/src/components/ExposingOverlay.tsx \
  && test ! -f example/src/components/ContextControls.tsx \
  && test ! -f example/src/components/RegionSelector.tsx
```

→ exit 0.

### Step 3: Thin `useVisionRun`

Rewrite the hook to:

1. Generation token + `clearResult` clears loading (001 contract).
2. `ensureLocalImagePath` then:
   - **cutout**: `VisionKit.removeBackground(path, { trim: true, retainMask: false, maxPixels: 6_000_000 })` → `saveToTemporaryFile('png', 100)` → `toFileUri` as `cutoutUri`; `originalUri` = `toFileUri(localPath)` (or the input photo URI if already displayable). `dispose()` in `finally`. Unlink temp on clear/stale as in 001.
   - **classify**: `VisionKit.classifyImage(path, { maxResults: 6, minConfidence: 0.35 })` — **no region**.
3. Remove `keepDurableCopy`, remove multi-phase `RunProgress` if the UI no longer needs it (`loading: boolean` is enough).
4. Preserve stale-gen discard (`if (gen.current !== myGen) return null`).

**Verify**: `rg -n "keepDurableCopy|useRegion|removeBackground|classifyImage" example/src/hooks/useVisionRun.ts` →
no `keepDurableCopy` / `useRegion`; both VisionKit methods present.  
`cd example && npm run typecheck` → exit 0 after Step 4 wires the screen.

### Step 4: Rewrite `PlaygroundScreen` as a thin shell

State to keep: mode, frames, selected asset/id, picking, export status, local
error, stageView. Remove: options state, region state, recipe registry helpers,
`swipeAllowed`, WorkbenchEdges.

Layout:

1. Top bar: brand + Export (when cutout exists) — keep export busy/gen guards from 001
2. `HeroStage`
3. `FrameRail`
4. `ModeLine`
5. Error banner + `LabelsTape`
6. Bottom dock: single Run button

`busy = picking || loading || exportStatus.kind === 'saving'`.

**Verify**: `cd example && npm run typecheck` → exit 0.  
`rg -n "WorkbenchEdges|ZoomCanvas|ContextControls|useRegion|getRecipe|RECIPES" example/src` → no matches.

### Step 5: Slim persistence + picker

1. `persistMedia.ts`: only `exportImage` (CameraRoll). Remove `keepDurableCopy`
   and dead keep-dir constants if unused.
2. `pickPhoto.ts`: set `cropping: false` (and remove cropper toolbar strings that
   no longer apply). Keep permissions helper as-is.

**Verify**: `rg -n "keepDurableCopy|nitro-vision-kept" example/src` → no matches.  
`rg -n "cropping:" example/src/pickPhoto.ts` → `cropping: false`.  
`npm run typecheck` → exit 0.

### Step 6: Shrink sample media

1. Delete `example/assets/banner.png`.
2. Keep **2 or 3** samples that are useful for subject cutout + classify. Delete
   the rest and update `SAMPLE_PHOTOS` in `samplePhotos.ts`.
3. Re-encode kept images so each is roughly ≤ 1280px on the long edge and
   preferably ≤ ~400KB (sips/ffmpeg/ImageMagick are fine if available). If you
   cannot recompress in this environment, delete the largest files
   (`2.jpeg` at ~6.7MB and `1.jpeg` at ~2.3MB must not remain) and keep only
   currently small ones (`4.jpg`, `6.jpg`, `7.jpg` are candidates) — report in
   the commit message that recompress was skipped.

Remove dead `label` / `nextFrameLabel` if still unused after FrameRail check;
or keep labels if FrameRail shows them — either is fine.

**Verify**: `test ! -f example/assets/banner.png`.  
`du -ch example/assets/images/* | tail -1` → ≤ 2.5M.  
`npm run typecheck` → exit 0.

### Step 7: Update `example/README.md`

Replace the body so it matches the thinned app:

- Pick a frame → Lift or Classify → run on-device
- Lift = cutout; Classify = labels; Export saves cutout to the photo library
- Setup commands stay npm-based; include `bundle install` before pods to align
  with root README (plan 004 will sync root claims)

**Verify**: `rg -n "Selective|analyze|TypeLadder|zoom" example/README.md` → no
misleading Selective/analyze claims. `rg -n "Lift|Classify|Export" example/README.md` → matches.

### Step 8: LOC budget gate

**Verify**:

```sh
find example/src \( -name '*.ts' -o -name '*.tsx' \) -print0 | xargs -0 wc -l | tail -1
```

→ total ≤ 800. If 800–1000, trim more comments/styles before finishing. If
> 1000, STOP and report — do not declare DONE.

## Test plan

- `cd example && npm run typecheck` is the automated gate.
- Manual smoke (operator): Lift on a sample → cutout + Export; Classify → labels;
  switch modes mid-idle; add a library photo with cropping disabled.

## Done criteria

- [ ] `cd example && npm run typecheck` exits 0
- [ ] `example/src` total LOC ≤ 800
- [ ] Files listed for deletion are gone
- [ ] `rg` gate: no `WorkbenchEdges|ZoomCanvas|ExposingOverlay|ContextControls|RegionSelector|keepDurableCopy|PIXEL_PRESETS|getRecipe|RECIPES|useRegion` under `example/src`
- [ ] Selective UI gone; classify is whole-image only
- [ ] Sample assets ≤ ~2.5MB total; `banner.png` gone
- [ ] Plan 001 contracts still hold (`clearResult` clears loading; cache fail-safe; export busy/gen)
- [ ] `example/README.md` matches Lift / Classify / Export
- [ ] `plans/README.md` row 002 → DONE

## STOP conditions

Stop and report if:

- Plan 001 contracts are missing and you cannot fold them safely.
- Removing `keepDurableCopy` makes cutout images fail to display on a platform
  you can verify — report the URI scheme that failed; do not reintroduce the
  full keep-dir design without reporting (a single session-scoped copy is an
  acceptable escape hatch if documented in the PR/commit body).
- LOC remains > 1000 after honest thinning.
- You feel forced to add a third mode (`analyzeImage`) — that is plan 005.
- Native library changes seem required.

## Maintenance notes

- Plan 005 adds `analyzeImage`; keep `Mode` / `useVisionRun` easy to extend with
  one more branch.
- Plan 003 will typecheck this tree in CI — keep `npm run typecheck` green.
- Reviewers: confirm no orphan imports, no leftover assets, and that Export still
  works with temp-file URIs on both platforms.
