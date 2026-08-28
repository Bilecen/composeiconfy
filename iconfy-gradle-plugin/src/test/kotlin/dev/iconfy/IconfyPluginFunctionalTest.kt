package dev.iconfy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class IconfyPluginFunctionalTest {

    private fun seed(projectDir: File) {
        File(projectDir, "settings.gradle.kts").writeText("""rootProject.name = "it-test"""")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("io.github.bilecen.iconfy") }
            iconfy { icons { add("mdi:home") } }
            """.trimIndent()
        )
    }

    private fun runner(projectDir: File, vararg args: String) =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args)

    @Test
    fun `fetch is cache-first and works offline`(@TempDir tmp: Path) {
        val dir = tmp.toFile()
        seed(dir)
        // Pre-seed the cache so no network is needed.
        File(dir, ".iconfy/cache/mdi").mkdirs()
        File(dir, ".iconfy/cache/mdi/home.svg").writeText(
            """<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">""" +
                """<path fill="currentColor" d="M10 20v-6h4v6h5v-8h3L12 3L2 12h3v8z"/></svg>"""
        )

        val result = runner(dir, "iconfyFetch", "--offline").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":iconfyFetch")?.outcome)
    }

    @Test
    fun `offline build fails clearly when an icon is not cached`(@TempDir tmp: Path) {
        val dir = tmp.toFile()
        seed(dir)
        // No cache seeded → offline cannot fetch → build fails with our message.
        val result = runner(dir, "iconfyFetch", "--offline").buildAndFail()

        assertTrue(
            result.output.contains("mdi:home") && result.output.contains("offline"),
            "expected an actionable offline cache-miss error, was:\n${result.output}",
        )
    }
}
