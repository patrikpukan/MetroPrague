import Foundation

/// A Golemio request failure.
nonisolated enum GolemioError: Error, Equatable {
    /// A non-2xx HTTP status, or 429 reported early while a Retry-After window is still open.
    case http(Int)
    /// A response that does not have the expected shape or content.
    case invalidData
    /// The request did not complete (no connection, timeout, and similar).
    case transport
}

/// Sends one HTTP request. `URLSession` conforms in the app; tests use fakes.
nonisolated protocol HTTPTransport: Sendable {
    func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse)
}

extension URLSession: HTTPTransport {
    nonisolated func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await data(for: request, delegate: nil)
        guard let response = response as? HTTPURLResponse else { throw GolemioError.transport }
        return (data, response)
    }
}

/// The one paced transport shared by catalogue and board requests.
///
/// Requests run one at a time. After each response, the next request starts no sooner than
/// 600 ms later. On HTTP 429, a numeric `Retry-After` (clamped to 1…3600 s, otherwise 8 s)
/// pushes the next start further out; a call that would have to wait longer than the normal
/// 600 ms throws `GolemioError.http(429)` at once instead of stalling. Never logs the key or
/// URLs.
actor GolemioHTTPClient {
    static let pacing: Duration = .milliseconds(600)
    static let defaultRetryAfter = 8
    static let retryAfterRange = 1...3600

    private let apiKey: String
    private let transport: any HTTPTransport
    private let now: @Sendable () -> ContinuousClock.Instant
    private let sleep: @Sendable (Duration) async throws -> Void

    private var nextRequestAt: ContinuousClock.Instant?
    private var lastRequest: Task<Void, Never>?

    init(
        apiKey: String,
        transport: any HTTPTransport = URLSession.shared,
        now: @escaping @Sendable () -> ContinuousClock.Instant = { .now },
        sleep: @escaping @Sendable (Duration) async throws -> Void = {
            try await Task.sleep(for: $0)
        }
    ) {
        self.apiKey = apiKey
        self.transport = transport
        self.now = now
        self.sleep = sleep
    }

    /// Fetches `url` and returns the body of a 2xx response.
    func get(_ url: URL) async throws -> Data {
        // Actors are reentrant: while one request awaits the network, another call can enter
        // the actor. Chaining each request behind the previous one keeps them strictly one at a
        // time, like a mutex.
        let previous = lastRequest
        let request = Task {
            await previous?.value
            return try await send(url)
        }
        lastRequest = Task { _ = try? await request.value }
        return try await withTaskCancellationHandler {
            try await request.value
        } onCancel: {
            request.cancel()
        }
    }

    private func send(_ url: URL) async throws -> Data {
        if let nextRequestAt {
            let wait = nextRequestAt - now()
            // Still inside a Retry-After window: report it now instead of silently stalling.
            if wait > Self.pacing { throw GolemioError.http(429) }
            if wait > .zero { try await sleep(wait) }
        }
        defer {
            let paced = now() + Self.pacing
            nextRequestAt = max(nextRequestAt ?? paced, paced)
        }

        var request = URLRequest(url: url, timeoutInterval: 10)
        request.setValue(apiKey, forHTTPHeaderField: "X-Access-Token")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let data: Data
        let response: HTTPURLResponse
        do {
            (data, response) = try await transport.data(for: request)
        } catch is CancellationError {
            throw CancellationError()
        } catch let error as GolemioError {
            throw error
        } catch {
            throw GolemioError.transport
        }

        guard (200..<300).contains(response.statusCode) else {
            if response.statusCode == 429 {
                let seconds =
                    response.value(forHTTPHeaderField: "Retry-After")
                    .flatMap { Int($0) }
                    .map {
                        min(
                            max($0, Self.retryAfterRange.lowerBound),
                            Self.retryAfterRange.upperBound)
                    }
                    ?? Self.defaultRetryAfter
                nextRequestAt = now() + .seconds(seconds)
            }
            throw GolemioError.http(response.statusCode)
        }
        return data
    }
}
