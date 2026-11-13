import Foundation

/// Loads shared test inputs from the repository's `fixtures/` directory.
///
/// Fixtures are read in place rather than bundled into the test target, so Android and iOS always
/// test against the same files. This works because the iOS Simulator can read host file paths.
enum Fixtures {
    struct NotFound: Error, CustomStringConvertible {
        let description: String
    }

    /// Returns the URL of `relativePath` inside `fixtures/`, for example
    /// `"golemio/departureboards/empty-board.json"`.
    static func url(_ relativePath: String, filePath: StaticString = #filePath) throws -> URL {
        let fixturesDirectory = try repositoryRoot(from: filePath).appending(path: "fixtures")
        let url = fixturesDirectory.appending(path: relativePath)
        guard FileManager.default.fileExists(atPath: url.path(percentEncoded: false)) else {
            throw NotFound(description: "No fixture at \(url.path(percentEncoded: false))")
        }
        return url
    }

    static func data(_ relativePath: String, filePath: StaticString = #filePath) throws -> Data {
        try Data(contentsOf: url(relativePath, filePath: filePath))
    }

    /// Walks up from `filePath` to the first directory containing `fixtures/README.md`.
    private static func repositoryRoot(from filePath: StaticString) throws -> URL {
        let start = URL(filePath: "\(filePath)")
        var directory = start.deletingLastPathComponent()
        while directory.path(percentEncoded: false) != "/" {
            let marker = directory.appending(path: "fixtures/README.md")
            if FileManager.default.fileExists(atPath: marker.path(percentEncoded: false)) {
                return directory
            }
            directory = directory.deletingLastPathComponent()
        }
        throw NotFound(
            description: "No repository root with fixtures/README.md above \(start.path)")
    }
}
