package dev.iconfy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NamesTest {

    @Test
    fun `kebab and snake to PascalCase`() {
        assertEquals("Home", Names.pascal("home"))
        assertEquals("ArrowLeft", Names.pascal("arrow-left"))
        assertEquals("AccountBox", Names.pascal("account_box"))
        assertEquals("GearSixFill", Names.pascal("gear-six-fill"))
        assertEquals("MaterialSymbols", Names.pascal("material-symbols"))
    }

    @Test
    fun `leading digit gets underscore`() {
        assertEquals("_1password", Names.pascal("1password"))
        assertEquals("_24Hours", Names.pascal("24-hours"))
    }

    @Test
    fun `locale-independent uppercasing (no Turkish dotless-i bug)`() {
        // 'i'.uppercaseChar() must be 'I', never 'İ', regardless of the JVM default locale.
        assertEquals("Iconfy", Names.pascal("iconfy"))
    }
}
