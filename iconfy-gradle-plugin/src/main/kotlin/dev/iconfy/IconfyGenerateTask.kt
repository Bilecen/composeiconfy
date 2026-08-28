package dev.iconfy

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Converts the cached `<prefix>/<name>.svg` tree into a single Kotlin file of `ImageVector`
 * accessors grouped by prefix (`Iconfy.Mdi.Home`). No network access — pure codegen over the
 * fetch output, via our own [SvgToImageVector] + [IconfyCodegen] (compose-ui only, no JitPack).
 */
abstract class IconfyGenerateTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cacheDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val accessorName: Property<String>

    @get:OutputDirectory
    abstract val generatedSrcDir: DirectoryProperty

    /** Root dir for pretty log paths; not an input (Internal) so it doesn't affect up-to-date checks. */
    @get:Internal
    abstract val rootDir: DirectoryProperty

    @TaskAction
    fun run() {
        val out = generatedSrcDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()

        val svgs = cacheDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension.equals("svg", ignoreCase = true) }
            .sortedBy { it.invariantSeparatorsPath }
            .toList()
        if (svgs.isEmpty()) {
            logger.lifecycle("iconfy: no cached icons — nothing to generate.")
            return
        }

        val entries = ArrayList<IconEntry>(svgs.size)
        val failed = mutableListOf<String>()
        for (svg in svgs) {
            val prefix = svg.parentFile.name
            val name = svg.nameWithoutExtension
            SvgToImageVector.convert(svg)
                .onSuccess { entries += IconEntry(prefix, name, it) }
                .onFailure { failed += "$prefix:$name (${it.message})" }
        }

        if (entries.isEmpty()) {
            throw GradleException("iconfy: no icons could be converted: ${failed.joinToString()}")
        }

        IconfyCodegen.generate(packageName.get(), accessorName.get(), entries, out)

        if (failed.isNotEmpty()) {
            logger.warn("iconfy: ${failed.size} icon(s) skipped (unrepresentable): ${failed.joinToString()}")
        }
        val where = rootDir.orNull?.asFile?.let { runCatching { out.relativeTo(it) }.getOrDefault(out) } ?: out
        logger.lifecycle("iconfy: generated ${entries.size} ${accessorName.get()} accessor(s) into $where")
    }
}
