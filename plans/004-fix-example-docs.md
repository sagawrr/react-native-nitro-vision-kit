# Plan 004: Align example docs with the thin harness

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 89a4151..HEAD -- README.md example/README.md`
> If either README changed since this plan was written, read the live files and
> adjust only the mismatched claims — do not rewrite unrelated sections.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: plans/002-thin-api-harness.md (so docs describe the thinned UX)
- **Category**: docs
- **Planned at**: commit `89a4151`, 2026-07-12

## Why this matters

Root README tells adopters the example can “run analyze / remove background /
classify,” but the app only exposes Lift (remove background) and Classify — and
after plan 002 it still will not expose analyze until plan 005. Stale setup
commands also diverge (`bundle install` vs not; `npx react-native run-ios`
vs `npm run ios`). Wrong docs are worse than missing docs for a library demo.

## Current state

Root `README.md` Example section (approx. lines 22–35):

```markdown
## Example

A bare React Native demo lives in [`example/`](./example):

```sh
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npx react-native run-ios --device
# or
npx react-native run-android
```

Pick a photo, then run analyze / remove background / classify. See [`example/README.md`](./example/README.md).
```

`example/README.md` (pre-002) mentions Lift / Classify / Selective / Export and
setup without `bundle install`. After 002 it should already describe Lift /
Classify / Export without Selective — this plan syncs the **root** README and
makes both setup blocks identical.

Package manager note (document, do not change tooling):

- Repo root: Bun (`bun.lock`, CI)
- Example app: npm (`example/package-lock.json`)

## Commands you will need

| Purpose | Command | Expected |
|---------|---------|----------|
| Claim check | `rg -n "analyze|Selective|remove background|Lift|Classify" README.md example/README.md` | root must not claim analyze until 005; both describe Lift/Classify |

## Scope

**In scope**:
- `README.md` (Example section only — do not rewrite Installation/Usage API docs)
- `example/README.md`
- `plans/README.md` (status)

**Out of scope**:
- Changing Usage examples for `analyzeImage` in the root README API section
  (those document the library, not the demo)
- Plan 005 feature work
- CI / code changes

## Git workflow

- Branch: `advisor/004-example-docs`
- Commit: `docs: align example README with thin harness`
- Do NOT push unless asked.

## Steps

### Step 1: Read live READMEs after 002

Open `example/README.md` and `README.md`. Confirm 002 has removed Selective /
theatrical claims from `example/README.md`. If 002 is not done, STOP.

### Step 2: Rewrite root Example blurb

Replace the Example section’s closing sentence and align run commands with
`example/README.md`. Target content (adapt wording slightly if brand strings
differ, but keep facts):

```markdown
## Example

A bare React Native demo lives in [`example/`](./example):

```sh
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios
# or
npm run android
```

Pick a frame, choose **Lift** (remove background) or **Classify**, then run
on-device. **Export** saves a cutout to the photo library. See
[`example/README.md`](./example/README.md).

> The example app uses npm; the library package at the repo root uses Bun.
```

If plan 005 has already landed and the demo includes Analyze, then and only then
mention Analyze — otherwise do **not** say “analyze”.

**Verify**: `rg -n "analyze / remove|run analyze" README.md` → no matches.  
`rg -n "Lift|Classify" README.md` → matches in Example section.

### Step 3: Sync `example/README.md` setup block

Make the setup shell block match root (include `bundle install`). Keep the
product description consistent with the thinned app (no Selective, no zoom
theater). Mention Export briefly.

**Verify**: Both READMEs’ setup blocks include `bundle install` and
`bundle exec pod install`.  
`rg -n "Selective" example/README.md README.md` → no matches (unless you
intentionally document classify-only region later — do not).

## Test plan

- Docs-only; no code tests.
- Skim as a new adopter: can you run the demo and know which two modes exist?

## Done criteria

- [ ] Root Example section does not claim an analyze demo mode (unless 005 done)
- [ ] Both READMEs share the same setup commands (`bundle install` + npm run ios/android)
- [ ] Package-manager split (root Bun / example npm) is noted once
- [ ] `plans/README.md` row 004 → DONE

## STOP conditions

- 002 not landed and example still has Selective / heavy UX — do not document the
  old UI as the target.
- Request to delete library `analyzeImage` docs from Usage — out of scope.

## Maintenance notes

- When 005 adds Analyze to the demo, update **both** README Example blurb and
  `example/README.md` in the same PR as the feature.
