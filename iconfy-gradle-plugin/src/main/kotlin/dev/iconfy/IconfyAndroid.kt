package dev.iconfy

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

/**
 * Registers an [IconfyGenerateTask] per Android variant and wires its generated Kotlin sources into
 * the variant so `Iconfy.*` accessors compile into the app. `addGeneratedSourceDirectory` also
 * establishes the generate→compile task dependency automatically; the generate→fetch dependency is
 * declared explicitly since the cache directory is a plain input, not the fetch task's output.
 */
internal fun wireAndroid(
    project: Project,
    ext: IconfyExtension,
    fetch: TaskProvider<IconfyFetchTask>,
    cacheLocation: Directory,
) {
    val androidComponents =
        project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return

    androidComponents.onVariants { variant ->
        val capName = variant.name.replaceFirstChar { it.uppercase() }

        val generate = project.tasks.register<IconfyGenerateTask>("iconfyGenerate$capName") {
            group = IconfyPlugin.GROUP
            description = "Generates Iconfy ImageVector accessors for the ${variant.name} variant."
            dependsOn(fetch)
            cacheDir.set(cacheLocation)
            packageName.set(ext.packageName)
            accessorName.set(ext.accessorName)
            rootDir.set(project.rootProject.layout.projectDirectory)
        }

        // Register the generated dir as a variant Java source: AGP wires the generate→compile task
        // dependency and output location automatically, and the Kotlin compilation consumes the
        // variant's Java source dirs, so the `Iconfy.*` accessors resolve. (KotlinCompile is not a
        // SourceTask, so it can't be poked directly without depending on the Kotlin Gradle plugin.)
        variant.sources.java?.addGeneratedSourceDirectory(generate, IconfyGenerateTask::generatedSrcDir)
    }
}
