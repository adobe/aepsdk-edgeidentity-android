---
title: "Test Framework: Android Unit Tests"
entity_type: pattern
confidence: 0.70
created: 2026-09-28T10:40:27Z
last_accessed: 2026-09-28T10:40:27Z
last_reinforced: 2026-09-28T10:40:27Z
access_count: 1
sources:
  - "code/edgeidentity/src/test/java/com/adobe/marketing/mobile/edge/identity/IdentityStateTests.java"
  - "code/edgeidentity/src/test/java/com/adobe/marketing/mobile/edge/identity/IdentityExtensionTests.java"
tags: ["testing", "unit-tests", "android", "gradle", "junit", "mockito"]
tier: procedures
source: inferred
---

# Test Framework: Android Unit Tests

Scope: JVM unit tests for Edge Identity API event construction, extension listener/configuration behavior, state transitions, models, and storage helpers. Tests are under `code/edgeidentity/src/test/java`; functional Android tests live separately under `src/androidTest`. Run the unit suite with `make unit-test` or `cd code && ./gradlew :edgeidentity:testPhoneDebugUnitTest`. Use unit tests for isolated state/event logic and connected functional tests for integration with the Android runtime.
