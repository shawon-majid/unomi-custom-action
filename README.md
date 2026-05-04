# unomi-test

Local spike of [Apache Unomi](https://unomi.apache.org/) 2.x:

- Browser page sends `view` events to Unomi.
- A rule increments `profile.pageViewCount` on every view.
- A **custom Groovy action** POSTs to a local Bun webhook every time the count is a multiple of 10.

```
web (bun :3000)  ──view──▶  Unomi (docker :8181)  ──webhook──▶  server (bun :3001)
```

## Prereqs

- Docker (compose v2)
- Bun ≥ 1.1

## Quickstart

```bash
# 1. start Unomi + Elasticsearch
bun run up

# 2. install dev deps (just types)
bun install

# 3. start the webhook receiver (terminal A)
bun run server

# 4. upload the custom Groovy action + rule (terminal B, one-shot)
bun run setup

# 5. start the web page (terminal C)
bun run web
# → open http://localhost:3000
```

Reload the page or click **Send view event**. Every 10th view, terminal A logs:

```
[unomi-hook] profile=<uuid> count=10 at=2026-…
```

## Files

| Path | What |
|---|---|
| `docker-compose.yml` | Elasticsearch 7.17.5 + `apache/unomi:2.6.0` |
| `web/index.html` | Sends `view` events via `/cxs/context.json`, manages local profile/session ids |
| `web/serve.ts` | Bun static server on `:3000` |
| `server/index.ts` | Bun webhook receiver on `:3001` |
| `unomi/action.groovy` | Custom action: HTTP POST when `pageViewCount % threshold == 0` |
| `unomi/rule.json` | Rule on `view` events: `incrementPropertyAction` then the custom action |
| `unomi/setup.sh` | Waits for Unomi, uploads action, posts rule |

## Useful commands

```bash
docker compose logs -f unomi              # tail Unomi (Karaf) logs
curl -u karaf:karaf http://localhost:8181/cxs/cluster
curl -u karaf:karaf http://localhost:8181/cxs/rules | jq '.[].metadata.id'
# Inspect a profile (the page logs its id):
curl -u karaf:karaf http://localhost:8181/cxs/profiles/<profileId> | jq
bun run down                              # stop the stack
```

## Notes

- Default Unomi credentials: `karaf:karaf`.
- The page uses `/cxs/context.json` directly with locally-managed profile/session ids (in `localStorage`) — simpler than the bundled `unomi-web-tracker.min.js` and avoids cross-origin cookie quirks. Click **New profile** to wipe ids.
- `host.docker.internal` is mapped via `extra_hosts` so the Unomi container can reach the host's Bun server on `:3001`.
- The Groovy action reads `event.profile.properties.pageViewCount` *after* `incrementPropertyAction` runs (action order in the rule matters).

## Gotchas hit while building this

Things Unomi 2.6 quietly rejects, in case anyone hits the same wall:

1. **Scopes must be registered** before events can use them. `validateScope: true` is on every event/item field. Hence `setup.sh` POSTs to `/cxs/scopes` first.
2. **`itemId` regex** is `^(\w|[-_@\.]){0,60}$` — `/` is rejected. We use `home`, not `/`.
3. **Groovy action filename matters**: the script is stored under its filename, and the dispatcher resolves `groovy:foo` by looking up a script named `foo`. The file *must* be `<id>.groovy`. Don't call it `action.groovy`.
4. **`actionExecutor` casing must match the script lookup name.** `groovy:notifyOnPageViewMilestoneAction` ≠ `groovy:NotifyOnPageViewMilestoneAction`.
5. **`incrementPropertyAction.propertyName`** operates inside `profile.properties` already — pass `pageViewCount`, not `properties.pageViewCount` and definitely not `properties(pageViewCount)` (that triggers BeanUtils indexed-property parsing and throws).
6. **`view` event JSON schema is strict** (`unevaluatedProperties: false`). Source must be a `site` item, target must be a `page` item; only the documented fields are allowed.
