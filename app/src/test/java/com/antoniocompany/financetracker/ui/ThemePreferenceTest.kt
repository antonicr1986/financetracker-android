package com.antoniocompany.financetracker.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemePreferenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs().edit().clear().commit()
    }

    @After
    fun tearDown() = ThemePreference.set(context, ThemePreference.Mode.SYSTEM)

    private fun prefs() = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    @Test
    fun `defaults to following the system`() {
        assertEquals(ThemePreference.Mode.SYSTEM, ThemePreference.current(context))
    }

    @Test
    fun `dark is saved and applied`() {
        ThemePreference.set(context, ThemePreference.Mode.DARK)

        assertEquals(ThemePreference.Mode.DARK, ThemePreference.current(context))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `light is saved and applied`() {
        ThemePreference.set(context, ThemePreference.Mode.LIGHT)

        assertEquals(ThemePreference.Mode.LIGHT, ThemePreference.current(context))
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `system removes the key instead of saving it`() {
        ThemePreference.set(context, ThemePreference.Mode.DARK)
        ThemePreference.set(context, ThemePreference.Mode.SYSTEM)

        assertFalse(prefs().contains("theme"))
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.getDefaultNightMode()
        )
    }

    @Test
    fun `unknown stored value falls back to system`() {
        prefs().edit().putString("theme", "sepia").commit()

        assertEquals(ThemePreference.Mode.SYSTEM, ThemePreference.current(context))
    }

    @Test
    fun `theme survives logout`() {
        ThemePreference.set(context, ThemePreference.Mode.DARK)

        com.antoniocompany.financetracker.data.SessionStore(context).clear()

        assertEquals(ThemePreference.Mode.DARK, ThemePreference.current(context))
    }
}
