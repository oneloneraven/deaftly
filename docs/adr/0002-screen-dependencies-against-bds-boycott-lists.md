# 2. Screen dependencies and vendors against BDS boycott lists

## Status

Accepted

## Context

Who maintains a dependency or runs a vendor, and what that organisation
does, is part of this project's values, not just its supply-chain risk.

## Decision

Before adopting a dependency or vendor of any weight (a non-trivial runtime
package, a hosting or platform vendor, an identity provider, a third-party
API), check it against the BDS movement's
[consumer boycott priority targets](https://bdsmovement.net/Guide-to-BDS-Boycott)
and its
[No Tech for Oppression, Apartheid or Genocide campaign](https://bdsmovement.net/no-tech-oppression-apartheid-or-genocide).

Record the check (dependency, maintainer, on a list or not) in the PR, ADR
or doc that records the adoption.

A hit is surfaced and weighed explicitly against the alternatives and their
costs. It is never skipped.

**Exception:** a free, industry-standard tool with no viable alternative is
allowed, because using it pays nothing to the listed company. Record it
where it's adopted.

No product or brand on either list is named or recommended in this
project's copy.

## Consequences

- Every new dependency or vendor carries a visible screening record.
- A clearance is a point-in-time result. Re-check a vendor before
  substantially expanding the relationship.
- This is a recorded manual check, not an automated gate: there's no
  machine-readable list to check against.

## Screening record

Checked at bootstrap for the project's default dependencies and vendors.

| Dependency | Maintainer | On a list | Note |
|---|---|---|---|
| GitHub (repository, Actions, Dependabot, Projects) | Microsoft | Y | Free tier, industry standard, no viable alternative: allowed under the free-tool exception. Nothing is paid. |
| Android SDK, Android Gradle Plugin, Jetpack (AndroidX, Compose), Android Lint, Android Studio and emulator images | Google | Y | Google is a pressure target in the BDS boycott guide and a No Tech for Apartheid target (Project Nimbus). Free, industry standard, and there is no viable alternative for building a native Android app: allowed under the free-tool exception. Nothing is paid; there is no Google Play listing. |
| Robolectric | Google (open source, Robolectric project) | Y | MIT licence, maintained by Google engineers. Free, dev-only, and the only maintained way to run Android UI tests on the JVM: allowed under the free-tool exception. |
| Kotlin and the Kotlin Gradle plugins | JetBrains | N | Apache-2.0 licence. |
| Gradle and gradle/actions | Gradle Inc. | N | Apache-2.0 and MIT licences. CI uses setup-gradle's open-source basic cache provider, not the proprietary enhanced one. |
| Eclipse Temurin (JDK 21) | Eclipse Adoptium (Eclipse Foundation) | N | GPL-2.0 with Classpath Exception. |
| Roborazzi | takahirom (independent open source maintainer) | N | Apache-2.0 licence. Dev-only. |
| ktlint | ktlint project (community open source, started at Pinterest) | N | MIT licence. Dev-only. |
| detekt | detekt project (community open source) | N | Apache-2.0 licence. Dev-only. |
| Spotless | DiffPlug (open source) | N | Apache-2.0 licence. Dev-only. |
| JUnit 4 | JUnit team (community open source) | N | EPL-1.0 licence. Dev-only. |
