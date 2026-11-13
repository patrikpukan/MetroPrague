import SwiftUI

/// The line letter on the line color. VoiceOver reads it as "Line A".
struct LineBadge: View {
    let line: Line

    /// Grows with the letter's text style, so the letter always fits the circle.
    @ScaledMetric(relativeTo: .subheadline) private var size = 28.0

    var body: some View {
        Text(verbatim: line.rawValue)
            .font(.subheadline.bold())
            .foregroundStyle(line.letterColor)
            .frame(width: size, height: size)
            .background(line.color, in: .circle)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("Line \(line.rawValue)"))
    }
}

extension Line {
    /// The letter color on the line color, chosen to reach 4.5:1 at every text size (the letter
    /// drops below the 14 pt bold large-text size at the smallest Dynamic Type settings).
    /// Black on A is 6.6:1 (white only 3.2:1), black on B 10.5:1 (white 2.0:1), and white on C
    /// 5.0:1.
    var letterColor: Color {
        self == .c ? .white : .black
    }

    /// The line's brand color.
    var color: Color {
        Color(
            red: Double((colorHex >> 16) & 0xFF) / 255,
            green: Double((colorHex >> 8) & 0xFF) / 255,
            blue: Double(colorHex & 0xFF) / 255
        )
    }
}

#Preview {
    HStack {
        ForEach(Line.allCases, id: \.self) { line in
            LineBadge(line: line)
        }
    }
}
