package dev.iconfy

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class IconfyCodegenTest {

    private fun filledPath(color: Long) =
        VPath("M0,0h24v24h-24z", color, 1f, null, 1f, 0f, "butt", "miter", 4f, "nonZero")

    @Test
    fun `category colliding with a top-level group fails fast (H1)`(@TempDir tmp: Path) {
        val image = VectorImage(24f, 24f, 24f, 24f, listOf(filledPath(0xFF000000L)))
        val ex = assertThrows(IllegalStateException::class.java) {
            IconfyCodegen.generate(
                "com.test.icons", "Iconfy",
                listOf(
                    IconEntry("", "mdi", "Nav", "home", "", image),      // top-level object Nav
                    IconEntry("Nav", "mdi", "", "settings", "", image),  // category Nav → collision
                ),
                tmp.toFile(),
            )
        }
        assertTrue(ex.message!!.contains("collision"), ex.message)
    }

    @Test
    fun `path-unsafe set prefix or icon name is rejected (M2)`() {
        assertThrows(IllegalArgumentException::class.java) {
            IconfyExtension.encode("", "mdi", "", "../../etc/passwd", "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            IconfyExtension.encode("", "../evil", "", "home", "")
        }
    }

    @Test
    fun `generates nested objects with lazy accessors`(@TempDir tmp: Path) {
        val out = tmp.toFile()
        val image = VectorImage(24f, 24f, 24f, 24f, listOf(filledPath(0xFF112233L)))

        IconfyCodegen.generate(
            packageName = "com.test.icons",
            accessorName = "Iconfy",
            icons = listOf(IconEntry("", "mdi", "", "home", "", image)),
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
                IconEntry("", "mdi", "", "home", "", image),
                IconEntry("", "tabler", "", "home", "", image),
            ),
            outDir = out,
        )

        val src = File(out, "com/test/icons/Iconfy.kt").readText()
        assertTrue(src.contains("object Mdi"))
        assertTrue(src.contains("object Tabler"))
    }

    @Test
    fun `named overrides the accessor name`(@TempDir tmp: Path) {
        val out = tmp.toFile()
        val image = VectorImage(24f, 24f, 24f, 24f, listOf(filledPath(0xFF000000L)))

        IconfyCodegen.generate(
            packageName = "com.test.icons",
            accessorName = "Iconfy",
            icons = listOf(IconEntry("", "lucide", "", "chart-bar", "Chart", image)),
            outDir = out,
        )

        val src = File(out, "com/test/icons/Iconfy.kt").readText()
        assertTrue(src.contains("val Chart: ImageVector"), src)       // custom accessor name
        assertTrue(src.contains("addPathNodes"), src)
        assertTrue(!src.contains("val ChartBar"), src)                // default name not used
    }

    @Test
    fun `category nests prefix under a named object`(@TempDir tmp: Path) {
        val out = tmp.toFile()
        val image = VectorImage(24f, 24f, 24f, 24f, listOf(filledPath(0xFF000000L)))

        IconfyCodegen.generate(
            packageName = "com.test.icons",
            accessorName = "Iconfy",
            icons = listOf(
                IconEntry("", "mdi", "", "home", "", image),               // Iconfy.Mdi.Home
                IconEntry("Dashboard", "mdi", "", "home", "", image),      // Iconfy.Dashboard.Mdi.Home
                IconEntry("Dashboard", "tabler", "", "settings", "", image), // Iconfy.Dashboard.Tabler.Settings
            ),
            outDir = out,
        )

        val src = File(out, "com/test/icons/Iconfy.kt").readText()
        // Category object wraps prefix objects.
        assertTrue(src.contains("object Dashboard"), src)
        assertTrue(src.contains("Iconfy.Dashboard.Mdi.Home"), src)
        assertTrue(src.contains("Iconfy.Dashboard.Tabler.Settings"), src)
        // Top-level accessor still present alongside the categorized one.
        assertTrue(src.contains("Iconfy.Mdi.Home"), src)
    }

    @Test
    fun `prefix display renames the middle segment`(@TempDir tmp: Path) {
        val out = tmp.toFile()
        val image = VectorImage(24f, 24f, 24f, 24f, listOf(filledPath(0xFF000000L)))

        IconfyCodegen.generate(
            packageName = "com.test.icons",
            accessorName = "Iconfy",
            icons = listOf(IconEntry("Dashboard", "mdi", "Nav", "home", "", image)),
            outDir = out,
        )

        val src = File(out, "com/test/icons/Iconfy.kt").readText()
        assertTrue(src.contains("Iconfy.Dashboard.Nav.Home"), src)   // renamed middle
        assertTrue(!src.contains("object Mdi"), src)                 // default Mdi not used
    }
}
