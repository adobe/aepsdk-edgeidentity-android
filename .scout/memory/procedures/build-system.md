---
title: "Build System"
entity_type: procedure
confidence: 0.75
created: 2026-09-28T10:40:27Z
last_accessed: 2026-09-28T10:40:27Z
last_reinforced: 2026-09-28T10:40:27Z
access_count: 1
sources:
  - "Makefile"
  - "code/settings.gradle.kts"
  - "code/edgeidentity/build.gradle.kts"
tags: ["build", "workflow", "gradle", "android", "makefile"]
tier: procedures
source: inferred
---

# Build System

Primary build system: Gradle Kotlin DSL, with wrapper at `code/gradlew`; Gradle project includes `:edgeidentity` and `:app`. Common root Makefile targets: `make unit-test` runs `testPhoneDebugUnitTest`; `make lint` runs Spotless check and Checkstyle; `make format` applies Spotless; `make assemble-phone` builds the library. From repository root, the direct unit-test command is `cd code && ./gradlew :edgeidentity:testPhoneDebugUnitTest`. Android SDK/Gradle environment is required.
