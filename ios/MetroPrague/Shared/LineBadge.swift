import SwiftUI

/// The line letter in white on the line color. VoiceOver reads it as "Line A".
struct LineBadge: View {
    let line: Line

    var body: some View {
        Text(verbatim: line.rawValue)
            .font(.subheadline.bold())
            .foregroundStyle(.white)
            .frame(minWidth: 28, minHeight: 28)
            .background(line.color, in: .circle)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("Line \(line.rawValue)"))
    }
}

extension Line {
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
