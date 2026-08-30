# Iconfy

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.bilecen.iconfy)](https://plugins.gradle.org/plugin/io.github.bilecen.iconfy)
[![CI](https://github.com/Bilecen/composeiconfy/actions/workflows/ci.yml/badge.svg)](https://github.com/Bilecen/composeiconfy/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**🇬🇧 English** · [🇹🇷 Türkçe](README.tr.md)

Use any of [Iconify](https://iconify.design)'s 200,000+ icons (Material Design Icons, Tabler,
Lucide, Phosphor, Font Awesome, Carbon, …) in **Android Jetpack Compose** — declared in your Gradle
build, embedded at build time, fully **offline** afterward.

You pick an icon on [icon-sets.iconify.design](https://icon-sets.iconify.design/), add its
`prefix:name` to the `iconfy { }` block, and the Gradle plugin downloads it once, converts it into a
type-safe `ImageVector`, and generates a clean accessor:

```kotlin
Icon(
    imageVector = Iconfy.Mdi.Home,
    contentDescription = null,
    tint = MaterialTheme.colorScheme.primary,
)
```

No runtime network calls, no manual SVG copying, no 200k-icon dependency bloat — only the icons you
actually declare are embedded.

## Install

Apply the plugin in your app module and declare icons:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.github.bilecen.iconfy") version "0.2.6"
}

iconfy {
    packageName.set("com.example.app.icons")  // where the generated `Iconfy` object lives
    icons {
        add("mdi:home")
        add("tabler:user")
        prefix("lucide") {          // sugar for a single set
            add("heart")
            add("star")
        }
    }
}
```

The plugin depends on `com.android.tools:sdk-common` (build-time only), so make sure **`google()`**
is in your plugin-resolution repositories:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Generated code depends only on `androidx.compose.ui`, which your Compose app already has.

## Usage

Icons are exposed as lazily-built, memoized `ImageVector`s, grouped by set prefix. A single import
covers everything:

```kotlin
import com.example.app.icons.Iconfy

Icon(Iconfy.Mdi.Home, contentDescription = "Home")
Icon(Iconfy.Tabler.User, contentDescription = "Profile", tint = Color.Red)
Icon(Iconfy.Lucide.Heart, contentDescription = null)
```

Names are converted to PascalCase: `arrow-left` → `ArrowLeft`, `mdi:github` → `Iconfy.Mdi.Github`,
`material-symbols:settings` → `Iconfy.MaterialSymbols.Settings`. Nesting by prefix means the same
icon name in different sets never collides.

### Categories

Wrap declarations in `category("Name")` to add an outer semantic cluster that mixes icon sets. The
prefix grouping is kept inside, so you get `Iconfy.<Category>.<Prefix>.<Icon>`:

```kotlin
iconfy {
    icons { add("mdi:home") }              // → Iconfy.Mdi.Home
    category("Dashboard") {
        prefix("mdi") { add("home") }      // → Iconfy.Dashboard.Mdi.Home
        add("tabler:settings")             // → Iconfy.Dashboard.Tabler.Settings
    }
}
```

The same icon can appear both top-level and in any number of categories (it is downloaded once). IDE
autocomplete surfaces the whole tree as soon as you type `Iconfy.Dashboard.`.

Rename the middle prefix segment with `prefix("mdi", named = "Nav")` when `Mdi` reads awkwardly inside
a semantic category:

```kotlin
category("Dashboard") {
    prefix("mdi", named = "Nav") { add("home") }   // → Iconfy.Dashboard.Nav.Home
}
```

To pull icons from **different sets into one group** without repeating `prefix(...)` blocks, set the
middle group per-icon with `into` (the set comes from the coordinate):

```kotlin
icons {
    add("hugeicons:gpu",              named = "Gpu",    into = "Cards")
    add("clarity:hard-disk-line",     named = "Disk",   into = "Cards")
    add("iconoir:multi-mac-os-window", named = "OsType", into = "Cards")
}
// → Iconfy.Cards.Gpu, Iconfy.Cards.Disk, Iconfy.Cards.OsType
```

`into` also works **inside a `prefix(...)` block** — fix the set once and route each icon to its group:

```kotlin
prefix("hugeicons") {
    add("dashboard-square-02", named = "Dashboard", into = "Sidebar")   // → Iconfy.Sidebar.Dashboard
    add("gpu",                 named = "Gpu",       into = "Hardware")  // → Iconfy.Hardware.Gpu
}
```

### Custom accessor names

Pass `named` to rename the generated accessor (handy for long or awkward icon names):

```kotlin
icons {
    add("lucide:chart-bar", named = "Chart")   // → Iconfy.Lucide.Chart
    prefix("mdi") { add("home", named = "Main") }  // → Iconfy.Mdi.Main
}
```

### Tinting

Monochrome icons (`fill="currentColor"`, the vast majority) are emitted with an opaque black fill and
tint normally via the `Icon` composable's `tint`. For a genuinely multi-color icon, use
`Image(imageVector = …)` instead so its colors are preserved.

## Configuration

| Option | Default | Description |
|---|---|---|
| `packageName` | `iconfy.generated` | Package of the generated `Iconfy` object |
| `accessorName` | `Iconfy` | Root object name (`Iconfy.Mdi.Home`) |
| `apiUrl` | `https://api.iconify.design` | Override to use a self-hosted Iconify API |
| `failOnMissing` | `true` | Fail the build on an unknown icon (vs. warn & skip) |

## How it works

Two build-time tasks, both incremental:

1. **`iconfyFetch`** — batches one request per set to the Iconify API, wraps each icon's body into a
   self-contained SVG, and writes it to `<rootDir>/.iconfy/cache/<prefix>/<name>.svg`. Cache-first:
   an icon already on disk is never re-downloaded, so `--offline` builds do zero network I/O.
2. **`iconfyGenerate<Variant>`** — normalizes each SVG through Android's `Svg2Vector` (handling
   shapes, transforms and `currentColor`) and emits one Kotlin file of `ImageVector` accessors. The
   generated code stores the raw path data and lets Compose's `addPathNodes(...)` parse it at
   runtime. Default display size is normalized to 24dp so sets with different viewBoxes render
   consistently.

### Reproducible / offline CI

The cache lives at `.iconfy/cache/` **outside** `build/`, so `clean` never forces a re-download.
**Commit `.iconfy/cache/`** to get fully network-free, reproducible builds on CI — or run once with
network, then build everywhere with `--offline`.

## Publishing (maintainers)

The plugin is a standalone build under `iconfy-gradle-plugin/`.

```bash
# Local testing
./gradlew -p iconfy-gradle-plugin publishToMavenLocal

# Gradle Plugin Portal (requires com.gradle.plugin-publish + credentials)
./gradlew -p iconfy-gradle-plugin publishPlugins
```

Update `website`/`vcsUrl` in `iconfy-gradle-plugin/build.gradle.kts` before publishing.

## Limitations

- Icons using SVG features VectorDrawable can't express (filters, embedded raster images, some
  masks) are skipped with a warning naming the icon.
- Multi-color icons should be used with `Image`, not tinted `Icon`.

## Requirements

Build with JDK 17–21 (Android Gradle Plugin does not yet support JDK 25). AGP 8.7+, Kotlin 2.0+.

## License

MIT — see [LICENSE](LICENSE).
