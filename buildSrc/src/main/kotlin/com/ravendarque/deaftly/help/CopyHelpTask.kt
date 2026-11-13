package com.ravendarque.deaftly.help

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class CopyHelpTask
    @Inject
    constructor(
        private val fileSystem: FileSystemOperations,
    ) : DefaultTask() {
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.NAME_ONLY)
        abstract val pages: ConfigurableFileCollection

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        @TaskAction
        fun copy() {
            fileSystem.sync {
                from(pages)
                into(outputDir.dir("help"))
            }
        }
    }
