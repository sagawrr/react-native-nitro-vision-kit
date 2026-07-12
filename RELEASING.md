# Releasing

Current version: **0.2.2** (patch — upright photos/cutouts).

1. Clean tree, changelog looks right.
2. Push and publish:

```sh
git push origin main --follow-tags
npm publish --access public
```

`bun run release` is optional for future cuts (release-it keeps npm publish off).
