import Foundation

/// Values injected at build time through `Config/*.xcconfig` and `Config/Info.plist`.
///
/// `nonisolated` so the data layer can read it off the main actor; it is an immutable value type.
nonisolated struct AppConfiguration: Sendable {
    /// The Golemio API key, or `nil` when the build has none (Release builds, or Debug without
    /// `Config/Secrets.xcconfig`). Never log or print this value.
    let golemioAPIKey: String?

    init(infoDictionary: [String: Any]) {
        let rawKey = (infoDictionary["GolemioAPIKey"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if let rawKey, !rawKey.isEmpty, !rawKey.hasPrefix("$(") {
            golemioAPIKey = rawKey
        } else {
            golemioAPIKey = nil
        }
    }

    static let main = AppConfiguration(infoDictionary: Bundle.main.infoDictionary ?? [:])
}
