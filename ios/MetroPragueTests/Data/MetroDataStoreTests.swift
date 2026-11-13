import Foundation
import Synchronization
import Testing

@testable import MetroPrague

@Suite("MetroDataStore")
struct MetroDataStoreTests {
    private let fileStore = SnapshotFileStore(
        url: FileManager.default.temporaryDirectory.appending(
            path: "\(UUID().uuidString)/metro-data.json"))
    private let now = Date(timeIntervalSince1970: 1_791_288_000)

    private func store(_ transport: FakeTransport) -> MetroDataStore {
        let client = GolemioHTTPClient(transport: transport, clock: TestClock())
        let now = self.now
        return MetroDataStore(
            source: GolemioMetroDataSource(client: client, fileStore: fileStore, now: { now }))
    }

    @Test("Nothing is requested at launch")
    func noRequestAtLaunch() async {
        let transport = MetroTestData.transport()
        let store = store(transport)
        await store.loadSavedData()

        #expect(transport.requests.isEmpty)
        #expect(store.status.canReload)
        #expect(store.status.stationCount == 0)
        #expect(store.stations.count == 58)
    }

    @Test("Before the first reload, boards are unavailable and stale")
    func boardWithoutData() {
        let board = store(MetroTestData.transport()).board(for: "andel", at: now)
        #expect(board.isUnavailable)
        #expect(board.isStale)
    }

    @Test("A reload saves every station's board in one snapshot")
    func successfulReload() async throws {
        let transport = MetroTestData.transport { request in
            let isAndel = request.url!.query()?.contains("B-9-down") == true
            return .json(isAndel ? MetroTestData.board : MetroTestData.emptyBoard)
        }
        let store = store(transport)
        store.reload()
        #expect(store.status.phase != .idle)
        await store.waitForReload()

        #expect(transport.requests(path: "/v2/pid/departureboards").count == 58)
        let status = store.status
        #expect(status.phase == .idle)
        #expect(status.error == nil)
        #expect(status.stationCount == 58)
        #expect(status.departureCount == 1)
        #expect(status.reloadedAt == now)
        let saved = try #require(fileStore.read())
        #expect(saved.boards.count == 58)

        let andel = store.board(for: "andel", at: now)
        #expect(!andel.isStale)
        #expect(andel.departures.map(\.directionTerminusStationID) == ["zlicin"])
        #expect(andel.infoTexts == ["Service information"])
        #expect(store.board(for: "unknown", at: now).isUnavailable)
    }

    @Test(
        "A failed reload keeps the saved file and maps the error",
        arguments: [
            (FakeTransport.Reply.status(401), MetroReloadError.authentication),
            (.status(403), .authentication),
            (.status(429), .rateLimited),
            (.status(500), .networkOrData),
            (.json("not json"), .networkOrData),
        ])
    func failedReload(reply: FakeTransport.Reply, error: MetroReloadError) async throws {
        try await store(MetroTestData.transport()).reloadAndWait()
        let savedBytes = try Data(contentsOf: fileStore.url)

        let calls = Mutex(0)
        let failing = MetroTestData.transport { _ in
            calls.withLock {
                $0 += 1
                return $0
            } == 3 ? reply : .json(MetroTestData.emptyBoard)
        }
        let store = store(failing)
        await store.loadSavedData()
        try await store.reloadAndWait()

        #expect(store.status.error == error)
        #expect(store.status.phase == .idle)
        #expect(store.status.stationCount == 58)
        #expect(try Data(contentsOf: fileStore.url) == savedBytes)
    }

    @Test("A new reload clears the previous error")
    func errorCleared() async throws {
        let fail = Mutex(true)
        let store = store(
            MetroTestData.transport { _ in
                fail.withLock { $0 } ? .status(500) : .json(MetroTestData.emptyBoard)
            })
        try await store.reloadAndWait()
        #expect(store.status.error == .networkOrData)

        fail.withLock { $0 = false }
        store.reload()
        #expect(store.status.error == nil)
        await store.waitForReload()
        #expect(store.status.error == nil)
    }

    @Test("A second reload while reloading is ignored")
    func duplicateReload() async {
        let transport = MetroTestData.transport()
        let store = store(transport)
        store.reload()
        store.reload()
        await store.waitForReload()

        #expect(transport.requests(path: "/v2/gtfs/routes").count == 1)
        #expect(transport.requests(path: "/v2/pid/departureboards").count == 58)
    }

    @Test("Saved data is read at launch without requests")
    func loadsSavedData() async throws {
        try await store(MetroTestData.transport()).reloadAndWait()

        let transport = MetroTestData.transport()
        let relaunched = store(transport)
        await relaunched.loadSavedData()
        #expect(transport.requests.isEmpty)
        #expect(relaunched.status.stationCount == 58)
        #expect(relaunched.status.reloadedAt == now)
        #expect(!relaunched.board(for: "andel", at: now).isUnavailable)
    }

    @Test("The mock source cannot reload and generates boards")
    func mockSource() async {
        let store = MetroDataStore(source: MockMetroDataSource())
        store.reload()
        await store.waitForReload()
        #expect(!store.status.canReload)
        #expect(store.status.phase == .idle)
        #expect(store.stations.count == 58)
        #expect(store.board(for: "unknown", at: now).departures.isEmpty)
    }
}

extension MetroDataStore {
    fileprivate func reloadAndWait() async throws {
        reload()
        await waitForReload()
    }
}
