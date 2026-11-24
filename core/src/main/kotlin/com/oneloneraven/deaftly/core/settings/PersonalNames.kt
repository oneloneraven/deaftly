package com.oneloneraven.deaftly.core.settings

import com.oneloneraven.deaftly.core.template.PersonalField

public data class PersonalNames(
    val firstName: String = "",
    val lastName: String = "",
    val legalNameDiffers: Boolean = false,
    val legalName: String = "",
    val useFirstNameInChats: Boolean = false,
) {
    public fun valueOf(field: PersonalField): String? {
        val first = firstName.trim()
        val last = lastName.trim()
        val full = listOf(first, last).filter(String::isNotEmpty).joinToString(" ")
        val differentLegal = legalName.trim().takeIf { legalNameDiffers && it.isNotEmpty() }
        return when (field) {
            PersonalField.FirstName -> first
            PersonalField.LastName -> last
            PersonalField.FullName -> full
            PersonalField.LegalName -> differentLegal ?: full
        }.ifEmpty { null }
    }

    val chatName: String?
        get() = firstName.trim().takeIf { useFirstNameInChats && it.isNotEmpty() }
}
