import Foundation

/// Reads and atomically writes the metro snapshot file, `Application Support/metro-data.json`.
nonisolated struct SnapshotFileStore: Sendable {
    let url: URL

    static let defaultURL = URL.applicationSupportDirectory.appending(path: "metro-data.json")

    init(url: URL = Self.defaultURL) {
        self.url = url
    }

    /// The saved snapshot, or `nil` when the file is missing, unreadable, or of another version.
    func read() -> MetroSnapshot? {
        guard let data = try? Data(contentsOf: url),
            let snapshot = try? Self.decoder.decode(MetroSnapshot.self, from: data),
            snapshot.version == MetroSnapshot.currentVersion
        else { return nil }
        return snapshot
    }

    /// Replaces the file. The atomic write means a reader sees either the old or the new file,
    /// never a partial one. The file is excluded from backups because it can be downloaded again.
    func write(_ snapshot: MetroSnapshot) throws {
        try FileManager.default.createDirectory(
            at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try Self.encoder.encode(snapshot).write(to: url, options: .atomic)
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var fileURL = url
        try fileURL.setResourceValues(values)
    }

    private static var encoder: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .millisecondsSince1970
        return encoder
    }

    private static var decoder: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .millisecondsSince1970
        return decoder
    }
}
