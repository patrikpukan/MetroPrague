import SwiftUI

extension DepartureDisplay {
    /// The user-visible text for this display.
    var label: LocalizedStringResource {
        switch self {
        case .noService: "No service"
        case .unavailable: "Live departures unavailable"
        case .stale: "Reload departures in Settings"
        case .cancelled: "Cancelled"
        case .atStation: "At station"
        case .now: "Now"
        case .inMinutes(let minutes): "\(minutes) min"
        case .atTime(let hour, let minute):
            // A fixed 24-hour HH:mm, zero padded, matching Android.
            "\(Self.twoDigits(hour)):\(Self.twoDigits(minute))"
        }
    }

    private static func twoDigits(_ value: Int) -> String {
        value < 10 ? "0\(value)" : "\(value)"
    }
}

#Preview {
    let displays: [DepartureDisplay] = [
        .noService, .unavailable, .stale, .cancelled, .atStation, .now, .inMinutes(4),
        .atTime(hour: 7, minute: 5),
    ]
    List(displays, id: \.self) { display in
        Text(display.label)
    }
}
