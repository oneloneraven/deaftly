package com.ravendarque.deaftly.guard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class VerifyReleaseManifestTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val permissionAllowlist: RegularFileProperty

    @get:Input
    abstract val appNamespace: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val namespace = appNamespace.get()
        val allowed = allowedPermissions(permissionAllowlist.get().asFile.readText(), namespace)
        val violations = manifestViolations(mergedManifest.get().asFile.readText(), namespace, allowed)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "The release manifest has permissions or exported components nobody has reviewed (ADR-0011).\n" +
                    "If a feature needs one, add it to $ALLOWLIST_PATH in the same PR as its threat model and privacy update:\n" +
                    violations.joinToString("\n") { "  - $it" },
            )
        }
        report.get().asFile.writeText("ok\n")
    }
}
