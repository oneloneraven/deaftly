package com.oneloneraven.deaftly.core.settings

import com.oneloneraven.deaftly.core.template.PersonalField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonalNamesTest {
    private val amira = PersonalNames(firstName = " Amira ", lastName = " Hassan ")

    @Test
    fun theFirstNameAndLastNameAreEachTrimmed() {
        assertEquals("Amira", amira.valueOf(PersonalField.FirstName))
        assertEquals("Hassan", amira.valueOf(PersonalField.LastName))
    }

    @Test
    fun theFullNameIsTheFirstNameAndLastNameTogether() {
        assertEquals("Amira Hassan", amira.valueOf(PersonalField.FullName))
    }

    @Test
    fun blankPartsAreLeftOutOfTheFullName() {
        assertEquals("Amira", PersonalNames(firstName = "  Amira ", lastName = "   ").valueOf(PersonalField.FullName))
        assertEquals("Hassan", PersonalNames(firstName = "", lastName = " Hassan ").valueOf(PersonalField.FullName))
    }

    @Test
    fun noNamesMeansNoValueForAnyField() {
        val blank = PersonalNames(firstName = " ", lastName = " ", legalNameDiffers = true, legalName = " ")
        PersonalField.entries.forEach { field ->
            assertNull(PersonalNames().valueOf(field), field.name)
            assertNull(blank.valueOf(field), field.name)
        }
    }

    @Test
    fun theLegalNameIsTheFullNameUnlessItIsDifferent() {
        assertEquals("Tunde Bello", PersonalNames("Tunde", "Bello").valueOf(PersonalField.LegalName))
    }

    @Test
    fun aDifferentLegalNameReplacesOnlyTheLegalName() {
        val names = PersonalNames("Tunde", "Bello", legalNameDiffers = true, legalName = " Olatunde Bello ")

        assertEquals("Olatunde Bello", names.valueOf(PersonalField.LegalName))
        assertEquals("Tunde Bello", names.valueOf(PersonalField.FullName))
        assertEquals("Tunde", names.valueOf(PersonalField.FirstName))
    }

    @Test
    fun theDifferentLegalNameIsIgnoredUnlessTheFlagIsSet() {
        assertEquals("Tunde Bello", PersonalNames("Tunde", "Bello", legalName = "Olatunde Bello").valueOf(PersonalField.LegalName))
    }

    @Test
    fun aBlankDifferentLegalNameFallsBackToTheFullName() {
        assertEquals(
            "Tunde Bello",
            PersonalNames("Tunde", "Bello", legalNameDiffers = true, legalName = " ").valueOf(PersonalField.LegalName),
        )
    }

    @Test
    fun theChatNameIsTheTrimmedFirstNameOnlyWhenChosen() {
        assertEquals("Amira", PersonalNames(firstName = " Amira ", useFirstNameInChats = true).chatName)
        assertNull(PersonalNames(firstName = "Amira").chatName)
        assertNull(PersonalNames(firstName = " ", useFirstNameInChats = true).chatName)
    }
}
