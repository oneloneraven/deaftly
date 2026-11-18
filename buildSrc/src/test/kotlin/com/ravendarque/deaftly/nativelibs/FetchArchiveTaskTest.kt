package com.ravendarque.deaftly.nativelibs

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FetchArchiveTaskTest {
    private val work = createTempDirectory("fetch-archive").toFile()

    private fun tarGz(entries: Map<String, String>): File {
        val file = work.resolve("src.tar.gz")
        GZIPOutputStream(file.outputStream()).use { out ->
            entries.forEach { (name, text) ->
                val bytes = text.toByteArray()
                val header = ByteArray(BLOCK)
                name.toByteArray().copyInto(header)
                "0000644".toByteArray().copyInto(header, 100)
                "%011o".format(bytes.size).toByteArray().copyInto(header, 124)
                header[156] = '0'.code.toByte()
                "ustar".toByteArray().copyInto(header, 257)
                "        ".toByteArray().copyInto(header, 148)
                "%06o".format(header.sumOf { it.toInt() and 0xff }).toByteArray().copyInto(header, 148)
                header[154] = 0
                header[155] = ' '.code.toByte()
                out.write(header)
                out.write(bytes)
                out.write(ByteArray((BLOCK - bytes.size % BLOCK) % BLOCK))
            }
            out.write(ByteArray(BLOCK * 2))
        }
        return file
    }

    private fun sha(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private fun fetch(
        archive: File,
        checksum: String = sha(archive),
        configure: FetchArchiveTask.() -> Unit = {},
    ): File {
        val out = work.resolve("out")
        val task =
            ProjectBuilder
                .builder()
                .withProjectDir(work)
                .build()
                .tasks
                .create("fetch", FetchArchiveTask::class.java)
        task.url.set(archive.toURI().toString())
        task.sha256.set(checksum)
        task.outputDir.set(out)
        task.configure()
        task.fetch()
        return out
    }

    @Test
    fun extractsTarGzStrippingTheFirstDirectory() {
        val out = fetch(tarGz(mapOf("pkg-1/a.txt" to "a", "pkg-1/sub/b.txt" to "b")))
        assertEquals("a", out.resolve("a.txt").readText())
        assertEquals("b", out.resolve("sub/b.txt").readText())
    }

    @Test
    fun keepsTheFirstDirectoryWhenAsked() {
        val out = fetch(tarGz(mapOf("pkg-1/a.txt" to "a"))) { stripFirstDir.set(false) }
        assertTrue(out.resolve("pkg-1/a.txt").isFile)
    }

    @Test
    fun includesNarrowTheExtraction() {
        val out = fetch(tarGz(mapOf("pkg-1/keep/a.txt" to "a", "pkg-1/skip/b.txt" to "b"))) { includes.set(listOf("*/keep/**")) }
        assertTrue(out.resolve("keep/a.txt").isFile)
        assertFalse(out.resolve("skip").exists())
    }

    @Test
    fun refusesAChecksumMismatch() {
        assertFailsWith<IllegalStateException> { fetch(tarGz(mapOf("pkg-1/a.txt" to "a")), checksum = "0".repeat(64)) }
    }

    private companion object {
        const val BLOCK = 512
    }
}
