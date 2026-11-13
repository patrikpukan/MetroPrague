import SwiftUI

struct SettingsView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "Settings",
                systemImage: "gear",
                description: Text("Coming soon.")
            )
            .navigationTitle("Settings")
        }
    }
}

#Preview {
    SettingsView()
}
