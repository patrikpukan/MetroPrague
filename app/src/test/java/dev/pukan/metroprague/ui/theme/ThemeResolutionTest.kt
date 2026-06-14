package dev.pukan.metroprague.ui.theme

import dev.pukan.metroprague.domain.model.ThemePreference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolutionTest {

    @Test
    fun `System resolves to true when system is dark`() {
        assertTrue(resolveTheme(ThemePreference.System, isSystemDark = true))
    }

    @Test
    fun `System resolves to false when system is light`() {
        assertFalse(resolveTheme(ThemePreference.System, isSystemDark = false))
    }

    @Test
    fun `Light resolves to false when system is dark`() {
        assertFalse(resolveTheme(ThemePreference.Light, isSystemDark = true))
    }

    @Test
    fun `Light resolves to false when system is light`() {
        assertFalse(resolveTheme(ThemePreference.Light, isSystemDark = false))
    }

    @Test
    fun `Dark resolves to true when system is light`() {
        assertTrue(resolveTheme(ThemePreference.Dark, isSystemDark = false))
    }

    @Test
    fun `Dark resolves to true when system is dark`() {
        assertTrue(resolveTheme(ThemePreference.Dark, isSystemDark = true))
    }
}
