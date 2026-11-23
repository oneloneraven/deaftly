package com.ravendarque.deaftly.ui.show

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class VolumeKeysTest {
    private val keys = VolumeKeys()
    private val steps = mutableListOf<Boolean>()
    private val handler: (Boolean) -> Unit = { steps += it }

    private fun press(code: Int) = keys.handle(KeyEvent(KeyEvent.ACTION_DOWN, code)) and keys.handle(KeyEvent(KeyEvent.ACTION_UP, code))

    @Test
    fun withNothingClaimingThemTheVolumeKeysAreLeftToTheSystem() {
        assertFalse(press(KeyEvent.KEYCODE_VOLUME_UP))
        assertFalse(press(KeyEvent.KEYCODE_VOLUME_DOWN))
    }

    @Test
    fun whenClaimedEachPressStepsOnceAndTheSystemNeverSeesIt() {
        keys.claim(handler)

        assertTrue(press(KeyEvent.KEYCODE_VOLUME_UP))
        assertTrue(press(KeyEvent.KEYCODE_VOLUME_DOWN))

        assertEquals(listOf(true, false), steps)
    }

    @Test
    fun holdingAKeyStepsOnlyOnce() {
        keys.claim(handler)

        assertTrue(keys.handle(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0)))
        assertTrue(keys.handle(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 1)))

        assertEquals(listOf(true), steps)
    }

    @Test
    fun otherKeysAreNotTouched() {
        keys.claim(handler)

        assertFalse(press(KeyEvent.KEYCODE_BACK))
        assertEquals(emptyList(), steps)
    }

    @Test
    fun releasingHandsTheKeysBack() {
        keys.claim(handler)
        keys.release(handler)

        assertFalse(press(KeyEvent.KEYCODE_VOLUME_UP))
    }
}
