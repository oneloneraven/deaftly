package com.ravendarque.deaftly.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonalNamesTest {
    @Test
    fun theFormalNameIsTheFirstNameAndLastNameTogether() {
        assertEquals("Amira Hassan", PersonalNames(firstName = "Amira", lastName = "Hassan").formalName)
    }

    @Test
    fun blankPartsAreIgnoredAndTheRestIsTrimmed() {
        assertEquals("Amira", PersonalNames(firstName = "  Amira ", lastName = "   ").formalName)
        assertEquals("Hassan", PersonalNames(firstName = "", lastName = " Hassan ").formalName)
    }

    @Test
    fun noNamesMeansNoFormalName() {
        assertNull(PersonalNames().formalName)
        assertNull(PersonalNames(firstName = " ", lastName = " ", legalNameDiffers = true, legalName = " ").formalName)
    }

    @Test
    fun aDifferentLegalNameReplacesTheFirstNameAndLastName() {
        val names = PersonalNames("Tunde", "Bello", legalNameDiffers = true, legalName = " Olatunde Bello ")

        assertEquals("Olatunde Bello", names.formalName)
    }

    @Test
    fun theLegalNameIsIgnoredUnlessTheFlagIsSet() {
        assertEquals("Tunde Bello", PersonalNames("Tunde", "Bello", legalName = "Olatunde Bello").formalName)
    }

    @Test
    fun aBlankLegalNameFallsBackToTheFirstNameAndLastName() {
        assertEquals("Tunde Bello", PersonalNames("Tunde", "Bello", legalNameDiffers = true, legalName = " ").formalName)
    }

    @Test
    fun theChatNameIsTheTrimmedFirstNameOnlyWhenChosen() {
        assertEquals("Amira", PersonalNames(firstName = " Amira ", useFirstNameInChats = true).chatName)
        assertNull(PersonalNames(firstName = "Amira").chatName)
        assertNull(PersonalNames(firstName = " ", useFirstNameInChats = true).chatName)
    }
}
