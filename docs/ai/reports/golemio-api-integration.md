# Golemio API integration research

Last checked: 2026-09-28. This is a working report; update it as access, live responses, and product choices are verified.

## Current status

| Area | Finding | Status |
| --- | --- | --- |
| Access | Golemio public transport API uses a generated token in `X-Access-Token`. | Owner's local key works; authenticated HTTP 200 responses verified. |
| Account | Free registration and email verification precede key generation. | Owner completed registration and key generation. |
| Departures | `GET /v2/pid/departureboards` supports metro departures, realtime prediction, delay, cancellation, and information text. | Live responses checked for the current 20-station catalogue. |
| Stops | Each existing app station slug maps to metro GTFS platform stop IDs. | 42 IDs across 20 stations verified on 2026-09-28; mapping is a snapshot. |
| App | Debug uses Golemio when a local key is set; release uses mocks. | Debug/release builds and JVM tests pass; no emulator/device flow checked. |

## Decisions from project owner (2026-09-26)

- First release scope is **metro A, B, and C only**. Do not expand the domain or API mapping to trams for this phase.
- Station/platform ID mapping was initially undecided. The current 20-station catalogue now has a verified local mapping; a complete metro catalogue and ongoing ID refresh remain future work.
- The owner will check the key portal's terms, scopes, and actual per-key limits after registration.
- Backend use remains a product/operations decision. Direct Android access is acceptable for a first working integration if the terms permit it and the owner accepts that the shared key is recoverable from the APK.

## Local development setup

1. The owner registered at the [Golemio API key portal](https://api.golemio.cz/api-keys/), verified email, and generated a key for the [public transport output API](https://api.golemio.cz/pid/docs/openapi/).
2. The key is in the existing, gitignored root `local.properties` as `GOLEMIO_API_KEY=...`; keep the `sdk.dir` entry too. Never paste the token into chat, an issue, a test fixture, or a commit.
3. Run `./gradlew assembleDebug` and install the debug APK. A nonblank local key selects the Golemio departure repository in debug. Without it, debug uses mock departures. Release always uses mocks and its generated `BuildConfig` contains no Golemio key. A debug APK **does** contain the key and must not be distributed.
4. The owner still needs to review the [portal terms](https://api.golemio.cz/api-keys/terms-and-conditions). That page requires JavaScript, so this report does **not** claim a verified redistribution or attribution rule. Confirm applicable terms and attribution before publication.

No paid plan, organization approval, or extra account was found in the public registration instructions. If the portal asks for any additional step, record it here.

## API facts relevant to Metro Prague

- Production base URL: `https://api.golemio.cz`. The [public transport OpenAPI specification](https://api.golemio.cz/docs/static/vp-output-gateway/openapi.json) also lists a test server; use production data for the eventual user-facing integration.
- Authentication: send the generated value as the `X-Access-Token` request header. The [API documentation](https://api.golemio.cz/pid/docs/openapi/) states a default limit of **20 requests per 8 seconds per key**. Avoid polling separately for every favorite; share or batch requests and respect HTTP 429.
- Departure query: `GET /v2/pid/departureboards` requires at least one of `ids[]` (GTFS stop IDs), `aswIds[]`, `cisIds[]`, or `names[]`. It accepts up to 100 stops in one request. `names[]` is an exact, case and whitespace sensitive match and can match the same name in another town. Stable IDs are preferable for production. Useful parameters include `minutesAfter`, `limit`, `mode=departures`, and `order=real`.
- Response: `departures[]` contains `route.type` (`1` = metro, `0` = tram), `route.short_name` (A/B/C for metro), `trip.id`, `trip.headsign`, `trip.is_at_stop`, `trip.is_canceled`, `departure_timestamp.scheduled`, `departure_timestamp.predicted`, and `delay.is_available`/`seconds`. `infotexts[]` has Czech `text` and optional English `text_en`. A valid response can have an empty departures array.
- GTFS static data are published by ROPID and described as covering approximately the next 12 days, with daily updates. Stop/route metadata therefore needs refreshing rather than hardcoded permanent assumptions. Golemio's [GTFS stops endpoint](https://api.golemio.cz/pid/docs/openapi/) returns a GeoJSON feature collection. Its stops expose platform level `stop_id` and `parent_station`; a user-facing station can comprise multiple stops.
- A credentialless request returned HTTP 401 on 2026-09-26. Authenticated requests returned HTTP 200 on 2026-09-28 for all 20 current station names and for mapped stop-ID queries at Anděl, Můstek, and Háje.

## Codebase findings and implementation path

The current [`RepositoryModule`](../../../app/src/main/java/dev/pukan/metroprague/di/RepositoryModule.kt) binds both station and departure repositories to mocks. [`Station`](../../../app/src/main/java/dev/pukan/metroprague/domain/model/Station.kt) uses internal slug IDs and ordered metro line positions. Favorites persist those IDs and direction terminus IDs. A direct switch to GTFS stop IDs would invalidate existing favorites and the current direction logic.

The [`GolemioDepartureBoardParser`](../../../app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioDepartureBoardParser.kt) maps the documented v2 board JSON into `DepartureBoard`. It selects metro route type `1` and lines A/B/C. [`MetroStopIds`](../../../app/src/main/java/dev/pukan/metroprague/data/golemio/MetroStopIds.kt) maps the 20 current app station slugs to 42 GTFS platform IDs and a travel direction. The [HTTP service](../../../app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioBoardService.kt) requests only those IDs, avoiding the tram/bus departures that exact-name queries also returned. [`GolemioDepartureRepository`](../../../app/src/main/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepository.kt) refreshes while collected and emits an unavailable board on failure. The UI labels this separately from “No service.”

Live observations on 2026-09-28: an exact-name Anděl query returned 30 departures, of which only 6 were metro; Můstek returned both A and B; Háje returned line C trains with headsign **Chodov**, not the nominal line terminus Letňany. Other line C southbound trains showed **Pražského povstání** as a short-turn destination. This is why direction matching now uses the mapped platform and the UI shows the selected trip's actual headsign. Golemio timestamps in these responses used an ISO 8601 `+02:00` offset; a parser test covers that format. These service patterns can change with operations.

Recommended next implementation steps:

1. Expand the current representative 20-station list to the full metro A/B/C catalogue. Derive station order and platform membership from current GTFS data where practical; keep favorite slug migration in mind. The committed 42-ID mapping is an initial snapshot, not an automatic update mechanism.
2. Run an emulator/device flow with the local key. The SDK's `adb` is installed but showed no attached device on 2026-09-28, so UI/network behavior on Android remains unverified despite successful builds and HTTP checks from the development machine.
3. Improve request coordination: Home currently starts one flow per favorite station and each active flow polls every 30 seconds. Deduplicate or batch across screens, cache responses, honor HTTP 429 and `Retry-After` if supplied, and measure real request volume before broader use.
4. Distinguish transient errors from longer outages with last-success timestamps and a retry action. The initial implementation shows “Live departures unavailable” on any failed fetch; it does not retain stale departure data.
5. Decide how the released app obtains departures using the options below. Local debug embeds the key; do not mistake an ignored `local.properties` file or code obfuscation for protection once its value is compiled into an APK.

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

## Validation log and next checks

1. **Done:** authenticated requests for all 20 current station names identified metro platform IDs and current headsigns; ID-only requests for Anděl, Můstek, and Háje returned only metro route type `1`. No token or full response body was committed.
2. **Done:** deterministic JVM tests cover parser success, empty boards, non-metro filtering, offset timestamps, short-turn platform direction, HTTP 429 represented as unavailable, unknown stations, and encoded stop-ID URLs. `./gradlew assembleDebug testDebugUnitTest`, `./gradlew assembleRelease`, and `./gradlew assembleDebugAndroidTest` passed on 2026-09-28. The instrumentation APK compiled but could not run without an attached device. Debug generated configuration contains the local key; release generated configuration does not.
3. **Pending:** on an emulator, verify foreground refresh, returning from background, loss and restoration of connectivity, error text, and a station with no departures. Validate actual UI state under live line C short turns.
4. **Pending:** add deterministic tests for 401, timeout, malformed responses, and retry/backoff behavior when those policies are implemented. Do not deliberately flood production to provoke 429.
5. **Pending:** measure combined Search and Home request volume, then deduplicate or batch favorites and adjust refresh timing using the owner's actual portal limits.

## Open questions / validation log

- How should the full metro catalogue be loaded and refreshed beyond the 20 mapped prototype stations?
- Does the portal impose additional terms, key scopes, or a different limit on the owner's new key? Owner will check.
- Which proxy contract and hosting choice should replace the direct debug transport before public distribution?
- Device offline/error behavior and real request volume remain unverified; use the plan above.

## Sources

- [Golemio public transport API documentation](https://api.golemio.cz/pid/docs/openapi/)
- [Public transport OpenAPI JSON specification](https://api.golemio.cz/docs/static/vp-output-gateway/openapi.json)
- [Golemio registration and open data guide](https://operator-ict.gitlab.io/golemio/documentation/en/open-data-api/)
- [Golemio API key portal](https://api.golemio.cz/api-keys/)
- [API key portal terms](https://api.golemio.cz/api-keys/terms-and-conditions)
- [PID open data and GTFS Static documentation](https://pid.cz/en/opendata/)
- [Cloudflare Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/), [limits](https://developers.cloudflare.com/workers/platform/limits/), [secrets](https://developers.cloudflare.com/workers/configuration/secrets/), and [`workers.dev` routing](https://developers.cloudflare.com/workers/configuration/routing/workers-dev/)
- [Hostinger web hosting connectivity](https://www.hostinger.com/support/which-web-standards-and-connectivity-features-are-supported-at-hostinger/)
