package dev.iconfy

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Applies the `iconfy` extension and registers the build-time icon pipeline:
 *  - [IconfyFetchTask] (`iconfyFetch`) — always registered; downloads & caches declared icons.
 *  - Codegen + Android source-set wiring — attached only when an Android plugin is present
 *    (see Faz 2 [wireAndroid]).
 */
class IconfyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("iconfy", IconfyExtension::class.java)

        // Durable cache outside build/ so `clean` doesn't force a re-download.
        val cacheLocation = project.rootProject.layout.projectDirectory.dir(".iconfy/cache")

        val fetch = project.tasks.register<IconfyFetchTask>("iconfyFetch") {
            group = GROUP
            description = "Downloads declared Iconify icons and caches them as SVG."
            placements.set(ext.placements)
            apiUrl.set(ext.apiUrl)
            failOnMissing.set(ext.failOnMissing)
            offline.set(project.gradle.startParameter.isOffline)
            cacheDir.set(cacheLocation)
        }

        // Faz 2: generate ImageVector sources and register them with the Android variant.
        project.pluginManager.withPlugin("com.android.application") { wireAndroid(project, ext, fetch, cacheLocation) }
        project.pluginManager.withPlugin("com.android.library") { wireAndroid(project, ext, fetch, cacheLocation) }
    }

    companion object {
        const val GROUP = "iconfy"
    }
}
