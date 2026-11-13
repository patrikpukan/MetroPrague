/// Derives travel directions from the stations' line positions.
nonisolated enum StationDirections {
    /// The first and last stations of `line`, in line order. A line with one station has one
    /// terminus; a line with none has no termini.
    static func termini(of line: Line, in stations: [Station]) -> [Station] {
        let ordered =
            stations
            .compactMap { station in
                station.lines.first { $0.line == line }.map { (station, $0.order) }
            }
            .sorted { $0.1 < $1.1 }
            .map(\.0)
        guard let first = ordered.first, let last = ordered.last else { return [] }
        return ordered.count == 1 ? [first] : [first, last]
    }

    /// Every direction available at `station`: for each of its lines in A, B, C order, each
    /// terminus of that line except the station itself.
    static func directions(at station: Station, in stations: [Station]) -> [Direction] {
        station.lines
            .sorted { $0.line.sortIndex < $1.line.sortIndex }
            .flatMap { position in
                termini(of: position.line, in: stations).compactMap { terminus -> Direction? in
                    let terminusOrder = terminus.lines.first { $0.line == position.line }?.order
                    guard terminusOrder != position.order else { return nil }
                    return Direction(
                        line: position.line,
                        terminusStationID: terminus.id,
                        terminusName: terminus.name
                    )
                }
            }
    }
}
