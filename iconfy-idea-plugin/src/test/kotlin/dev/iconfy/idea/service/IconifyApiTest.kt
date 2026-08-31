package dev.iconfy.idea.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Live-API smoke tests for the Iconify client (network required). */
class IconifyApiTest {

    private val api = IconifyApi()

    @Test
    fun `search returns prefix name coordinates`() {
        val result = api.search("home", limit = 20)
        assertTrue(result.icons.isNotEmpty(), "expected search hits for 'home'")
        assertTrue(result.icons.all { it.contains(":") }, "icons must be 'prefix:name': ${result.icons.take(3)}")
        assertTrue(result.icons.any { it.substringAfter(":").contains("home") }, "expected a home-ish icon")
    }

    @Test
    fun `collections lists known icon sets`() {
        val sets = api.collections()
        assertTrue(sets.size > 100, "expected 100+ icon sets, got ${sets.size}")
        assertTrue("mdi" in sets, "expected the mdi set")
        assertTrue((sets["mdi"]?.total ?: 0) > 5000, "mdi should have thousands of icons")
    }

    @Test
    fun `collection lists bare icon names`() {
        val detail = api.collection("lucide")
        assertTrue(detail.allNames().isNotEmpty(), "expected lucide icon names")
        assertTrue(detail.allNames().none { it.contains(":") }, "collection names are bare (no prefix)")
    }

    @Test
    fun `svg returns markup for a known icon`() {
        val svg = api.svg("mdi", "home")
        assertTrue(svg.trimStart().startsWith("<svg"), "expected SVG markup, got: ${svg.take(40)}")
        assertTrue(svg.contains("<path"), "expected a path in the SVG")
    }
}
