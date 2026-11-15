package com.ravendarque.deaftly.core.settings

public data class PersonalNames(
    val firstName: String = "",
    val lastName: String = "",
    val legalNameDiffers: Boolean = false,
    val legalName: String = "",
    val useFirstNameInChats: Boolean = false,
) {
    val formalName: String?
        get() {
            val legal = legalName.trim()
            val full = listOf(firstName, lastName).map(String::trim).filter(String::isNotEmpty).joinToString(" ")
            return (if (legalNameDiffers && legal.isNotEmpty()) legal else full).ifEmpty { null }
        }

    val chatName: String?
        get() = firstName.trim().takeIf { useFirstNameInChats && it.isNotEmpty() }
}
