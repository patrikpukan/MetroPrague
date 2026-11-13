import Synchronization

@testable import MetroPrague

/// A manual clock for `GolemioHTTPClient`. Sleeping records the duration and advances the clock
/// instead of waiting, so tests check pacing without real delays.
nonisolated final class TestClock: Sendable {
    private struct State {
        var now: ContinuousClock.Instant
        var sleeps: [Duration] = []
    }

    private let state = Mutex(State(now: .now))

    var now: ContinuousClock.Instant { state.withLock { $0.now } }
    var sleeps: [Duration] { state.withLock { $0.sleeps } }

    func advance(by duration: Duration) {
        state.withLock { $0.now += duration }
    }

    func sleep(_ duration: Duration) {
        state.withLock {
            $0.sleeps.append(duration)
            $0.now += duration
        }
    }
}

extension GolemioHTTPClient {
    /// A client on `clock` that never really sleeps.
    init(apiKey: String = "test-key", transport: any HTTPTransport, clock: TestClock) {
        self.init(
            apiKey: apiKey, transport: transport, now: { clock.now }, sleep: { clock.sleep($0) })
    }
}
