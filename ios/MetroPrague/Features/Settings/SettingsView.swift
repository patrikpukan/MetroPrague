import SwiftUI

/// Appearance, saved metro data with its reload, and About.
struct SettingsView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(MetroDataStore.self) private var metroData

    var body: some View {
        @Bindable var settings = settings
        NavigationStack {
            Form {
                Section("Appearance") {
                    // The navigation-link style opens a list and returns to Settings on a choice.
                    Picker("Theme", selection: $settings.theme) {
                        ForEach(ThemePreference.allCases, id: \.self) { theme in
                            Text(theme.label).tag(theme)
                        }
                    }
                    .pickerStyle(.navigationLink)
                }
                MetroDataSection(status: metroData.status, reload: metroData.reload)
                Section("About") {
                    NavigationLink("Changelog") {
                        ChangelogView()
                    }
                    LabeledContent("App version") {
                        Text(AppVersion.main.label)
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }
}

/// The saved metro data, the reload button, and its progress or error.
private struct MetroDataSection: View {
    let status: MetroDataStatus
    let reload: () -> Void

    var body: some View {
        Section("Metro data") {
            VStack(alignment: .leading, spacing: 8) {
                Text(
                    "Stations and departures are saved on this device. Data updates only when you reload it. Departure snapshots expire after 5 minutes."
                )
                ForEach(status.savedDataLines.indices, id: \.self) { index in
                    Text(status.savedDataLines[index])
                }
            }
            if let message = status.progressMessage {
                if let fraction = status.progressFraction {
                    ProgressView(value: fraction) { Text(message) }
                } else {
                    ProgressView { Text(message) }
                }
            }
            if let message = status.errorMessage {
                // The red symbol marks the error; the text keeps full contrast.
                Label {
                    Text(message)
                } icon: {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(.red)
                }
            }
            if status.canReload {
                Button("Reload data", systemImage: "arrow.clockwise", action: reload)
                    .disabled(status.isReloading)
            } else {
                Text("Data reload is unavailable in this build.")
            }
        }
        .onChange(of: status.error) { _, _ in
            // VoiceOver users hear the result without moving focus.
            if let message = status.errorMessage {
                AccessibilityNotification.Announcement(String(localized: message)).post()
            }
        }
    }
}

extension ThemePreference {
    var label: LocalizedStringResource {
        switch self {
        case .system: "System default"
        case .light: "Light"
        case .dark: "Dark"
        }
    }
}

#Preview("Mock build") {
    SettingsView()
        .environment(AppSettings())
        .environment(MetroDataStore(source: MockMetroDataSource()))
}

#Preview("Reloading") {
    Form {
        MetroDataSection(
            status: MetroDataStatus(
                canReload: true, phase: .loadingBoards(completed: 20, total: 58)),
            reload: {})
    }
}

#Preview("Saved data and an error") {
    Form {
        MetroDataSection(
            status: MetroDataStatus(
                canReload: true, stationCount: 58, departureCount: 1_234, reloadedAt: .now,
                error: .rateLimited),
            reload: {})
    }
}
