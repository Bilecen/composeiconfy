package dev.iconfy

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class IconfyCodegenTest {

    private fun filledPath(color: Long) =
        VPath("M0,0h24v24h-24z", color, 1f, null, 1f, 0f, "butt", "miter", 4f, "nonZero")

    @Test
    fun `generates nested objects with lazy accessors`(@TempDir tmp: Path) {
        val out = tmp.toFile()
        val image = VectorImage(24f, 24f, 24f, 24f, listOf(filledPath(0xFF112233L)))

        IconfyCodegen.generate(
            packageName = "com.test.icons",
            accessorName = "Iconfy",
            icons = listOf(IconEntry("mdi", "home", image)),
            outDir = out,
        )

        val src = File(out, "com/test/icons/Iconfy.kt").readText()
        assertTrue(src.contains("object Iconfy"), src)
        assertTrue(src.contains("object Mdi"), src)
        assertTrue(src.contains("val Home: ImageVector"), src)
        assertTrue(src.contains("addPathNodes(\"M0,0h24v24h-24z\")"), src)
        assertTrue(src.contains("Color(0xFF112233L)"), src)
    }

    @Test
    fun `same icon name in different sets does not collide`(@TempDir tmp: Path) {
        val out = tmp.toFile()
        val image = VectorImage(24f, 24f, 24f, 24f, listOf(filledPath(0xFF000000L)))

        IconfyCodegen.generate(
            packageName = "com.test.icons",
            accessorName = "Iconfy",
            icons = listOf(
                IconEntry("mdi", "home", image),
                IconEntry("tabler", "home", image),
            ),
            outDir = out,
        )

        val src = File(out, "com/test/icons/Iconfy.kt").readText()
        assertTrue(src.contains("object Mdi"))
        assertTrue(src.contains("object Tabler"))
    }
}
