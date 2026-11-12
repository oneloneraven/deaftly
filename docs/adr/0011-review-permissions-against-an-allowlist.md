# 11. Review permissions against an allowlist

## Status

Accepted. Supersedes [ADR-0004](0004-the-app-has-no-network-access.md).

## Context

ADR-0004 banned network access outright and enforced it in CI. It was never
a product requirement. It ruled out features like a chat across two nearby
phones (#185), sending a chat to another Deaftly user (#184) and sync (#61)
before anyone had weighed them.

What is worth keeping is why the guard was built: a dependency can merge
permissions into the app without anyone noticing. ML Kit, for example,
merges `INTERNET` and `ACCESS_NETWORK_STATE` and uploads diagnostics.

## Decision

- **No blanket ban.** Whether a feature may use the network, or any other
  permission, is decided for that feature, in its own PR, with an update to
  the threat model, the record of processing and the privacy wording.
- **A checked-in allowlist.** `config/permissions-allowlist.txt` lists the
  permissions the release build may request. Today that's `CAMERA` and the
  signature permission AndroidX adds. `verifyReleaseManifest` fails the
  build if the merged release manifest asks for anything else, or exports a
  component other than the main activity.
- **Adding a permission means editing the allowlist** in the same PR as the
  feature that needs it, so the change is visible in review.
- **On-device processing that matters to users keeps a test of its own.**
  `OcrSendsNothingTest` checks that recognising text sends and receives
  nothing, which backs "Pictures never leave your phone".

## Consequences

- A library that merges a permission still fails the build. Remove the
  permission with `tools:node="remove"`, or add it to the allowlist with a
  reason.
- The app no longer tells users it has no internet access. The first-run
  screen says what's true today: templates and settings are kept on the phone.
- Features that need the network can now be proposed. Each one brings its own
  threat model and privacy update.
