# Shared API fixtures

`fixtures/` contains platform-neutral API inputs that Android and iOS test suites can share.
Fixtures use the naming rule `<endpoint>/<scenario>.json`; for example,
`golemio/departureboards/metro-predicted-delay.json`.

Fixtures must never contain API tokens, credentials, or personal data. The table below is the
single place that records the expected parse results and assertions every platform's tests must
make for each fixture. No sibling scenario Markdown files are used. The Android parser tests still
use inline copies of these samples; switching those tests to read `fixtures/` is a follow-up.

All four inputs below are passed without a clock value (`now`). “Expected parse result” describes
the assertions in the named Android test, with parser behavior noted where relevant to the fixture's
context.

| Fixture | Input context | Expected parse result and shared assertions |
| --- | --- | --- |
| [`metro-predicted-delay.json`](golemio/departureboards/metro-predicted-delay.json) | Station `muzeum`; no explicit stop or catalogue. Test: `maps metro departures with predicted time and delay`. | Exactly one departure is returned. It is line A, with destination/headsign `Depo Hostivař`; scheduled time is `2026-09-26T10:00:00Z`, predicted time is `2026-09-26T10:01:30Z`, delay is 90 seconds, and it is not canceled. The board carries the info text `Výluka`. |
| [`mixed-transport-scheduled-only.json`](golemio/departureboards/mixed-transport-scheduled-only.json) | Station `andel`; no explicit stop or catalogue. Test: `ignores other transport types and supports scheduled only metro service`. | The parser filters out route type 0, leaving exactly one departure with trip id `metro`. The test asserts that its predicted time and delay are null and that it is at the stop. The retained input has scheduled time `2026-09-26T10:03:00Z`. |
| [`empty-board.json`](golemio/departureboards/empty-board.json) | Station `haje`; no departures, explicit stop, or catalogue. Test: `empty departure board is a valid response`. | The board is valid, preserves station id `haje`, and has no departures or info texts. |
| [`short-turn.json`](golemio/departureboards/short-turn.json) | Station `muzeum`; line C stop `U400Z122P` (the parser's stop mapping points toward `haje`); no catalogue. Test: `short turn is assigned by metro platform instead of headsign`. | The test asserts that direction C toward Háje (`haje`) returns exactly the trip `short-turn`, even though its headsign is `Pražského povstání`. The scheduled timestamp with offset resolves to `2026-09-28T10:00:00Z`. No prediction or delay assertion is made in this test. |
