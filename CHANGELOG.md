# Changelog

All notable changes to this project are documented here. This project adheres to
[Semantic Versioning](https://semver.org/).

## [0.2.2] - 2026-08-28

### Added
- **`named` parameter** on `add(...)` to override the generated accessor name, e.g.
  `add("lucide:chart-bar", named = "Chart")` → `Iconfy.Lucide.Chart`.

## [0.2.1] - 2026-08-28

### Added
- **`category("Name") { … }` DSL** for semantic clusters that mix icon sets, generating an outer
  object: `Iconfy.Dashboard.Mdi.Home`, `Iconfy.Dashboard.Tabler.Settings`. Prefix grouping is kept
  inside the category, and the same icon can appear both top-level and in one or more categories.

## [0.2.0] - 2026-08-28

### Fixed
- **AGP 9 / built-in Kotlin compatibility.** Generated accessors failed to compile on AGP 9+
  (`Unresolved reference 'let'/'apply'/'addPath'`) because the `.kt` was registered under the
  variant's Java sources. The plugin now registers with `variant.sources.kotlin` when AGP owns the
  Kotlin compilation (no `kotlin-android` plugin), and falls back to Java sources on AGP 8.x +
  `kotlin-android`.

### Added
- Automated test suite: unit tests (`Names`, `SvgToImageVector`, `IconfyCodegen`) and Gradle TestKit
  functional tests (cache-first fetch, offline cache-miss error).
- GitHub Actions CI (build + test on push/PR) and tag-triggered publishing to the Gradle Plugin Portal.

### Changed
- `iconfyGenerate` is now Gradle configuration-cache compatible.

## [0.1.1] - 2026-08-27

### Fixed
- Corrected repository URLs (`website`/`vcsUrl`/POM) to the real GitHub account.

## [0.1.0] - 2026-08-27

### Added
- Initial release: `iconfy { }` DSL, build-time fetch + cache of Iconify icons, and generation of
  offline, type-safe `ImageVector` accessors (`Iconfy.Mdi.Home`) via an own Svg2Vector + KotlinPoet
  engine. Android, Jetpack Compose.
