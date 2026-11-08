package com.ravendarque.deaftly.nativelibs

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

abstract class FetchNcnnTask
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
        abstract val abis: ListProperty<String>

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        @TaskAction
        fun fetch() {
            val zip = temporaryDir.resolve("ncnn.zip")
            val hash = MessageDigest.getInstance("SHA-256")
            val connection =
                URI(url.get()).toURL().openConnection().apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
            DigestInputStream(connection.getInputStream(), hash).use {
                Files.copy(it, zip.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            val actual = hash.digest().joinToString("") { "%02x".format(it) }
            check(actual == sha256.get()) { "ncnn archive checksum mismatch: expected ${sha256.get()}, got $actual" }
            files.sync {
                from(archives.zipTree(zip)) {
                    include(abis.get().map { "*/$it/**" })
                    eachFile { path = path.substringAfter('/') }
                    includeEmptyDirs = false
                }
                into(outputDir)
            }
        }

        private companion object {
            const val TIMEOUT_MS = 60_000
        }
    }
