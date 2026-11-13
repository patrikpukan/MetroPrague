/// Identifies a favorite station direction. Display names are resolved from the current stations.
///
/// The coding keys match the version-1 favorites JSON shared with Android.
nonisolated struct FavoriteKey: Codable, Hashable, Sendable {
    let stationID: String
    let line: Line
    let terminusStationID: String

    private enum CodingKeys: String, CodingKey {
        case stationID = "stationId"
        case line
        case terminusStationID = "terminusStationId"
    }
}
