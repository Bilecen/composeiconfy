package dev.iconfy

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * `iconfy { }` DSL extension.
 *
 * Declare Iconify icons as `"prefix:name"` coordinates (e.g. `"mdi:home"`). At build time the
 * plugin downloads each icon from the Iconify API, caches it, and generates type-safe
 * [androidx.compose.ui.graphics.vector.ImageVector] accessors grouped by prefix
 * (e.g. `Iconfy.Mdi.Home`).
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

    /** Resolved set of `"prefix:name"` coordinates. Populated via [icons]. */
    val iconSpecs: SetProperty<String> = objects.setProperty(String::class.java)

    /** Declare icons: `icons { add("mdi:home"); prefix("lucide") { add("heart") } }`. */
    fun icons(action: Action<IconsScope>) {
        action.execute(IconsScope(iconSpecs))
    }

    /** Scope for [icons]. */
    class IconsScope(private val specs: SetProperty<String>) {
        /** Add a full `"prefix:name"` coordinate. */
        fun add(coordinate: String) {
            specs.add(normalize(coordinate))
        }

        /** Add several full coordinates at once. */
        fun add(vararg coordinates: String) {
            coordinates.forEach { add(it) }
        }

        /** Sugar: `prefix("lucide") { add("heart"); add("star") }`. */
        fun prefix(prefix: String, action: Action<PrefixScope>) {
            action.execute(PrefixScope(prefix.trim(), specs))
        }
    }

    /** Scope bound to a single icon-set prefix. */
    class PrefixScope(private val prefix: String, private val specs: SetProperty<String>) {
        /** Add an icon name within the enclosing prefix. */
        fun add(name: String) {
            specs.add(normalize("$prefix:${name.trim()}"))
        }

        /** Add several icon names within the enclosing prefix. */
        fun add(vararg names: String) {
            names.forEach { add(it) }
        }
    }

    companion object {
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
