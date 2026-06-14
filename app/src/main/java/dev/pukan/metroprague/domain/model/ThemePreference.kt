package dev.pukan.metroprague.domain.model

enum class ThemePreference(val storedValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStoredValue(value: String?): ThemePreference =
            entries.firstOrNull { it.storedValue == value } ?: System
    }
}
