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

        // Kotlin Multiplatform: generate into commonMain (covers all targets, incl. androidTarget).
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            wireKmp(project, ext, fetch, cacheLocation)
        }

        // Android-only projects (not KMP): wire the Android variant sources. Guarded so a KMP project
        // — whose androidTarget also applies the Android plugin — doesn't generate twice.
        val notKmp = { !project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") }
        project.pluginManager.withPlugin("com.android.application") {
            if (notKmp()) wireAndroid(project, ext, fetch, cacheLocation)
        }
        project.pluginManager.withPlugin("com.android.library") {
            if (notKmp()) wireAndroid(project, ext, fetch, cacheLocation)
        }
    }

    companion object {
        const val GROUP = "iconfy"
    }
}
