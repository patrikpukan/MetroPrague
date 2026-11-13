/// The app's appearance choice. Persisted by its `storedValue`.
nonisolated enum ThemePreference: String, CaseIterable, Sendable {
    case system
    case light
    case dark

    var storedValue: String { rawValue }

    /// Resolves a stored value; a missing or unknown value falls back to `.system`.
    init(storedValue: String?) {
        self = storedValue.flatMap(Self.init(rawValue:)) ?? .system
    }
}
