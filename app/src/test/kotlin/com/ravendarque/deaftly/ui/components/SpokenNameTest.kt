package com.ravendarque.deaftly.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SpokenNameTest {
    @Test
    fun theNameIsRespelledForSpeechKeepingItsCase() {
        assertEquals("Deftly", spokenText("Deaftly"))
        assertEquals("deftly", spokenText("deaftly"))
        assertEquals("Welcome to Deftly", spokenText("Welcome to Deaftly"))
    }

    @Test
    fun everyOccurrenceInASentenceIsRespelled() {
        assertEquals("Deftly is not Deftly's rival", spokenText("Deaftly is not Deaftly's rival"))
    }

    @Test
    fun textWithoutTheNameIsUntouched() {
        assertEquals("Tap lines, or drag across words.", spokenText("Tap lines, or drag across words."))
        assertEquals("deft", spokenText("deft"))
    }
}
