package com.ravendarque.deaftly.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeModeTest {
    @Test
    fun systemFollowsThePhone() {
        assertTrue(ThemeMode.System.isDark(systemIsDark = true))
        assertFalse(ThemeMode.System.isDark(systemIsDark = false))
    }

    @Test
    fun lightAndDarkIgnoreThePhone() {
        assertFalse(ThemeMode.Light.isDark(systemIsDark = true))
        assertTrue(ThemeMode.Dark.isDark(systemIsDark = false))
    }

    @Test
    fun offersSystemLightAndDarkInThatOrder() {
        assertEquals(listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark), ThemeMode.entries)
    }
}
