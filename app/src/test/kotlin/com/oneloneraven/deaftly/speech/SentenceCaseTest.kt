package com.oneloneraven.deaftly.speech

import kotlin.test.Test
import kotlin.test.assertEquals

class SentenceCaseTest {
    @Test
    fun capitalisesTheFirstLetterOnly() {
        assertEquals("See you on tuesday", toSentenceCase(" SEE YOU ON TUESDAY "))
    }

    @Test
    fun capitalisesIAndItsContractions() {
        assertEquals("I think I'm late and I'll call", toSentenceCase("I THINK I'M LATE AND I'LL CALL"))
    }

    @Test
    fun leavesWordsStartingWithILowerCase() {
        assertEquals("It is in", toSentenceCase("IT IS IN"))
    }

    @Test
    fun keepsBlankTextBlank() {
        assertEquals("", toSentenceCase("   "))
    }
}
