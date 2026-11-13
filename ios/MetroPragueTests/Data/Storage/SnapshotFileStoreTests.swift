import Foundation
import Testing

@testable import MetroPrague

@Suite("SnapshotFileStore")
struct SnapshotFileStoreTests {
    private let directory = FileManager.default.temporaryDirectory.appending(
        path: UUID().uuidString)
    private var store: SnapshotFileStore {
        SnapshotFileStore(url: directory.appending(path: "metro-data.json"))
    }

    private func snapshot() throws -> MetroSnapshot {
        MetroSnapshot(
            catalogue: try MetroTestData.catalogue(),
            boards: [
                "andel": SavedBoard(
                    response: MetroTestData.board, fetchedAt: Date(timeIntervalSince1970: 100.25))
            ],
            reloadedAt: Date(timeIntervalSince1970: 123),
            departureCount: 1)
    }

    @Test("Stations, platforms, boards, and times survive a write and read")
    func roundTrip() throws {
        let saved = try snapshot()
        try store.write(saved)
        #expect(store.read() == saved)
    }

    @Test("The file is excluded from backups")
    func excludedFromBackup() throws {
        try store.write(try snapshot())
        let values = try store.url.resourceValues(forKeys: [.isExcludedFromBackupKey])
        #expect(values.isExcludedFromBackup == true)
    }

    @Test("A missing, malformed, or other-version file is no snapshot")
    func unreadable() throws {
        #expect(store.read() == nil)

        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try Data("broken-json".utf8).write(to: store.url)
        #expect(store.read() == nil)

        var other = try snapshot()
        other.version = 2
        try store.write(other)
        #expect(store.read() == nil)
    }
}
