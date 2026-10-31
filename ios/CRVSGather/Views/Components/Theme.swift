import SwiftUI

// MARK: - Color Palette
extension Color {
    static let primaryIndigo = Color(red: 0.39, green: 0.40, blue: 0.95) // #6366F1
    static let primaryIndigoDark = Color(red: 0.31, green: 0.27, blue: 0.89) // #4F46E5
    static let accentEmerald = Color(red: 0.06, green: 0.73, blue: 0.51) // #10B981
    static let accentAmber = Color(red: 0.96, green: 0.62, blue: 0.04) // #F59E0B
    static let accentDanger = Color(red: 0.94, green: 0.27, blue: 0.27) // #EF4444
    static let accentPurple = Color(red: 0.66, green: 0.33, blue: 0.98) // #A855F7
    static let accentCyan = Color(red: 0.02, green: 0.71, blue: 0.83) // #06B6D4

    // Dynamic Background Colors
    static let bgPrimary = Color("BgPrimary", bundle: nil)
    static let bgSecondary = Color("BgSecondary", bundle: nil)
    static let bgCard = Color("BgCard", bundle: nil)
    static let borderSubtle = Color("BorderSubtle", bundle: nil)

    // Fallbacks when asset catalog is not loaded
    static func dynamicBg(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(red: 0.06, green: 0.09, blue: 0.16) : Color(red: 0.97, green: 0.98, blue: 0.99)
    }

    static func dynamicCard(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(red: 0.12, green: 0.16, blue: 0.23) : Color.white
    }

    static func dynamicSecondary(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(red: 0.09, green: 0.13, blue: 0.20) : Color(red: 0.93, green: 0.95, blue: 0.98)
    }

    static func dynamicBorder(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color.white.opacity(0.12) : Color.black.opacity(0.08)
    }

    static func dynamicTextPrimary(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(red: 0.97, green: 0.98, blue: 0.99) : Color(red: 0.06, green: 0.09, blue: 0.16)
    }

    static func dynamicTextMuted(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(red: 0.58, green: 0.64, blue: 0.72) : Color(red: 0.39, green: 0.45, blue: 0.55)
    }
}

// MARK: - View Modifiers
struct CardBackgroundModifier: ViewModifier {
    @Environment(\.colorScheme) var colorScheme
    var cornerRadius: CGFloat = 16

    func body(content: Content) -> some View {
        content
            .background(Color.dynamicCard(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(Color.dynamicBorder(for: colorScheme), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.25 : 0.05), radius: 8, x: 0, y: 3)
    }
}

extension View {
    func cardStyle(cornerRadius: CGFloat = 16) -> some View {
        modifier(CardBackgroundModifier(cornerRadius: cornerRadius))
    }
}
