package dev.iconfy

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Registers a single [IconfyGenerateTask] and adds its output to the `commonMain` Kotlin source set,
 * so the generated `Iconfy.*` accessors are shared across every Kotlin Multiplatform target (Android,
 * iOS, Desktop, Web). The generated code depends only on `androidx.compose.ui`, which Compose
 * Multiplatform provides under the same package names, so no per-target code is needed.
 *
 * In a KMP project the Android target already flows through `commonMain`, so this replaces (not
 * augments) the Android variant wiring — [IconfyPlugin] guards against running both.
 */
internal fun wireKmp(
    project: Project,
    ext: IconfyExtension,
    fetch: TaskProvider<IconfyFetchTask>,
    cacheLocation: Directory,
) {
    val kotlinExt = project.extensions.findByName("kotlin") as? KotlinMultiplatformExtension ?: return

    val generate = project.tasks.register<IconfyGenerateTask>("iconfyGenerate") {
        group = IconfyPlugin.GROUP
        description = "Generates Iconfy ImageVector accessors into commonMain."
        dependsOn(fetch)
        cacheDir.set(cacheLocation)
        placements.set(ext.placements)
        packageName.set(ext.packageName)
        accessorName.set(ext.accessorName)
        rootDir.set(project.rootProject.layout.projectDirectory)
        generatedSrcDir.set(project.layout.buildDirectory.dir("generated/iconfy/commonMain"))
    }

    // Adding the task provider as a source dir registers both the directory and the build dependency,
    // so every target's Kotlin compilation depends on and sees the generated accessors.
    kotlinExt.sourceSets.named("commonMain").configure {
        kotlin.srcDir(generate)
    }
}
