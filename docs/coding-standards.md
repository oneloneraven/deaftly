# Coding standards

The standard this project's code, and every code review, is held to. Part 1
is how to review. Part 2 is the decisions already made here, so they aren't
silently reversed.

## Part 1: Review framework

### Personas

Adopt all of them at once; each surfaces issues in its own domain.

- **Platform specialist**: whether the code is idiomatic for the platform
  it runs on, its runtime limits, and its failure modes. Each stack section
  in Part 2 says what this persona checks.
- **Architecture generalist**: structure, data flow, the client-server
  boundary, API design (semantics, status codes, error shapes,
  idempotency), coupling between modules, and whether it will scale as
  features are added.
- **Software engineering principles**: single responsibility, unnecessary
  abstraction or premature generalisation, duplication that should be
  extracted (or extraction that's over-engineered for one caller),
  speculative code for requirements that don't exist, and cleverness where
  a simpler solution was available. SOLID, KISS, DRY, YAGNI,
  Tell-Don't-Ask, high cohesion and low coupling.
- **Quality and operations**: test coverage of what's actually risky,
  error handling (network failures, malformed input, races), validation at
  trust boundaries, secrets and whether anything sensitive could leak into
  a bundle or git history, deploy and rollback safety, and observability.
- **UI and UX**, where there's a UI: accessibility, responsive behaviour,
  loading, empty and error states, feedback for async actions, and visual
  consistency.

### Process

- Read the actual code. Don't infer from file names or assume best practice
  was followed.
- For each finding, cite the file and line, explain why it's a problem, and
  state the concrete risk if it's left.
- Severity:
  - **Blocking**: will break in production, or is insecure.
  - **Should-fix**: a real problem, not urgent.
  - **Worth considering**: opinion, lower confidence.
- Don't pad. If a persona has nothing significant, say so briefly.
- Where a fix isn't obvious, propose one concretely.
- Verify platform behaviour empirically where practical rather than trusting
  documentation alone.

### Output

Group findings by severity, then by persona. End with the top five things
to fix first and a short paragraph on what's done well.

## Part 2: Project standards

### Engineering

- Hold work to real rigour: the principles above applied while building,
  not just while reviewing. When the honest scope is bigger than the
  cheapest version, say so and let the owner choose.
- Validate all input at trust boundaries on the server. Client-side
  validation is never a trust boundary.
- Never trust an identity or owner id supplied in a request body; derive it
  from the authenticated session.
- Let errors reach the boundary. Don't catch an error just to tidy it up;
  the boundary logs it once and returns a reference the user can quote.
- Use timing-safe comparison for any secret, token or signature, and rate
  limit the endpoint that checks it.

### Logging

- Use a structured logger, not raw console output: one JSON object per
  event with request id, route, environment and release version.
- Name events as stable dotted names (`email.failed`); put details in
  fields.
- Levels: `error` needs looking at, `warn` a handled failure, `info` a
  notable step that worked, `debug` for investigation only.
- The logger passes only allowlisted fields and redacts the rest. Never log
  a body, an email address, a password, token or cookie, or user content.
- Identify a person by internal id only.

### Testing

- Test at the right layer: unit tests for logic, integration tests against
  the real runtime, end-to-end tests against the real application. Mock only
  states the real backend can't produce.
- A failing test is a bad test or a real bug. Never dismiss one as flaky.
- For anything user-facing, verify in a real browser, not just by reading
  the code.

### Accessibility

Where there's a UI, every page meets WCAG 2.2 AA, checked automatically and
by hand at phone and desktop widths in light and dark themes.

### Comments and docs

- Code documents itself. The default is no comment.
- A comment is one line, for a "why" the code can't express: a platform
  workaround, a subtle invariant, a surprising behaviour. Never what the code
  does, and never how it got here.
- Anything longer goes in `docs/` or an ADR; history goes in commits and PRs.
- No sidecar notes files.
- A comment that no longer matches the code is a bug: fix or delete it in
  the same change. Cut any narrative comment you touch.
- Nothing users download contains comments.
- Docs describe the current state and link to ADRs for the reasons. Update
  them in the same PR as the change that makes them wrong.

### Security

- Keep [the threat model](threat-model.md) current. A change that adds or
  moves a trust boundary updates it in the same PR: a new host, an auth or
  session change, data exposed beyond its owner, a new processor, a new
  secret or CI permission.
- Secrets never enter git. Local secrets files are gitignored and treated
  as real.
- CI workflows declare least-privilege permissions and pin actions by commit
  SHA.
- Every job's `runs-on` is an exact image (`ubuntu-26.04`, `macos-26`), never
  a `-latest` label. Dependabot doesn't move it: when GitHub moves a
  `-latest` label to a new image, move every pin to that image by hand in
  one PR.

### Android toolchain

Platform persona for this stack: the activity and process lifecycle,
configuration changes and state restoration, what the app exposes to other
apps, what leaves the device, accessibility across font scales and themes,
and the APK's size and startup.

- **One entry point.** `./gradlew check` runs Spotless (ktlint), detekt,
  Android Lint, the unit and Robolectric tests, the Roborazzi screenshot
  comparison and a release build with R8. CI runs exactly that. Run it before
  pushing.
- **Versions live in `gradle/libs.versions.toml`**, and the Gradle wrapper
  pins its distribution by checksum. Dependabot updates both. Build scripts
  never hard-code a version, and every repository is declared once in
  `settings.gradle.kts`.
- **JDK 21 toolchain.** Gradle compiles with a JDK 21 toolchain whatever JDK
  runs it, so CI and development produce the same bytecode.
- **Two modules.** `:core` is pure Kotlin with no Android dependency: put
  logic there and test it with plain unit tests. `:app` holds the Android
  and Compose code. `:core` uses explicit API mode.
- **Format with ktlint through Spotless** (`./gradlew spotlessApply`), in
  the `ktlint_official` style. The ktlint rules are set in the root
  `build.gradle.kts`, because Spotless doesn't read `.editorconfig` for them.
  detekt runs with its default rules plus `config/detekt/detekt.yml`.
  Compiler warnings are errors.
- **Lint warnings are errors**, and the accessibility and security checks
  are errors in their own right. The checks that only say a newer version
  exists are off, because a new upstream release must not fail an unrelated
  PR. Don't suppress a lint issue without a comment saying why.
- **Test UI with Robolectric and Compose's test APIs** on the JVM, with
  accessibility checks on (`enableAccessibilityChecks()`). Screenshot tests
  use Roborazzi; the reference images live in `app/src/test/screenshots`.
  When a UI change is intended, run `./gradlew recordRoborazziDebug`, look at
  every changed image, and commit them with the change. Screenshot the light
  and dark themes and a large font scale.
- **Nothing leaves the device unless a decision says so.** The template has
  no `INTERNET` permission, and adding one (or any other permission) needs an
  ADR and an update to the threat model and record of processing. Backups
  and device transfer include only the template store, through explicit rules
  ([ADR-0008](adr/0008-allow-android-backup-of-templates-and-settings.md)).
- **Never log user content**, at any level, in any build type. Log events
  and error types, not what the user wrote, said or captured.
- **Export nothing but the launcher activity.** Every other component sets
  `android:exported="false"`; a library component that would be exported is
  removed in the manifest with `tools:node="remove"`. Check the merged
  release manifest when a dependency changes.
- **Release builds** are minified and shrunk with R8 and signed with the
  upload key from the `release` environment's secrets. `versionName` is the
  release tag without its `v` and `versionCode` is
  `major × 1,000,000 + minor × 1,000 + patch`, so each part stays below
  1,000. Nothing reads a version from a file; local builds are `0.0.0`. A
  keystore never enters the repository.
- **Verifying on Android** replaces "drive it in a browser": run the change
  on an emulator (or a device), in the light and dark themes and at the
  largest font scale, with TalkBack on for anything interactive.
- **detekt 1.23** is the current stable line. It uses Gradle APIs that
  Gradle 10 removes, so move to detekt 2 once it's stable.
