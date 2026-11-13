/// A Prague metro line.
nonisolated enum Line: String, CaseIterable, Codable, CodingKeyRepresentable, Hashable, Sendable {
    case a = "A"
    case b = "B"
    case c = "C"

    /// The line's brand color as `0xRRGGBB`.
    var colorHex: UInt32 {
        switch self {
        case .a: 0x00A562
        case .b: 0xF6A800
        case .c: 0xE00034
        }
    }

    /// The position of the line in A, B, C order, used to sort lines.
    var sortIndex: Int {
        Self.allCases.firstIndex(of: self) ?? 0
    }
}
