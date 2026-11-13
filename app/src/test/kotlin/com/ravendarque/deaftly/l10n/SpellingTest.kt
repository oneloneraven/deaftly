package com.ravendarque.deaftly.l10n

import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.R
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val endings = listOf("", "s", "es", "d", "ed", "ing", "er", "ers", "ation", "ations")

private fun forms(stem: String): List<String> =
    if (stem.endsWith("e")) {
        endings.map { stem.dropLast(1) + "e" + it } + endings.drop(5).map { stem.dropLast(1) + it }
    } else {
        endings.map {
            stem +
                it
        }
    }

private fun words(vararg stems: String) = Regex("\\b(?:${stems.flatMap(::forms).joinToString("|")})\\b", RegexOption.IGNORE_CASE)

private val usOrEr = arrayOf("color", "favorite", "center", "gray", "behavior", "flavor", "honor", "neighbor", "theater", "defense")
private val ukOur = arrayOf("colour", "favourite", "centre", "grey", "behaviour", "flavour", "honour", "neighbour", "theatre", "defence")
private val usIze =
    arrayOf(
        "organize",
        "recognize",
        "customize",
        "realize",
        "apologize",
        "minimize",
        "maximize",
        "summarize",
        "prioritize",
        "authorize",
        "personalize",
        "normalize",
        "synchronize",
        "analyze",
    )
private val ukIse =
    arrayOf(
        "organise",
        "recognise",
        "customise",
        "realise",
        "apologise",
        "minimise",
        "maximise",
        "summarise",
        "prioritise",
        "authorise",
        "personalise",
        "normalise",
        "synchronise",
        "analyse",
    )
private val usOther =
    arrayOf("traveling", "canceled", "labeled", "modeling", "fulfill", "enrollment", "skeptic", "sulfite", "mollusk", "catalog")
private val ukOther =
    arrayOf("travelling", "cancelled", "labelled", "modelling", "fulfil", "enrolment", "sceptic", "sulphite", "mollusc", "catalogue")
private val ukVocabulary = arrayOf("torch", "toilet", "lift", "till", "queue", "chemist", "soya")

private val usSpellings = words(*usOrEr, *usIze, *usOther)
private val ukSpellings = words(*ukOur, *ukIse, *ukOther)
private val canadianExcluded = words(*usOrEr, "sulfite", "mollusk", *ukIse)
private val ukVocabularyInUs = words(*ukVocabulary)

@RunWith(AndroidJUnit4::class)
class SpellingTest {
    private fun texts(resources: Resources): Map<String, String> {
        val strings =
            R.string::class.java.fields
                .filter { it.type == Int::class.javaPrimitiveType }
                .map { it.name to listOf(resources.getString(it.getInt(null))) }
        val arrays =
            R.array::class.java.fields
                .filter { it.type == Int::class.javaPrimitiveType }
                .map { it.name to resources.getStringArray(it.getInt(null)).toList() }
        return (strings + arrays).flatMap { (name, values) -> values.mapIndexed { index, value -> "$name[$index]" to value } }.toMap()
    }

    private fun offenders(
        qualifiers: String,
        pattern: Regex,
    ): List<String> =
        texts(resourcesFor(qualifiers)).mapNotNull { (key, value) ->
            pattern.find(value)?.let { "$key: \"${it.value}\" in \"$value\"" }
        }

    private fun assertNone(
        qualifiers: String,
        pattern: Regex,
    ) = assertEquals(emptyList(), offenders(qualifiers, pattern))

    @Test
    fun theScannerCoversEveryStringAndArrayResource() {
        val keys = texts(resourcesFor("en-rGB")).keys

        assertTrue("template_check_in_body[0]" in keys)
        assertTrue("placeholder_allergen_options[13]" in keys)
        assertTrue(keys.size > 200)
    }

    @Test
    fun theScannerFlagsWhatItShould() {
        listOf(
            "Color",
            "favorite",
            "organizing",
            "Grays",
            "centers",
            "mollusks",
        ).forEach { assertTrue(usSpellings.containsMatchIn(it), it) }
        listOf(
            "Colour",
            "favourite",
            "organising",
            "Greys",
            "centres",
            "molluscs",
        ).forEach { assertTrue(ukSpellings.containsMatchIn(it), it) }
        assertTrue(!usSpellings.containsMatchIn("Colour centre"))
        assertTrue(!ukSpellings.containsMatchIn("Color center"))
    }

    @Test
    fun otherEnglishLocalesFollowTheBritishDefault() {
        val default = texts(resourcesFor("fr"))

        listOf("en-rGB", "en-rIN", "en-rZA", "en-rSG").forEach { assertEquals(default, texts(resourcesFor(it)), it) }
    }

    @Test
    fun britishEnglishHasNoAmericanSpellings() = assertNone("en-rGB", usSpellings)

    @Test
    fun americanEnglishHasNoBritishSpellingsOrVocabulary() {
        assertNone("en-rUS", ukSpellings)
        assertNone("en-rUS", ukVocabularyInUs)
    }

    @Test
    fun canadianEnglishHasNoAmericanOrBritishVariantsItDoesNotUse() = assertNone("en-rCA", canadianExcluded)

    @Test
    fun australianEnglishHasNoAmericanSpellings() = assertNone("en-rAU", usSpellings)

    @Test
    fun newZealandEnglishHasNoAmericanSpellings() = assertNone("en-rNZ", usSpellings)

    @Test
    fun irishEnglishHasNoAmericanSpellings() = assertNone("en-rIE", usSpellings)
}
