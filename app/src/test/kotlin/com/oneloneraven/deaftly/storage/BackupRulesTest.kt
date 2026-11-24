package com.oneloneraven.deaftly.storage

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupRulesTest {
    private val storeRule = "include" to ("file" to "datastore/$TEMPLATE_STORE_FILE")
    private val chatsRule = "include" to ("file" to "chats/")

    private fun children(element: Element): List<Element> =
        (0 until element.childNodes.length).mapNotNull { element.childNodes.item(it) as? Element }

    private fun rules(element: Element) = children(element).map { it.tagName to (it.getAttribute("domain") to it.getAttribute("path")) }

    private fun parse(name: String): Element =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/xml", name))
            .documentElement

    @Test
    fun fullBackupIncludesOnlyTheTemplateStoreAndKeptChats() {
        assertEquals(listOf(storeRule, chatsRule), rules(parse("backup_rules.xml")))
    }

    @Test
    fun cloudBackupAndDeviceTransferIncludeOnlyTheTemplateStoreAndKeptChats() {
        val sections = children(parse("data_extraction_rules.xml"))

        assertEquals(listOf("cloud-backup", "device-transfer"), sections.map { it.tagName })
        sections.forEach { assertEquals(listOf(storeRule, chatsRule), rules(it)) }
    }

    @Test
    fun theManifestEnablesBackupWithTheseRules() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue("""android:allowBackup="true"""" in manifest)
        assertTrue("@xml/backup_rules" in manifest && "@xml/data_extraction_rules" in manifest)
    }
}
