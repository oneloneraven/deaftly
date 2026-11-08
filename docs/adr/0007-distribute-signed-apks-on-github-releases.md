# 7. Distribute signed APKs on GitHub Releases

## Status

Accepted

## Context

Most Android users install apps from Google Play. Publishing there needs a
paid developer registration with Google, a BDS pressure target, and an
alternative route exists. F-Droid isn't available: the project's licence is
non-commercial, so the app isn't free and open-source software.

## Decision

Each release attaches an APK signed with the project's upload key to its
GitHub Release. There is no Google Play listing for now. The decision is
revisited (issue #69) if installing from outside the Play Store proves a
barrier for users.

## Screening

GitHub (Microsoft, a BDS consumer-boycott target) is allowed under the
free-tool exception recorded at bootstrap. No payment goes to Google.

## Consequences

- Users must allow installing from outside the Play Store, which needs a
  clear install guide.
- No automatic updates: users update by installing a newer APK signed with
  the same key, so the upload key must never be lost.
- `targetSdk` still follows Google Play's requirement, so moving to Play
  later needs no rework.
