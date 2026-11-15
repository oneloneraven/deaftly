package com.ravendarque.deaftly.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonalNamesTest {
    @Test
    fun theFormalNameIsTheFirstNameAndSurnameTogether() {
        assertEquals("Amira Hassan", PersonalNames(firstName = "Amira", surname = "Hassan").formalName)
    }

    @Test
    fun blankPartsAreIgnoredAndTheRestIsTrimmed() {
        assertEquals("Amira", PersonalNames(firstName = "  Amira ", surname = "   ").formalName)
        assertEquals("Hassan", PersonalNames(firstName = "", surname = " Hassan ").formalName)
    }

    @Test
    fun noNamesMeansNoFormalName() {
        assertNull(PersonalNames().formalName)
        assertNull(PersonalNames(firstName = " ", surname = " ", legalNameDiffers = true, legalName = " ").formalName)
    }

    @Test
    fun aDifferentLegalNameReplacesTheFirstNameAndSurname() {
        val names = PersonalNames("Tunde", "Bello", legalNameDiffers = true, legalName = " Olatunde Bello ")

        assertEquals("Olatunde Bello", names.formalName)
    }

    @Test
    fun theLegalNameIsIgnoredUnlessTheFlagIsSet() {
        assertEquals("Tunde Bello", PersonalNames("Tunde", "Bello", legalName = "Olatunde Bello").formalName)
    }

    @Test
    fun aBlankLegalNameFallsBackToTheFirstNameAndSurname() {
        assertEquals("Tunde Bello", PersonalNames("Tunde", "Bello", legalNameDiffers = true, legalName = " ").formalName)
    }

    @Test
    fun theChatNameIsTheTrimmedFirstNameOnlyWhenChosen() {
        assertEquals("Amira", PersonalNames(firstName = " Amira ", useFirstNameInChats = true).chatName)
        assertNull(PersonalNames(firstName = "Amira").chatName)
        assertNull(PersonalNames(firstName = " ", useFirstNameInChats = true).chatName)
    }
}
