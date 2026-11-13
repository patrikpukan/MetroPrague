import SwiftUI

/// What a direction row shows: a line toward a destination and its next departure.
nonisolated struct DirectionRowContent: Identifiable, Equatable, Sendable {
    let direction: Direction
    /// The next departure's headsign, otherwise the terminus name.
    let destination: String
    let display: DepartureDisplay
    let isFavorite: Bool

    var id: Direction { direction }
}

/// A line badge, an optional station name, "To <destination>", and the next-departure label,
/// with a trailing accessory such as a favorite button. VoiceOver reads the text as one element
/// and the accessory separately.
struct DirectionRow<Accessory: View>: View {
    let line: Line
    var stationName: String? = nil
    let destination: String
    let display: DepartureDisplay
    /// Called when the row is chosen; `nil` makes the row informational.
    var action: (() -> Void)? = nil
    @ViewBuilder var accessory: Accessory

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        HStack(spacing: 12) {
            if let action {
                Button(action: action) { label }
                    .buttonStyle(.plain)
            } else {
                label
            }
            accessory
                .buttonStyle(.borderless)
        }
    }

    private var label: some View {
        // At accessibility sizes the badge moves above the text, leaving the text the full width.
        let layout =
            dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8))
            : AnyLayout(HStackLayout(spacing: 12))
        return layout {
            LineBadge(line: line)
            VStack(alignment: .leading, spacing: 2) {
                if let stationName {
                    Text(stationName)
                        .font(.headline)
                }
                Text("To \(destination)")
                Text(display.label)
                    .font(.subheadline.weight(.medium))
                    .monospacedDigit()
            }
            Spacer(minLength: 0)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

/// A star that adds or removes a favorite, labeled for VoiceOver by what it will do.
struct FavoriteButton: View {
    let isFavorite: Bool
    let toggle: () -> Void

    var body: some View {
        Button(action: toggle) {
            Label(
                isFavorite ? "Remove from favorites" : "Add to favorites",
                systemImage: isFavorite ? "star.fill" : "star"
            )
            .labelStyle(.iconOnly)
            .foregroundStyle(isFavorite ? Color.accentColor : Color.secondary)
            .imageScale(.large)
            .frame(minWidth: 44, minHeight: 44)
        }
        .accessibilityAddTraits(isFavorite ? .isSelected : [])
    }
}

#Preview {
    List {
        DirectionRow(
            line: .a, destination: "Depo Hostivař", display: .inMinutes(3), action: {}
        ) {
            FavoriteButton(isFavorite: true) {}
        }
        DirectionRow(line: .c, stationName: "Muzeum", destination: "Háje", display: .stale) {
            FavoriteButton(isFavorite: false) {}
        }
    }
}
