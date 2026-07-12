# Plan 005: Add a thin Analyze mode that exercises `analyzeImage`

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 89a4151..HEAD -- example/src README.md example/README.md`
> This plan assumes plan 002’s thin harness exists. If `example/src` still has
> `ZoomCanvas` / `recipes/` / `ContextControls`, STOP and run 002 first.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED
- **Depends on**: plans/002-thin-api-harness.md, plans/004-fix-example-docs.md (004 may be updated in the same PR as this plan)
- **Category**: direction
- **Planned at**: commit `89a4151`, 2026-07-12

## Why this matters

The library’s headline API is single-decode `VisionKit.analyzeImage` (root
README Usage; `src/specs/VisionKitFactory.nitro.ts`). The example only calls
`removeBackground` and `classifyImage`, so adopters copy the less efficient
pattern and the demo never proves the marketed path. After the thin harness
lands, adding one third mode is cheap and keeps the example honest.

## Current state

Library API (`src/specs/VisionKitFactory.nitro.ts:31-39`):

```ts
/**
 * Decodes once and runs the requested operations.
 * At least one of `removeBackground` or `classify` is required.
 * When both run, classification uses `segmentation.bounds` if `region` is omitted.
 */
analyzeImage(
  path: string,
  options: AnalyzeImageOptions,
): Promise<ImageAnalysisResult>
```

`AnalyzeImageOptions` (`src/types/AnalyzeImageOptions.ts`):

```ts
export interface AnalyzeImageOptions {
  readonly removeBackground?: BackgroundRemovalOptions
  readonly classify?: ClassificationOptions
}
```

After plan 002, expect roughly:

- `Mode = 'cutout' | 'classify'`
- `useVisionRun` branches for those two only
- `ModeLine` lists two modes
- No recipe registry

**Do not** reintroduce TypeLadder, ZoomCanvas, recipes folder, or Selective UI.

## Commands you will need

| Purpose   | Command | Expected |
|-----------|---------|----------|
| Typecheck | `cd example && npm run typecheck` | exit 0 |
| API use   | `rg -n "analyzeImage" example/src` | ≥ 1 call site in the run hook |
| Modes     | `rg -n "'analyze'|\"analyze\"" example/src` | mode id present in ModeLine + hook |
| LOC guard | `find example/src \( -name '*.ts' -o -name '*.tsx' \) -print0 \| xargs -0 wc -l \| tail -1` | still ≤ ~950 (prefer ≤ 900) |

## Scope

**In scope**:
- `example/src/hooks/useVisionRun.ts`
- `example/src/types.ts` (extend `Mode` / `RunResult` if needed)
- `example/src/PlaygroundScreen.tsx`
- `example/src/components/ModeLine.tsx`
- `example/src/components/HeroStage.tsx` / `LabelsTape.tsx` only if needed to show
  cutout **and** labels together
- `example/README.md` and root `README.md` Example section (mention Analyze)
- `plans/README.md` (status)

**Out of scope**:
- Changing native `analyzeImage` implementation
- Re-adding option ladders / region UI
- New dependencies
- Exceeding ~950 LOC in `example/src` — if you need more, STOP and simplify UI

## Git workflow

- Branch: `advisor/005-analyze-image-mode`
- Commit: `feat(example): add Analyze mode via analyzeImage`
- Do NOT push unless asked.

## Steps

### Step 1: Extend mode union

Add `'analyze'` to the mode type used by the screen / hook / ModeLine.

ModeLine entries (labels are suggestions — keep palette/typography consistent):

| id | label | runLabel |
|----|-------|----------|
| cutout | Lift | Remove background |
| classify | Classify | Classify image |
| analyze | Analyze | Analyze image |

Availability: Analyze requires **both** `canSegment` and `canClassify`. If either
is false, disable/hide Analyze like the other capability gates.

**Verify**: `rg -n "analyze" example/src/components/ModeLine.tsx example/src/types.ts` → matches.

### Step 2: Implement analyze branch in `useVisionRun`

Add a third branch (preserve gen token, `clearResult`→`setLoading(false)`,
temp unlink discipline from 001/002):

```ts
const analysis = await VisionKit.analyzeImage(localPath, {
  removeBackground: {
    trim: true,
    retainMask: false,
    maxPixels: 6_000_000,
  },
  classify: {
    maxResults: 6,
    minConfidence: 0.35,
  },
})
```

Map result:

- If `analysis.segmentation` present: `saveToTemporaryFile('png', 100)` →
  `cutoutUri`; `dispose()` segmentation in `finally` (same as cutout mode).
- `classifications` from `analysis.classifications ?? []`
- `originalUri` from the local/display photo URI (same pattern as other modes)
- `meta` from segmentation source dimensions when present

Read `src/types/ImageAnalysisResult.ts` for exact field names — do not guess if
the live type differs from this sketch; match the exported type.

**Verify**: `rg -n "analyzeImage" example/src/hooks/useVisionRun.ts` → call present.  
`cd example && npm run typecheck` → exit 0.

### Step 3: Wire UI for combined result

When mode is `analyze` and a result exists:

- `HeroStage` shows cutout / Before toggle when `cutoutUri` is set (reuse cutout UI)
- `LabelsTape` shows classifications when non-empty (already does)

On successful analyze run, set `stageView` to `'result'` if a cutout exists
(same as Lift).

Do **not** add a fourth page or sheet.

**Verify**: `cd example && npm run typecheck` → exit 0.  
Manual expectation (document in commit body): Analyze shows cutout + labels.

### Step 4: Update docs

In both `README.md` (Example section) and `example/README.md`, mention three
modes: Lift, Classify, Analyze — with Analyze = single-decode
`analyzeImage` (cutout + labels).

**Verify**: `rg -n "Analyze|analyzeImage" README.md example/README.md` → both files mention it.

### Step 5: LOC guard

**Verify**: total `example/src` LOC ≤ 950. If higher, trim styles/duplication
before DONE.

## Test plan

- `cd example && npm run typecheck`
- Manual: Run Analyze on a sample with a clear subject — expect cutout image and
  at least one classification row when the device supports both capabilities.
- Manual: On a device/simulator without segmentation, Analyze should be unavailable
  and Lift disabled (capability gate).

## Done criteria

- [ ] Example calls `VisionKit.analyzeImage` for Analyze mode
- [ ] ModeLine exposes Analyze gated on both capabilities
- [ ] Combined cutout + labels render without new theatrical components
- [ ] `cd example && npm run typecheck` exits 0
- [ ] `example/src` LOC ≤ 950
- [ ] Root + example READMEs mention Analyze accurately
- [ ] `plans/README.md` row 005 → DONE

## STOP conditions

- Plan 002 not done (theatrical UI still present).
- `ImageAnalysisResult` shape does not include fields you expected — adapt to the
  real type; if segmentation/classifications are missing from the type entirely,
  STOP and report (library bug / version skew).
- Fix seems to require native changes.
- You are about to re-add recipes registry, ZoomCanvas, or option ladders.

## Maintenance notes

- Keep hardcoded options identical across cutout / classify / analyze so the demo
  stays comparable.
- Reviewers: ensure `dispose()` still runs for analyze segmentation; ensure stale
  gen discard still applies after the slower combined call.
- Follow-up explicitly deferred: Selective region for classify-only; exposing
  `analyzeImage` options in UI.
