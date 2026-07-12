# Plan 003: Gate the example with CI typecheck

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 89a4151..HEAD -- .github/workflows/ci.yml example/package.json example/package-lock.json`
> If CI or example package manifests changed since this plan was written, compare
> against "Current state" before proceeding; on mismatch, STOP.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: plans/002-thin-api-harness.md (preferred — typecheck a stable tree; may run after 001 if 002 is delayed, as long as `example` typechecks)
- **Category**: dx
- **Planned at**: commit `89a4151`, 2026-07-12

## Why this matters

The example is the only in-repo consumer of `react-native-nitro-vision-kit`
(`file:..` via Metro). Root CI runs library lint/typecheck/build/specs on Bun
and **never** installs or typechecks `example/`. Nitro/API renames can break the
demo while CI stays green. A single `npm run typecheck` job makes the harness
honest.

## Current state

Root CI (`.github/workflows/ci.yml`):

```yaml
jobs:
  ci:
    name: Lint, Typecheck, Build, Specs
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@...
      - uses: oven-sh/setup-bun@...
      - run: bun install
      - run: bun run lint-ci
      - run: bun run typecheck
      - run: bun run build
      - run: bun run specs
```

Example scripts (`example/package.json`):

```json
"scripts": {
  "android": "react-native run-android",
  "ios": "react-native run-ios",
  "start": "react-native start",
  "typecheck": "tsc --noEmit"
}
```

Example uses **npm** (`example/package-lock.json`). Root uses **Bun**
(`bun.lock`). Keep that split — do not convert the example to Bun in this plan.

`example/tsconfig.json` has a dead exclude `"amber"` — remove it while touching
the example config if convenient (one-line cleanup).

## Commands you will need

| Purpose            | Command | Expected |
|--------------------|---------|----------|
| Local example tsc  | `cd example && npm ci && npm run typecheck` | exit 0 |
| Workflow validate  | `rg -n "example" .github/workflows/ci.yml` | shows example typecheck job/step |

## Scope

**In scope**:
- `.github/workflows/ci.yml`
- `example/tsconfig.json` (optional dead `amber` exclude removal only)
- `plans/README.md` (status)

**Out of scope**:
- Adding ESLint to the example
- Adding Jest / Detox / native build CI
- Changing root Bun install
- Dependabot config (optional one-line add is nice-to-have; not required for Done)

## Git workflow

- Branch: `advisor/003-example-ci-typecheck`
- Commit: `ci: typecheck example app on pull requests`
- Do NOT push unless asked.

## Steps

### Step 1: Confirm example typechecks locally

From repo root:

```sh
cd example && npm ci && npm run typecheck
```

**Verify**: exit 0. If it fails, STOP — fix belongs in 001/002, not by weakening CI.

### Step 2: Add an example typecheck job (or step)

Prefer a **separate job** so library Bun CI and example npm CI stay isolated:

```yaml
  example:
    name: Example typecheck
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0 # v7.0.0

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: example/package-lock.json

      - name: Install example dependencies
        working-directory: example
        run: npm ci

      - name: Typecheck example
        working-directory: example
        run: npm run typecheck
```

Notes:

- Pin `actions/setup-node` to a full commit SHA if the repo’s existing style
  pins actions by SHA (checkout and setup-bun already use SHAs). Look up the
  current SHA for `actions/setup-node@v4` used elsewhere in the org/repo; if
  none exists, using `@v4` is acceptable **only if** you cannot resolve a SHA —
  prefer SHA to match `.github/workflows/ci.yml` style.
- Node `22` matches `example/package.json` `engines.node: ">= 22.11.0"`.
- `npm ci` requires `example/package-lock.json` to be committed — it already is.

Do **not** remove or reorder the existing Bun `ci` job.

**Verify**: `rg -n "npm run typecheck|working-directory: example" .github/workflows/ci.yml` → matches.  
YAML still has the Bun `ci` job with `bun run specs`.

### Step 3 (optional): Drop dead `amber` exclude

In `example/tsconfig.json`, change exclude to only `**/node_modules` and
`**/Pods` (remove `"amber"`).

**Verify**: `rg -n amber example/tsconfig.json` → no matches.  
`cd example && npm run typecheck` → exit 0.

## Test plan

- Local: `cd example && npm ci && npm run typecheck`
- After merge: GitHub Actions shows green `Example typecheck` job

## Done criteria

- [ ] `.github/workflows/ci.yml` runs `npm ci` + `npm run typecheck` in `example/`
- [ ] Existing Bun library job still present and unchanged in purpose
- [ ] `cd example && npm run typecheck` exits 0 locally
- [ ] `plans/README.md` row 003 → DONE

## STOP conditions

- Example typecheck fails locally after 002 — do not add `// @ts-nocheck` or
  skipLibCheck hacks to greenwash CI.
- Workflow syntax would require secrets or macOS runners — not needed; STOP if
  someone asks you to add `run-ios` in CI here.

## Maintenance notes

- When example deps change, `package-lock.json` must update or `npm ci` fails
  (desired).
- Follow-up (rejected for now): example lint job, Android assemble CI.
