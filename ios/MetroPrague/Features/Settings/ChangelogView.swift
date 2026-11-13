import SwiftUI

/// The release notes of one version.
struct ChangelogEntry: Identifiable {
    let version: String
    let notes: [LocalizedStringResource]

    var id: String { version }

    static let all = [
        ChangelogEntry(
            version: "1.0",
            notes: [
                "Initial release.",
                "Metro Prague line and station search.",
                "Theme preferences.",
            ]
        )
    ]
}

/// The embedded release notes, newest version first.
struct ChangelogView: View {
    var entries = ChangelogEntry.all

    var body: some View {
        List(entries) { entry in
            Section("Version \(entry.version)") {
                ForEach(entry.notes.indices, id: \.self) { index in
                    Text(entry.notes[index])
                }
            }
        }
        .navigationTitle("Changelog")
    }
}

#Preview {
    NavigationStack {
        ChangelogView()
    }
}
