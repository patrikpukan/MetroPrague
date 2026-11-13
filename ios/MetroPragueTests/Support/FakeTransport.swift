import Foundation
import Synchronization

@testable import MetroPrague

/// A fake HTTP transport that records every request and answers through `handler`.
nonisolated final class FakeTransport: HTTPTransport {
    struct Reply: Sendable {
        var status = 200
        var headers: [String: String] = [:]
        var body = Data()

        static func json(_ string: String) -> Reply { Reply(body: Data(string.utf8)) }
        static func status(_ status: Int, headers: [String: String] = [:]) -> Reply {
            Reply(status: status, headers: headers)
        }
    }

    typealias Handler = @Sendable (URLRequest) async throws -> Reply

    private let handler: Handler
    private let recorded = Mutex<[URLRequest]>([])

    init(handler: @escaping Handler = { _ in .json("{}") }) {
        self.handler = handler
    }

    var requests: [URLRequest] { recorded.withLock { $0 } }

    func requests(path: String) -> [URLRequest] {
        requests.filter { $0.url?.path() == path }
    }

    func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        recorded.withLock { $0.append(request) }
        let reply = try await handler(request)
        let response = HTTPURLResponse(
            url: request.url!, statusCode: reply.status, httpVersion: nil,
            headerFields: reply.headers)!
        return (reply.body, response)
    }
}
