/// The position of a station on one line, counted from the line's first terminus.
nonisolated struct LinePosition: Hashable, Sendable {
    let line: Line
    let order: Int
}

/// A metro station and the lines that serve it.
nonisolated struct Station: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    /// The lines serving the station, in A, B, C order.
    let lines: [LinePosition]
}
