import Foundation

/// The app's version and build number, shown in Settings as "1.0 (1)".
nonisolated struct AppVersion: Sendable {
    let version: String
    let build: String

    init(infoDictionary: [String: Any]) {
        version = infoDictionary["CFBundleShortVersionString"] as? String ?? "?"
        build = infoDictionary["CFBundleVersion"] as? String ?? "?"
    }

    static let main = AppVersion(infoDictionary: Bundle.main.infoDictionary ?? [:])

    var label: LocalizedStringResource { "\(version) (\(build))" }
}
