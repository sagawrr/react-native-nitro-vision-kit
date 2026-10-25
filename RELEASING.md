# Releasing

Current version: **0.4.1** (patch — Android option checks and combined analyze safety).

1. Clean tree, changelog looks right.
2. Push and publish:

```sh
git push origin main --follow-tags
npm publish --access public
```

`bun run release` is optional for future cuts (release-it keeps npm publish off).
