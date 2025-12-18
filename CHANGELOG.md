4.5.0 - 2025-12-19
==================

This release is mostly about fixing compatibility issues with IntelliJ IDEA 2025.3 and dropping legacy features that are no longer widely used.

### Breaking changes

- Drop support for TopCoder (#16).
- Remove legacy problem parsers in favor of Competitive Companion problem parsers (#13, #21, #22).
- Drop the legacy "Parse Contest" feature in favor of Competitive Companion contest parsers (#21).
- Remove the startup check for the legacy CHelper browser extension (#23).
- Remove integer overflow detection (#15).

### Fixes

Most are compatibility fixes for IntelliJ IDEA 2025.3.

- Fix a bug adding duplicate "Build Project" before-launch step to Task run configurations (#5).
- Fix a crash at start-up due to a `PluginException` due to not overriding the getId method of ConfigurationFactory (#10).
- Fix "Could not find or load class errors when running tests" (#11).

### Refactor

- Migrated legacy components `CHelperMain`, `AutoSwitcher`, and `ChromeParser` to the more modern Service architecture.
- Replace and remove a number of deprecated APIs.
- Remove `SSLUtils` and dead code related to legacy parsers.

### Build

- Upgrade Java version for building the plugin to Java 21.
- Upgrade target IntelliJ IDEA version for building the plugin to `2025.2` (#17).
- Upgrade Gradle to `v8.14.3` (#9).
- Upgrade IntelliJ Gradle Plugin from `v1` to `v2.10.5` (#9).
- Upgrade dependencies to newer versions (#30).
- Set up Github Actions for building the plugin (#14).
