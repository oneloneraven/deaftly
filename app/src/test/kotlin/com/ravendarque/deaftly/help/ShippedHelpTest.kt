package com.ravendarque.deaftly.help

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val RelativeLink = Regex("""\]\(([^)#]+\.md)(?:#[^)]*)?\)""")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShippedHelpTest {
    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    private val sourceFiles = File("../help").listFiles { file -> file.extension == "md" }.orEmpty().sortedBy { it.name }

    private val topics = HelpRepository(assets).topics()

    @Test
    fun everyHelpFileIsShippedAndParses() {
        assertTrue(sourceFiles.isNotEmpty())
        assertEquals(sourceFiles.map { it.nameWithoutExtension }.sorted(), topics.map { it.slug }.sorted())
        topics.forEach { assertTrue(it.title.isNotBlank() && it.body.isNotBlank(), it.slug) }
    }

    @Test
    fun theOrdersAreUniqueAndTheTopicsComeBackInOrder() {
        assertEquals(topics.size, topics.map { it.order }.distinct().size)
        assertEquals(topics.sortedBy { it.order }, topics)
    }

    @Test
    fun everyRelativeLinkPointsAtAnExistingPage() {
        val slugs = topics.map { it.slug }.toSet()
        topics.forEach { topic ->
            RelativeLink.findAll(topic.body).forEach { link ->
                val target = link.groupValues[1].removeSuffix(".md")
                assertTrue(target in slugs, "${topic.slug} links to missing page $target")
            }
        }
    }
}
