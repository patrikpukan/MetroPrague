# Golemio API integration research

Last checked: 2026-10-14. This is a working report; update it as access, live responses, and product choices are verified.

## Current status

| Area | Finding | Status |
| --- | --- | --- |
| Access | Golemio public transport API uses a generated token in `X-Access-Token`. | Owner's local key works; authenticated HTTP 200 responses verified. |
| Account | Free registration and email verification precede key generation. | Owner completed registration and key generation. |
| Departures | `GET /v2/pid/departureboards` supports metro departures, realtime prediction, delay, cancellation, and information text. | Manually downloaded departure snapshots; up to 30 departures per station in the next 60 minutes. |
| Stops | Each existing app station slug maps to metro GTFS platform stop IDs. | Full 58-station physical A/B/C network; current platform IDs and metadata load from GTFS endpoints. |
| App | Debug uses Golemio when a local key is set; release uses mocks. | Debug reads persisted snapshots and reloads only from Settings. Release still uses mocks. |

## Decisions from project owner (2026-09-26)

- First release scope is **metro A, B, and C only**. Do not expand the domain or API mapping to trams for this phase.
- Station/platform ID mapping was initially undecided. The initial 20-station mapping was verified in September. The full A/B/C physical topology is now bundled, with platform metadata and travel directions refreshed from the API on demand.
- The owner will check the key portal's terms, scopes, and actual per-key limits after registration.
- Backend use remains a product/operations decision. Direct Android access is acceptable for a first working integration if the terms permit it and the owner accepts that the shared key is recoverable from the APK.

## Local development setup

1. The owner registered at the [Golemio API key portal](https://api.golemio.cz/api-keys/), verified email, and generated a key for the [public transport output API](https://api.golemio.cz/pid/docs/openapi/).
2. The key is in the existing, gitignored root `local.properties` as `GOLEMIO_API_KEY=...`; keep the `sdk.dir` entry too. Never paste the token into chat, an issue, a test fixture, or a commit.
3. Run `./gradlew assembleDebug` and install the debug APK. A nonblank local key selects the saved Golemio station/departure repositories in debug. Open **Settings → Metro data → Reload data** to download the initial dataset or replace it with current data. Opening Home/Search or restarting the app never fetches API data. Without it, debug uses mock departures. Release always uses mocks and its generated `golemio_api_key_base64` string resource is empty. A debug APK **does** contain the key and must not be distributed.
4. The owner still needs to review the [portal terms](https://api.golemio.cz/api-keys/terms-and-conditions). That page requires JavaScript, so this report does **not** claim a verified redistribution or attribution rule. Confirm applicable terms and attribution before publication.

No paid plan, organization approval, or extra account was found in the public registration instructions. If the portal asks for any additional step, record it here.

## API facts relevant to Metro Prague

- Production base URL: `https://api.golemio.cz`. The [public transport OpenAPI specification](https://api.golemio.cz/docs/static/vp-output-gateway/openapi.json) also lists a test server; use production data for the eventual user-facing integration.
- Authentication: send the generated value as the `X-Access-Token` request header. The [API documentation](https://api.golemio.cz/pid/docs/openapi/) states a default limit of **20 requests per 8 seconds per key**. Avoid polling separately for every favorite; share or batch requests and respect HTTP 429.
- Departure query: `GET /v2/pid/departureboards` requires at least one of `ids[]` (GTFS stop IDs), `aswIds[]`, `cisIds[]`, or `names[]`. It accepts up to 100 stops in one request. `names[]` is an exact, case and whitespace sensitive match and can match the same name in another town. Stable IDs are preferable for production. Useful parameters include `minutesAfter`, `limit`, `mode=departures`, and `order=real`.
- Response: `departures[]` contains `route.type` (`1` = metro, `0` = tram), `route.short_name` (A/B/C for metro), `trip.id`, `trip.headsign`, `trip.is_at_stop`, `trip.is_canceled`, `departure_timestamp.scheduled`, `departure_timestamp.predicted`, and `delay.is_available`/`seconds`. `infotexts[]` has Czech `text` and optional English `text_en`. A valid response can have an empty departures array.
- GTFS static data are published by ROPID and described as covering approximately the next 12 days, with daily updates. Stop/route metadata therefore needs refreshing rather than hardcoded permanent assumptions. Golemio's [GTFS stops endpoint](https://api.golemio.cz/pid/docs/openapi/) returns a GeoJSON feature collection. Its stops expose platform level `stop_id` and `parent_station`; a user-facing station can comprise multiple stops.
- A credentialless request returned HTTP 401 on 2026-09-26. Authenticated requests returned HTTP 200 on 2026-09-28 for all 20 current station names and for mapped stop-ID queries at Anděl, Můstek, and Háje.

## Current implementation: manual reload and local storage

The development app calls Golemio directly with `X-Access-Token`; there is no backend. A debug build with a nonblank local key injects the saved-data repositories. Builds without a development key and release builds use mocks. Existing station slug IDs, including `ip-pavlova`, remain compatible with persisted favorites.

**Settings → Metro data → Reload data** runs this sequence:

1. Fetch `/v2/gtfs/routes` and select metro A/B/C.
2. Fetch `/v2/gtfs/stops` for the complete physical station list, retaining platform IDs, coordinates, parent stations, platform codes, accessibility and other returned metadata. `MetroNetwork` bundles station names and order: 17 A memberships, 24 B memberships, 20 C memberships, and 58 unique stations after merging interchanges. This retains stations such as Flora even when current GTFS trip patterns omit them.
3. Page `/v2/gtfs/trips` using six known central metro platforms and fetch one `/v2/gtfs/trips/{id}` detail per distinct route/shape/direction, with stops and stop times. These patterns establish current platform travel directions and confirm metro membership, including arrival-only platforms. Other platform IDs are accepted only through the verified metro fallback list, retaining known closed-station platforms without admitting same-name tram or bus stops. A shared reversing platform (observed at Skalka) retains an unspecified platform direction; each actual departure is assigned using its destination’s position on the line first, then an unambiguous platform or previous stop. New physical stations or changed topology require updating the bundled network; this is not automatic discovery of arbitrary future extensions.
4. Fetch `/v2/pid/departureboards` for every station's current platform IDs, with `minutesAfter=60`, `limit=30`, `mode=departures`, `order=real`. Save raw board responses, scheduled/predicted times, delays, cancellation and at-stop flags, headsigns, service texts, and a per-board fetch timestamp.
5. Validate and atomically replace one typed JSON DataStore snapshot at `files/datastore/metro-data.json`. A process-wide context delegate owns the store, including across Hilt test component recreation. The snapshot is excluded from cloud backup and device transfer. No API key is written into the snapshot. A failed download or cancellation of the application job leaves the previous complete snapshot untouched. Leaving Settings cancels only its waiter; the application-scoped reload continues and finishes its atomic write.

The shared HTTP client serializes requests and spaces them by at least 600 ms after each response. Duplicate reloads are ignored. A Retry-After cooldown reports rate limiting promptly instead of leaving a new reload waiting through a long delay. HTTP 401/403 and 429 get distinct Settings errors, and a numeric `Retry-After` delays the next request. Settings shows catalogue loading, departure progress, saved station/departure counts, and the last successful reload time.

Home and Search only observe the saved snapshot. A local 15-second timer updates departure time labels without network requests. Past departures are filtered out except for trains marked at-stop while the snapshot remains fresh; snapshots five minutes old show **Reload departures in Settings**. Stations remain searchable offline; the bundled full topology is also available before the first reload so existing favorites still resolve. Service texts are saved in `DepartureBoard.infoTexts`; the current Home/Search layouts do not yet display a disruption panel.

This stores the metadata and current departure data needed by the metro app. It does not download a complete multi-day timetable, all city transport modes, vehicle-position feeds, or every Golemio dataset. Representative trip stop times are metadata for platform/direction mapping, not a complete offline schedule.

## Is a backend necessary?

**No, not technically.** The Android app can call Golemio over HTTPS and send `X-Access-Token` itself. The [Golemio registration guide](https://operator-ict.gitlab.io/golemio/documentation/en/open-data-api/) describes free registration, and the [API documentation](https://api.golemio.cz/pid/docs/openapi/) states a default limit of 20 requests per 8 seconds **per key**. Free access does not mean that a key is private when shipped in an APK, or that all app installations get independent limits.

| Option | What it provides | Trade-off / recommendation |
| --- | --- | --- |
| App calls Golemio with one app key | No server cost or operations; shortest path to live departures. | Anyone can recover/reuse the shared key; all installs share its quota. Reasonable for a small first release if provider terms allow it and outage/rotation are acceptable. Use foreground-only refresh, batching, caching, 429 backoff, and a clear stale/offline state. |
| Each user supplies their own Golemio key | No shared app key or backend. | Registration burden for every rider makes this a poor default UX; useful only as a developer/advanced option. |
| Small proxy calls Golemio | Keeps upstream key off devices; can cache common boards, apply limits, and rotate the key centrally. | Hosting, monitoring, and abuse protection become our responsibility. Prefer if public usage grows or provider terms require key confidentiality. A public proxy endpoint still needs protection from abusive callers. |
| [PID GTFS Static feed](https://pid.cz/en/opendata/) for scheduled times | Public daily timetable download avoids an app-held Golemio key for **scheduled** service. PID documents metro station structure and a roughly 14-day timetable horizon. | Does not by itself provide live prediction, cancellation, or delay. It could be an offline schedule layer alongside Golemio, not a drop-in replacement for the departure board. |

**Current choice:** use direct Golemio calls only in local debug builds, then decide on a proxy before distributing the app. Do not commit or distribute the token. The repository interface allows transport to change later.

## Free or no-additional-cost proxy hosting (checked 2026-09-27)

**Recommended free option: [Cloudflare Workers Free](https://developers.cloudflare.com/workers/platform/pricing/).** A Worker is enough for an HTTPS endpoint that accepts a limited station/stop request, adds the Golemio token from a [Worker secret](https://developers.cloudflare.com/workers/configuration/secrets/), calls Golemio, and returns the response. The account receives a public [`workers.dev` address](https://developers.cloudflare.com/workers/configuration/routing/workers-dev/) without needing to buy or move a domain. A small JavaScript/TypeScript Worker is the most direct fit for this narrow proxy; it does not require running a JVM or full Python server.

At the time checked, the free tier allows **100,000 Worker requests per day per account**, **10 ms CPU per invocation**, and **50 subrequests per invocation**. Time waiting on a network `fetch()` does not count as CPU time. The request limit resets at midnight UTC; Cloudflare returns **Error 1027** after the free daily cap, so the app must handle a proxy outage. The paid Workers plan starts at **$5 USD/month**, but a simple proxy can remain on the free plan while within its limits. [Pricing](https://developers.cloudflare.com/workers/platform/pricing/) · [Limits](https://developers.cloudflare.com/workers/platform/limits/)

Cloudflare's limit is **not** extra Golemio capacity: the upstream key still has Golemio's default **20 requests per 8 seconds per key**. A short shared cache can reduce upstream calls; [Workers supports caching](https://developers.cloudflare.com/workers/examples/cache-using-fetch/). Cache behavior and freshness need testing against Golemio's response headers. The endpoint must allow only known routes/parameters, cap requested stops, bound response size/time, avoid sending the upstream token to the app, and apply some abuse control. A public proxy URL can be called by people outside the app, and an app-embedded proxy password would be extractable too.

**No additional hosting bill option:** if the owner's previously used Hostinger web hosting is still active and supports PHP, a small PHP endpoint could make the HTTPS request using cURL. Hostinger documents PHP hosting and [cURL connectivity](https://www.hostinger.com/support/which-web-standards-and-connectivity-features-are-supported-at-hostinger/). This reuses an existing subscription but is not a truly free independent service; its plan limits, secret placement, caching, and site reliability need verification before choosing it. Do not replace an existing site or publish a token under `public_html`.

**Recommendation for this project:** keep direct Golemio calls for local integration work. If the owner wants a proxy for public distribution without a new monthly bill, start with a single Cloudflare Worker and a small allowlisted endpoint, then measure Worker and Golemio requests. Reusing an already-paid Hostinger plan is a fallback if its PHP setup is convenient. Neither option has been deployed or tested with the owner's accounts yet.

## Initial key verification before the full catalogue change (2026-10-14)

- The current `GOLEMIO_API_KEY` in gitignored `local.properties` authenticated successfully. Tested the exact query used by `boardUrl`: mapped `ids[]`, `minutesAfter=60`, `limit=30`, `mode=departures`, and `order=real` against production `/v2/pid/departureboards`.
- All **20 prototype stations** returned **HTTP 200**, totaling **540 departures**. Every returned departure was metro route type `1` on A, B, or C and used a requested platform ID. Responses covered **41 of the 42 mapped platforms**; Háje returned `U286Z101P` only. Absence of departures for its second platform does not establish an invalid mapping.
- All 540 departures had predicted timestamps; **383** had available numeric delay values. Trip IDs, headsigns, scheduled timestamps, stop IDs, at-stop flags, and cancellation flags matched the app's expected structure. No information texts were returned, so populated disruption text was not verified live.
- A temporary JVM smoke test fed the captured responses through the actual `GolemioDepartureBoardParser`: all 540 departures parsed and had a nonnull mapped direction. Current short-turn headsigns included **Skalka** on A and **Kačerov** on C.
- `./gradlew assembleDebug testDebugUnitTest` passed with the existing cached JDK 21. The generated debug `golemio_api_key_base64` resource was decoded locally and confirmed to match the current key without printing either value. The temporary test was removed after verification; no token or live response body was added to the repository.
- This verifies API access, returned data, parser compatibility, and debug build configuration from the development machine. Android device networking/UI, offline recovery, full station catalogue coverage, and request coordination remain pending.

## Validation log and next checks

1. **Done:** authenticated requests for all 20 current station names identified metro platform IDs and current headsigns; ID-only requests for Anděl, Můstek, and Háje returned only metro route type `1`. No token or full response body was committed.
2. **Done:** deterministic JVM tests cover parser success, empty boards, non-metro filtering, offset timestamps, short-turn platform direction, HTTP 429 represented as unavailable, unknown stations, and encoded stop-ID URLs. `./gradlew assembleDebug testDebugUnitTest`, `./gradlew assembleRelease`, and `./gradlew assembleDebugAndroidTest` passed on 2026-09-28. The instrumentation APK compiled but could not run without an attached device. Debug generated configuration contains the local key; release generated configuration does not.
3. **Current validation:** manual reload, retained data after failure/cancellation, disk reopening, and expired departure snapshots have deterministic tests. Emulator validation results are recorded below.
4. **Pending:** add deterministic tests for 401, timeout, malformed responses, and retry/backoff behavior when those policies are implemented. Do not deliberately flood production to provoke 429.
5. **Done:** Home/Search issue no requests. A single manual reload coordinates all metadata and board requests. Measure reload duration and consider batching metadata further if needed.

## Full catalogue and emulator validation (2026-10-14)

- `./gradlew assembleDebug testDebugUnitTest` passed: **93 deterministic JVM tests**, plus a temporary check of captured real API metadata. The captured-data check parsed **58 stations and 122 platforms**; the temporary test was removed afterward.
- `./gradlew connectedDebugAndroidTest` passed: **25 instrumentation tests**, including reload-button callbacks, disabled loading state, progress, error messaging, and Hilt navigation/component recreation. The available Pixel 10 Pro emulator runs **API 37**; API 36 validation was subsequently completed in the Copilot review follow-up below.
- A real Settings button reload authenticated with the local key and saved **3 routes, 58 unique stations, 122 platforms, 24 trip patterns, 58 departure boards, and 1,605 departures**. The JSON snapshot occupied **1,517,510 bytes**. The station memberships were A=17, B=24, C=20. Flora's current board was empty, while its station and platforms remained searchable.
- Live validation exposed Skalka's `U953Z101P` being used for departures in both directions across GTFS patterns. The loader now retains this shared platform with an unspecified direction and resolves each departure by its actual destination; a deterministic regression test covers both directions.
- With emulator Wi-Fi and mobile data disabled, a forced app restart restored the same saved counts. Petřiny opened from cached Search data with both travel directions and departure estimates. A manual reload while offline reported failure, and the persisted snapshot remained **byte-for-byte unchanged**, verified by SHA-256. Connectivity was restored to its original enabled state. After the saved boards passed their five-minute age limit, both Petřiny directions displayed **Reload departures in Settings**, confirming that old countdowns were not presented as current data.
- The stored snapshot was checked locally and contained **no API key**. No token or raw live response body was committed. A Settings screenshot is available in the ignored build output `app/build/reports/metro-data/reload-settings.png`.

## Copilot review follow-up (2026-10-14)

Both findings on [PR #6](https://github.com/patrikpukan/MetroPrague/pull/6) are addressed. Platform membership now requires evidence from metro trip stop times or the verified metro-only fallback list, rather than station name/parent alone. Fresh at-stop departures survive past timestamp filtering; the exception ends when the snapshot reaches five minutes old.

The combined implementation retains Claude's initial-catalogue fallback, application-scoped reload, readable error states, and prompt cooldown handling. Debug/APK builds and **101 deterministic JVM tests** passed; **25 instrumentation tests passed on API 36**. A temporary replay of captured real data retained all **58 stations, 122 platforms, and 1,605 departures** with mapped directions. See [the detailed review report](copilot-review-follow-up.md) for provenance and regression coverage.

## Open questions / validation log

- How should future physical metro extensions update the bundled topology and six bootstrap platform IDs?
- Does the portal impose additional terms, key scopes, or a different limit on the owner's new key? Owner will check.
- Which proxy contract and hosting choice should replace the direct debug transport before public distribution?
- Manual reload, offline reopening, and retained data after failure are verified. Future public-release refresh policy and larger-scale request volume still need a product decision.

## Sources

- [Golemio public transport API documentation](https://api.golemio.cz/pid/docs/openapi/)
- [Public transport OpenAPI JSON specification](https://api.golemio.cz/docs/static/vp-output-gateway/openapi.json)
- [Golemio registration and open data guide](https://operator-ict.gitlab.io/golemio/documentation/en/open-data-api/)
- [Golemio API key portal](https://api.golemio.cz/api-keys/)
- [API key portal terms](https://api.golemio.cz/api-keys/terms-and-conditions)
- [PID open data and GTFS Static documentation](https://pid.cz/en/opendata/)
- [Cloudflare Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/), [limits](https://developers.cloudflare.com/workers/platform/limits/), [secrets](https://developers.cloudflare.com/workers/configuration/secrets/), and [`workers.dev` routing](https://developers.cloudflare.com/workers/configuration/routing/workers-dev/)
- [Hostinger web hosting connectivity](https://www.hostinger.com/support/which-web-standards-and-connectivity-features-are-supported-at-hostinger/)
