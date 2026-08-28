package dev.iconfy

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * `iconfy { }` DSL extension.
 *
 * Declare Iconify icons as `"prefix:name"` coordinates. By default they are grouped by set prefix
 * (`Iconfy.Mdi.Home`). Wrap declarations in [category] to add an outer semantic group that mixes
 * sets, e.g. `Iconfy.Dashboard.Mdi.Home` and `Iconfy.Dashboard.Tabler.Settings`.
 */
abstract class IconfyExtension @Inject constructor(objects: ObjectFactory) {

    /** Iconify API base URL. Override to point at a self-hosted instance. */
    val apiUrl: Property<String> =
        objects.property(String::class.java).convention("https://api.iconify.design")

    /** Package the generated accessor code lives in. */
    val packageName: Property<String> =
        objects.property(String::class.java).convention("iconfy.generated")

    /** Name of the root accessor object (`Iconfy` → `Iconfy.Mdi.Home`). */
    val accessorName: Property<String> =
        objects.property(String::class.java).convention("Iconfy")

    /** Fail the build when a requested icon is missing/unavailable; otherwise warn and skip. */
    val failOnMissing: Property<Boolean> =
        objects.property(Boolean::class.java).convention(true)

    /** Encoded placements `"category<SEP>prefix<SEP>name"` (empty category = top level). */
    val placements: SetProperty<String> = objects.setProperty(String::class.java)

    /** Declare icons grouped by their Iconify set prefix: `Iconfy.Mdi.Home`. */
    fun icons(action: Action<IconScope>) {
        action.execute(IconScope("", placements))
    }

    /** Declare icons under a named category: `category("Dashboard") { … }` → `Iconfy.Dashboard.Mdi.Home`. */
    fun category(name: String, action: Action<IconScope>) {
        action.execute(IconScope(name.trim(), placements))
    }

    /** Scope for [icons] / [category]; the same API, optionally tagged with a category. */
    class IconScope(private val category: String, private val placements: SetProperty<String>) {
        /**
         * Add a full `"prefix:name"` coordinate. Pass [named] to override the generated accessor
         * name, e.g. `add("lucide:chart-bar", named = "Chart")` → `…Lucide.Chart`.
         */
        @JvmOverloads
        fun add(coordinate: String, named: String = "") {
            val c = normalize(coordinate)
            placements.add(encode(category, c.substringBefore(':'), "", c.substringAfter(':'), named.trim()))
        }

        /**
         * Sugar for one set: `prefix("mdi") { add("home") }`. Pass [named] to rename the prefix
         * segment, e.g. `prefix("mdi", named = "Nav")` → `…Dashboard.Nav.Home`.
         */
        fun prefix(prefix: String, named: String = "", action: Action<PrefixScope>) {
            action.execute(PrefixScope(category, prefix.trim(), named.trim(), placements))
        }
    }

    /** Scope bound to a single icon-set prefix within [IconScope]. */
    class PrefixScope(
        private val category: String,
        private val prefix: String,
        private val prefixDisplay: String,
        private val placements: SetProperty<String>,
    ) {
        /** Add an icon name within the enclosing prefix; [named] overrides the accessor name. */
        @JvmOverloads
        fun add(name: String, named: String = "") {
            placements.add(encode(category, prefix, prefixDisplay, name.trim(), named.trim()))
        }
    }

    /** A decoded placement: where an icon goes and what to call its accessor. */
    data class Placement(
        val category: String,
        val prefix: String,
        /** Optional override for the prefix segment's object name (blank = derive from [prefix]). */
        val prefixDisplay: String,
        val name: String,
        val display: String,
    )

    companion object {
        private const val SEP = ""

        /** Encode a placement into a single Gradle-input-friendly string. */
        fun encode(
            category: String,
            prefix: String,
            prefixDisplay: String,
            name: String,
            display: String,
        ): String = listOf(category, prefix, prefixDisplay, name, display).joinToString(SEP)

        /** Decode a placement string. */
        fun decode(spec: String): Placement {
            val p = spec.split(SEP)
            return Placement(
                p[0],
                p.getOrElse(1) { "" },
                p.getOrElse(2) { "" },
                p.getOrElse(3) { "" },
                p.getOrElse(4) { "" },
            )
        }

        /** Validate and canonicalize a `"prefix:name"` coordinate. */
        fun normalize(coordinate: String): String {
            val c = coordinate.trim()
            val idx = c.indexOf(':')
            require(idx > 0 && idx < c.length - 1) {
                "iconfy: icon coordinate must be 'prefix:name', got '$coordinate'"
            }
            return c
        }
    }
}
