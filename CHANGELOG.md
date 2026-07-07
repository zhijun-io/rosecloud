# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Root `LICENSE` with Apache 2.0 terms.
- Root `CHANGELOG.md`.
- `test-api.sh` for monolith/microservice API verification with auto-detection.
- Shared `rosecloud-common.yaml` for base configuration reuse across services and monolith.

### Changed
- Updated service and monolith config imports to allow classpath and Nacos overrides.
- Simplified startup documentation to match the current `Taskfile.yml`.
- Added inline notes for `spring-boot-maven-plugin` exec packaging and monolith thin-jar dependency use.
