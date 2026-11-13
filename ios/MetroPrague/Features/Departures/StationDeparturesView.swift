import SwiftUI

/// The next departures from one station toward one terminus. Time labels refresh every 15
/// seconds from saved data; nothing is requested.
struct StationDeparturesView: View {
    @State private var model: StationDeparturesModel

    init(route: DepartureRoute, metroData: MetroDataStore) {
        _model = State(initialValue: StationDeparturesModel(route: route, metroData: metroData))
    }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 15)) { context in
            let content = model.content(at: context.date)
            List {
                Section {
                    ForEach(content.rows) { row in
                        DepartureRowView(destination: row.destination, display: row.display)
                    }
                } header: {
                    if !content.terminusName.isEmpty {
                        HStack(spacing: 8) {
                            LineBadge(line: model.route.line)
                            Text("To \(content.terminusName)")
                                .font(.headline)
                                .foregroundStyle(.primary)
                        }
                        .accessibilityElement(children: .combine)
                        .accessibilityAddTraits(.isHeader)
                    }
                }
            }
            .overlay {
                if let emptyState = content.emptyState {
                    DeparturesUnavailableView(state: emptyState)
                }
            }
        }
        .navigationTitle(model.stationName)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// One departure: its destination and time label, read together by VoiceOver.
private struct DepartureRowView: View {
    let destination: String
    let display: DepartureDisplay

    var body: some View {
        HStack {
            Text(destination)
            Spacer()
            Text(display.label)
                .fontWeight(.medium)
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
    }
}

/// The stale, unavailable, and no-service states, each with its own copy and symbol.
private struct DeparturesUnavailableView: View {
    let state: DepartureDisplay

    var body: some View {
        ContentUnavailableView {
            Label {
                Text(state.label)
            } icon: {
                Image(systemName: systemImage)
            }
        } description: {
            if state == .stale {
                Text("Departure snapshots expire after 5 minutes.")
            }
        }
    }

    private var systemImage: String {
        switch state {
        case .stale: "clock.arrow.circlepath"
        case .unavailable: "exclamationmark.icloud"
        default: "tram"
        }
    }
}

#if DEBUG
    private func previewView(_ board: @escaping @Sendable (String, Date) -> DepartureBoard)
        -> some View
    {
        NavigationStack {
            StationDeparturesView(
                route: DepartureRoute(stationID: "andel", line: .b, terminusStationID: "zlicin"),
                metroData: MetroDataStore(source: FixedMetroDataSource(boards: board)))
        }
    }

    #Preview("Departures") {
        previewView { stationID, now in
            DepartureBoard(
                stationID: stationID,
                departures: [10, 70, 200, 600, 4000, 5000].enumerated().map { index, seconds in
                    Departure(
                        tripID: "\(index)", line: .b,
                        headsign: index == 1 ? "Nové Butovice" : "Zličín",
                        scheduled: now.addingTimeInterval(seconds), predicted: nil,
                        delaySeconds: nil, isAtStop: index == 0, isCanceled: index == 3)
                },
                fetchedAt: now)
        }
    }

    #Preview("Stale") {
        previewView { stationID, now in
            DepartureBoard(
                stationID: stationID, departures: [], fetchedAt: now.addingTimeInterval(-600))
        }
    }

    #Preview("Unavailable") {
        previewView { stationID, _ in
            DepartureBoard(stationID: stationID, departures: [], isUnavailable: true)
        }
    }

    #Preview("No service") {
        previewView { stationID, now in
            DepartureBoard(stationID: stationID, departures: [], fetchedAt: now)
        }
    }
#endif
