# 14. Distribute through Google Play and GitHub Releases

## Status

Accepted

Supersedes [ADR-0007](0007-distribute-signed-apks-on-github-releases.md).

## Context

[ADR-0007](0007-distribute-signed-apks-on-github-releases.md) shipped Deaftly only as a signed APK on GitHub Releases, so no payment went to Google, a BDS pressure target. It was to be revisited if installing from outside the Play Store proved a barrier.

It does. Most Android users won't sideload an app, and deaf users testing the MVP (#56) need to install it the ordinary way. The owner decided on 11 November 2026 to publish on Google Play (website decision D2), superseding decision D8.

Google Play requires Android App Bundles and Play App Signing for new apps. Google then signs the APKs that Play delivers.

## Decision

- **Both channels.** Publish on Google Play, and keep attaching the signed APK to each GitHub Release for people without Google Play.
- **One app signing key.** Give Play the project's existing signing key (the one that signs the GitHub APK), encrypted with Google's PEPK tool, instead of letting Google generate one (decision D21). APKs from Play and from GitHub then share one signature, so people can move between the two channels without uninstalling.
- **A separate upload key.** A new key signs only the bundles uploaded to Play. Google can reset it if it's lost or leaked.
- **CI uploads to internal testing.** For each release, `release-artefacts.yml` builds the bundle, attaches it to the GitHub Release and uploads it to Play's internal testing track with a Play service account (decision D22). The owner promotes builds to closed testing and production in Play Console.
- **Uploader.** `r0adkll/upload-google-play`, pinned to a commit.

## Screening

Google is a BDS pressure target and a No Tech for Apartheid target. The free-tool exception in [ADR-0002](0002-screen-dependencies-against-bds-boycott-lists.md) doesn't cover Play, because registration costs a one-off fee. The owner accepts that fee as the cost of reaching users who won't sideload. Nothing else is paid, and the app stays free.

`r0adkll/upload-google-play` (MIT, an individual maintainer, 1,000+ stars) has no boycott-list associations. It runs only in the release job, and is pinned to a commit.

## Consequences

- Google holds a copy of the app signing key and could sign an update that installed copies accept. That's a new trust placed in Google, recorded in the [threat model](../threat-model.md).
- Handing the key to Play can't be undone. If Google's copy is ever compromised, the only route is Play's key upgrade, which leaves GitHub installs on the old key.
- The first bundle has to be uploaded by hand in Play Console, because the Play API can't create an app. CI uploads only once the owner turns them on. See the [Google Play runbook](../runbooks/google-play.md).
- Play Console shows the owner aggregate install numbers and Android vitals: crash and freeze reports from users who chose to share usage and diagnostics with developers. The app itself still sends nothing. This is recorded in the [record of processing](../data-protection.md).
- The app bundle has to stay within Play's size limit for the base module. Today's release APK is 140 MB, within the limit.
