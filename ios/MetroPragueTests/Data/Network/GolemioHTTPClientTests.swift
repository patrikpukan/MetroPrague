import Foundation
import Synchronization
import Testing

@testable import MetroPrague

@Suite("GolemioHTTPClient")
struct GolemioHTTPClientTests {
    private let url = URL(string: "https://api.golemio.cz/v2/test")!
    private let clock = TestClock()

    @Test("Requests send the key, JSON accept header, and a 10-second timeout")
    func headers() async throws {
        let transport = FakeTransport()
        let client = GolemioHTTPClient(apiKey: "secret", transport: transport, clock: clock)
        _ = try await client.get(url)

        let request = try #require(transport.requests.first)
        #expect(request.value(forHTTPHeaderField: "X-Access-Token") == "secret")
        #expect(request.value(forHTTPHeaderField: "Accept") == "application/json")
        #expect(request.timeoutInterval == 10)
    }

    @Test("The next request starts no sooner than 600 ms after a response")
    func spacing() async throws {
        let client = GolemioHTTPClient(transport: FakeTransport(), clock: clock)
        _ = try await client.get(url)
        #expect(clock.sleeps.isEmpty)

        clock.advance(by: .milliseconds(200))
        _ = try await client.get(url)
        #expect(clock.sleeps == [.milliseconds(400)])

        clock.advance(by: .seconds(1))
        _ = try await client.get(url)
        #expect(clock.sleeps == [.milliseconds(400)])
    }

    @Test("Concurrent calls are serialized")
    func serialization() async throws {
        let events = Mutex<[String]>([])
        let transport = FakeTransport { request in
            let id = request.url!.lastPathComponent
            events.withLock { $0.append("start \(id)") }
            for _ in 0..<20 { await Task.yield() }
            events.withLock { $0.append("end \(id)") }
            return .json("{}")
        }
        let client = GolemioHTTPClient(transport: transport, clock: clock)
        async let first = client.get(url.appending(path: "1"))
        async let second = client.get(url.appending(path: "2"))
        _ = try await (first, second)

        let recorded = events.withLock { $0 }
        #expect(recorded.count == 4)
        #expect(recorded[0].hasPrefix("start"))
        #expect(recorded[1] == recorded[0].replacing("start", with: "end"))
        #expect(recorded[2].hasPrefix("start"))
        #expect(clock.sleeps == [.milliseconds(600)])
    }

    @Test(
        "HTTP 429 waits for Retry-After, clamped to 1…3600 s, defaulting to 8 s",
        arguments: [
            ("5", 5), ("0", 1), ("99999", 3600), ("soon", 8), (nil, 8),
        ] as [(String?, Int)])
    func retryAfter(header: String?, seconds: Int) async throws {
        let replies = Mutex([
            FakeTransport.Reply.status(429, headers: header.map { ["Retry-After": $0] } ?? [:])
        ])
        let transport = FakeTransport { _ in
            replies.withLock { $0.isEmpty ? .json("{}") : $0.removeFirst() }
        }
        let client = GolemioHTTPClient(transport: transport, clock: clock)
        await #expect(throws: GolemioError.http(429)) { try await client.get(url) }

        // Still more than 600 ms of the window left: fail at once without a request.
        clock.advance(by: .seconds(seconds) - .milliseconds(700))
        await #expect(throws: GolemioError.http(429)) { try await client.get(url) }
        #expect(transport.requests.count == 1)

        // 500 ms left: wait it out, then send.
        clock.advance(by: .milliseconds(200))
        _ = try await client.get(url)
        #expect(clock.sleeps == [.milliseconds(500)])
        #expect(transport.requests.count == 2)
    }

    @Test("Other statuses and transport failures map to their errors")
    func errors() async {
        let failing = GolemioHTTPClient(
            transport: FakeTransport { _ in .status(500) }, clock: clock)
        await #expect(throws: GolemioError.http(500)) { try await failing.get(url) }

        let offline = GolemioHTTPClient(
            transport: FakeTransport { _ in throw URLError(.notConnectedToInternet) }, clock: clock)
        await #expect(throws: GolemioError.transport) { try await offline.get(url) }
    }
}
