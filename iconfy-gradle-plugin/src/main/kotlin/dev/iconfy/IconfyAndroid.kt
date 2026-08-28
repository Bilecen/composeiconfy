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

        // Register the generated .kt dir so it compiles with the full Kotlin classpath. The right
        // target depends on who owns the Kotlin compilation, which we detect by plugin presence:
        //  - AGP 8.x + the separate `kotlin-android` plugin: its Kotlin compile task consumes the
        //    variant's *Java* source dirs, and `sources.kotlin` does NOT feed compilation → use java.
        //  - AGP 9+ built-in Kotlin (no `kotlin-android` plugin): AGP owns Kotlin and `sources.kotlin`
        //    is the correct target; putting .kt under `sources.java` yields a broken compilation
        //    (unresolved `let`/`apply`/…).
        // addGeneratedSourceDirectory also wires the generate→compile task dependency.
        val kotlinAndroidApplied = project.pluginManager.hasPlugin("org.jetbrains.kotlin.android")
        val kotlinSources = variant.sources.kotlin
        if (!kotlinAndroidApplied && kotlinSources != null) {
            kotlinSources.addGeneratedSourceDirectory(generate, IconfyGenerateTask::generatedSrcDir)
        } else {
            variant.sources.java?.addGeneratedSourceDirectory(generate, IconfyGenerateTask::generatedSrcDir)
        }
    }
}
