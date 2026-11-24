package com.oneloneraven.deaftly.nativelibs

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.inject.Inject

abstract class FetchArchiveTask
    @Inject
    constructor(
        private val files: FileSystemOperations,
        private val archives: ArchiveOperations,
    ) : DefaultTask() {
        @get:Input
        abstract val url: Property<String>

        @get:Input
        abstract val sha256: Property<String>

        @get:Input
        abstract val includes: ListProperty<String>

        @get:Input
        abstract val stripFirstDir: Property<Boolean>

        @get:Input
        abstract val prefix: Property<String>

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        init {
            stripFirstDir.convention(true)
            prefix.convention("")
        }

        @TaskAction
        fun fetch() {
            val source = url.get()
            val archive = temporaryDir.resolve("archive")
            val hash = MessageDigest.getInstance("SHA-256")
            val connection =
                URI(source).toURL().openConnection().apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
            DigestInputStream(connection.getInputStream(), hash).use {
                Files.copy(it, archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            val actual = hash.digest().joinToString("") { "%02x".format(it) }
            check(actual == sha256.get()) { "Archive checksum mismatch for $source: expected ${sha256.get()}, got $actual" }
            val tree =
                when {
                    source.endsWith(".zip") -> archives.zipTree(archive)
                    source.endsWith(".tar.gz") -> archives.tarTree(archives.gzip(archive))
                    else -> error("Unsupported archive type: $source")
                }
            val patterns = includes.getOrElse(emptyList())
            val strip = stripFirstDir.get()
            val root = prefix.get()
            files.sync {
                from(tree) {
                    if (patterns.isNotEmpty()) include(patterns)
                    eachFile {
                        val stripped = if (strip) path.substringAfter('/') else path
                        path = if (root.isEmpty()) stripped else "$root/$stripped"
                    }
                    includeEmptyDirs = false
                }
                into(outputDir)
            }
        }

        private companion object {
            const val TIMEOUT_MS = 60_000
        }
    }
