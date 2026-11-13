import Foundation
import Observation

/// The user's app preferences.
@Observable
final class AppSettings {
    static let themeKey = "theme_preference"

    /// The appearance choice, saved under `theme_preference` as `system`, `light`, or `dark`.
    var theme: ThemePreference {
        didSet { defaults.set(theme.storedValue, forKey: Self.themeKey) }
    }

    @ObservationIgnored private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        theme = ThemePreference(storedValue: defaults.string(forKey: Self.themeKey))
    }
}
