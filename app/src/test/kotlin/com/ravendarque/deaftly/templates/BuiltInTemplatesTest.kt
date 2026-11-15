package com.ravendarque.deaftly.templates

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.template.Join
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateFill
import com.ravendarque.deaftly.core.template.TemplateRenderer
import com.ravendarque.deaftly.core.template.TemplateSegment
import com.ravendarque.deaftly.storage.toDomain
import com.ravendarque.deaftly.storage.toDto
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BuiltInTemplatesTest {
    private val templates = builtInTemplates(RuntimeEnvironment.getApplication().resources)
    private val replyPhrases = listOf("type it on my phone", "write down", "show me")
    private val token = Regex("""\{[^}]*}""")

    private fun wordCounts(body: String): List<Int> =
        body
            .replace(token, "x")
            .split('.', '?', '!')
            .map { sentence -> sentence.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
            .filter { it > 0 }

    private fun byId(id: String): Template = templates.first { it.id == id }

    @Test
    fun coversTheRequiredSituations() {
        val ids = templates.map { it.id }

        assertTrue(templates.size >= 8)
        listOf(
            BuiltInIds.CHECK_IN,
            BuiltInIds.ORDER_FOOD,
            BuiltInIds.ASK_HELP,
            BuiltInIds.EXPLAIN_DEAF,
            BuiltInIds.BSL_INTERPRETER,
            BuiltInIds.DOCTOR,
            BuiltInIds.PHARMACY,
            BuiltInIds.BUY_TICKET,
            BuiltInIds.PAYING,
        ).forEach { assertTrue(it in ids, "Missing $it") }
    }

    @Test
    fun idsAreUniqueAndEveryTemplateIsBuiltIn() {
        assertEquals(templates.size, templates.map { it.id }.toSet().size)
        assertTrue(templates.all { it.builtIn })
    }

    @Test
    fun everySentenceIsFourteenWordsOrFewer() {
        templates.forEach { template ->
            wordCounts(template.body).forEach { count ->
                assertTrue(count <= 14, "${template.id} has a sentence of $count words")
            }
        }
    }

    @Test
    fun everyTemplateTellsTheHearingPersonHowToReply() {
        templates.forEach { template ->
            val explicit = replyPhrases.any { it in template.body }
            assertTrue("{how to reply}" in template.body || explicit, "${template.id} gives no reply instruction")
        }
    }

    @Test
    fun everyPlaceholderInABodyIsDefined() {
        templates.forEach { template ->
            val keys = template.segments().filterIsInstance<TemplateSegment.PlaceholderRef>().map { it.key }
            keys.forEach { key ->
                assertTrue(key in template.placeholders, "${template.id} does not define {$key}")
            }
            assertEquals(keys.toSet(), template.placeholders.keys)
        }
    }

    @Test
    fun howToReplyOffersTheFivePresetChoices() {
        val placeholder = byId(BuiltInIds.CHECK_IN).placeholders.getValue("how to reply")

        assertEquals(PlaceholderType.Choices, placeholder.type)
        assertEquals(
            listOf("type on my phone", "type on your phone", "write it down", "point to the words", "face me and speak clearly"),
            placeholder.options,
        )
    }

    @Test
    fun pinsCheckInAndOrderFoodOnly() {
        assertEquals(setOf(BuiltInIds.CHECK_IN, BuiltInIds.ORDER_FOOD), templates.filter { it.pinned }.map { it.id }.toSet())
    }

    @Test
    fun placeholderKindsMatchTheStarterSpecification() {
        assertEquals(PlaceholderType.Camera, byId(BuiltInIds.ORDER_FOOD).placeholders.getValue("menu item").type)
        assertTrue(byId(BuiltInIds.ORDER_FOOD).placeholders.getValue("menu item").repeatable)
        assertEquals(
            14,
            byId(BuiltInIds.FOOD_ALLERGY)
                .placeholders
                .getValue("allergen")
                .options.size,
        )
        assertEquals(listOf("card", "cash", "phone"), byId(BuiltInIds.PAYING).placeholders.getValue("payment").options)
    }

    @Test
    fun theBslInterpreterTemplateFollowsExplainImDeafAndIsNotPinned() {
        val ids = templates.map { it.id }
        val interpreter = byId(BuiltInIds.BSL_INTERPRETER)

        assertEquals(ids.indexOf(BuiltInIds.EXPLAIN_DEAF) + 1, ids.indexOf(BuiltInIds.BSL_INTERPRETER))
        assertEquals("BSL interpreter", interpreter.title)
        assertEquals("Everyday", interpreter.category)
        assertEquals(
            "Hi! I'm deaf. My name is {name}. I should have a BSL interpreter booked. Are they here yet? To reply, please {how to reply}.",
            interpreter.body,
        )
        assertEquals(setOf("name", "how to reply"), interpreter.placeholders.keys)
        assertFalse(interpreter.pinned)
    }

    @Test
    fun theDoctorTemplateAsksHowYouWillCommunicateAndHowToReply() {
        val doctor = byId(BuiltInIds.DOCTOR)
        val communicate = doctor.placeholders.getValue("how I communicate")

        assertEquals(
            "Hi! I'm deaf. My name is {name}. I have an appointment at {time} with {doctor}. " +
                "I prefer to communicate by {how I communicate}. If you need to reply, please {how to reply}.",
            doctor.body,
        )
        assertEquals("Who is it with?", doctor.placeholders.getValue("doctor").label)
        assertEquals(PlaceholderType.Typing, doctor.placeholders.getValue("doctor").type)
        assertEquals(byId(BuiltInIds.CHECK_IN).placeholders.getValue("name"), doctor.placeholders.getValue("name"))
        assertEquals(PlaceholderType.Choices, communicate.type)
        assertTrue(communicate.repeatable)
        assertEquals("How will you communicate?", communicate.label)
        assertEquals(listOf("BSL interpreter", "speech to text", "typing on my phone", "lipreading"), communicate.options)
        assertEquals(byId(BuiltInIds.CHECK_IN).placeholders.getValue("how to reply"), doctor.placeholders.getValue("how to reply"))
    }

    @Test
    fun theDoctorTemplateJoinsTwoCommunicationChoices() {
        val fill =
            TemplateFill()
                .set("name", "Marcus Campbell")
                .set("time", "10:30")
                .set("doctor", "Dr Shah")
                .add("how I communicate", "lipreading")
                .add("how I communicate", "typing on my phone")
                .set("how to reply", "write it down")

        assertEquals(
            "Hi! I'm deaf. My name is Marcus Campbell. I have an appointment at 10:30 with Dr Shah. " +
                "I prefer to communicate by lipreading and typing on my phone. If you need to reply, please write it down.",
            TemplateRenderer.render(byId(BuiltInIds.DOCTOR), fill).text,
        )
    }

    @Test
    fun theDoctorTemplateRendersTheWebsiteMessage() {
        val fill =
            TemplateFill()
                .set("name", "Marcus Campbell")
                .set("time", "10:30")
                .set("doctor", "Dr Shah")
                .set("how I communicate", "typing on my phone")
                .add("how to reply", "write it down")
                .add("how to reply", "type on my phone")

        assertEquals(
            "Hi! I'm deaf. My name is Marcus Campbell. I have an appointment at 10:30 with Dr Shah. " +
                "I prefer to communicate by typing on my phone. " +
                "If you need to reply, please write it down or type on my phone.",
            TemplateRenderer.render(byId(BuiltInIds.DOCTOR), fill).text,
        )
    }

    @Test
    fun howToReplyJoinsWithOrAndFoodJoinsWithAnd() {
        val repliers = templates.filter { "how to reply" in it.placeholders }

        assertTrue(repliers.size >= 8)
        repliers.forEach {
            val reply = it.placeholders.getValue("how to reply")
            assertTrue(reply.repeatable, it.id)
            assertEquals(Join.Or, reply.join, it.id)
        }
        assertEquals(Join.And, byId(BuiltInIds.ORDER_FOOD).placeholders.getValue("menu item").join)
        assertEquals(Join.And, byId(BuiltInIds.FOOD_ALLERGY).placeholders.getValue("allergen").join)
    }

    @Test
    fun survivesStorageMapping() {
        assertEquals(templates, templates.map { it.toDto().toDomain() })
    }
}
