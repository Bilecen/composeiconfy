# Changelog

All notable changes to this project are documented here. This project adheres to
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
