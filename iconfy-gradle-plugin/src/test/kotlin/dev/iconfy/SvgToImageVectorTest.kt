package dev.iconfy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SvgToImageVectorTest {

    private fun allPaths(image: VectorImage): List<VPath> {
        val out = mutableListOf<VPath>()
        fun walk(nodes: List<VNode>) {
            for (n in nodes) when (n) {
                is VPath -> out += n
                is VGroup -> walk(n.children)
            }
        }
        walk(image.nodes)
        return out
    }

    @Test
    fun `converts a filled currentColor path`(@TempDir tmp: Path) {
        val svg = File(tmp.toFile(), "home.svg").apply {
            writeText(
                """<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">""" +
                    """<path fill="currentColor" d="M10 20v-6h4v6h5v-8h3L12 3L2 12h3v8z"/></svg>"""
            )
        }

        val image = SvgToImageVector.convert(svg).getOrThrow()

        assertEquals(24f, image.viewportWidth)
        assertEquals(24f, image.viewportHeight)
        val paths = allPaths(image)
        assertTrue(paths.isNotEmpty(), "expected at least one path")
        // currentColor was mapped to opaque black.
        assertEquals(0xFF000000L, paths.first().fillColor)
    }

    @Test
    fun `converts a stroke-only path`(@TempDir tmp: Path) {
        val svg = File(tmp.toFile(), "user.svg").apply {
            writeText(
                """<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">""" +
                    """<path fill="none" stroke="currentColor" stroke-width="2" """ +
                    """d="M8 7a4 4 0 1 0 8 0a4 4 0 0 0-8 0"/></svg>"""
            )
        }

        val paths = allPaths(SvgToImageVector.convert(svg).getOrThrow())
        assertTrue(paths.isNotEmpty())
        val p = paths.first()
        assertEquals(0xFF000000L, p.strokeColor)
        assertTrue(p.strokeWidth > 0f, "stroke width should be preserved")
    }
}
