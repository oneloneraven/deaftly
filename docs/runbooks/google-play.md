# Google Play

How Deaftly gets onto Google Play and how releases reach it ([ADR-0014](../adr/0014-distribute-through-google-play-and-github-releases.md)). The one-time setup is for the owner: it needs the Play Console account, the signing key and Google Cloud.

## How releases flow

1. A release tag runs `release-artefacts.yml`. It attaches the signed APK to the GitHub Release, as before.
2. If the Play upload key secrets are set, it also builds `deaftly-X.Y.Z.aab`, signed with the upload key, and keeps it as the `play-bundle` workflow artefact for 30 days.
3. If `PLAY_UPLOADS_ENABLED` is `true`, it uploads the bundle to the **internal testing** track as a completed release named after the tag.
4. The owner promotes a build from internal testing to closed testing, or to production, in Play Console.

### Reviews

- **Internal testing:** updates are generally not reviewed before they're available, though Google may review them afterwards. The app's very first release is the exception: it's reviewed even on internal testing.
- **Closed testing, open testing and production:** every release is reviewed. That usually takes from a few hours to seven days, and occasionally longer.
- **Managed publishing**, under Publishing overview, holds reviewed changes until you choose to publish them. It doesn't apply to internal testing.

Promote a build only once it has been tried on internal testing, and allow up to a week for review before any date it's needed, such as a user testing session (#56).

## One-time setup

### 1. Create the app

In Play Console, create the app with the package name `com.ravendarque.deaftly`, as a free app.

### 2. Give Play the app signing key (D21)

Under Test and release > App integrity > App signing, choose to use your own key, then **Export and upload a key from Java keystore**. Follow Play's instructions to run its PEPK tool against the existing release keystore, the one whose fingerprint is in [install.md](../install.md), and upload the encrypted output.

This can't be undone. Afterwards, check that the app signing certificate's SHA-256 fingerprint in Play Console matches the one in `install.md`.

### 3. Make the upload key

Create a separate upload key and keep it somewhere safe, apart from the app signing key:

```bash
keytool -genkeypair -v -keystore play-upload.jks -alias play-upload -keyalg RSA -keysize 4096 -validity 10000
```

Register its certificate in Play Console as the upload key when prompted. Export the certificate with `keytool -export -rfc -keystore play-upload.jks -alias play-upload -file upload_certificate.pem`.

Add these secrets to the repository's `release` environment:

| Secret | Value |
|---|---|
| `PLAY_UPLOAD_KEYSTORE_BASE64` | `base64 -w0 play-upload.jks` |
| `PLAY_UPLOAD_KEYSTORE_PASSWORD` | The keystore password |
| `PLAY_UPLOAD_KEY_ALIAS` | `play-upload` |
| `PLAY_UPLOAD_KEY_PASSWORD` | The key password |

### 4. Upload the first bundle by hand

The Play API can't create an app's first release, so this one is manual:

1. Cut a release, or re-run `release-artefacts.yml` for the latest tag.
2. Download the `play-bundle` artefact from that run.
3. Upload it to internal testing in Play Console, and add yourself as a tester.

Before Play accepts a release, Play Console asks for the app's details: the store listing, content rating, target audience, app access, ads, and the Data safety form. Apps only on internal testing don't need the Data safety form, but closed testing and production do.

### Data safety answers

Everything the app does stays on the device, except a feedback or issue report the user writes and sends ([privacy](../privacy.md#feedback-from-the-app)). Play counts anything sent off the device as collected, even when the user chooses to send it, so the form declares the report:

| Data type | Collected | Optional | Purposes |
|---|---|---|---|
| App activity > Other user-generated content (the message) | Yes | Yes | App functionality, Analytics |
| App info and performance > Other app performance data (the app version, sent with the message) | Yes | Yes | Analytics |
| Personal info > Email address | Yes | Yes | App functionality |
| Personal info > Name | Yes | Yes | App functionality, Advertising or marketing |

- **Optional:** the whole report is optional, since nothing is sent unless the user writes it and taps Send, and the app works without it.
- **Purposes:** the message and app version are used to understand and fix problems (Analytics) through the app's feedback feature (App functionality). The email address is only for replying. The name is only used to credit a quote on the website, which Play counts as marketing.
- **Shared:** no. The system that receives reports processes them on One Lone Raven's behalf, which Play doesn't count as sharing.
- **Encrypted in transit:** yes, over HTTPS.
- **Deletion requests:** yes, by emailing support@deaftly.com, as the website's privacy page says.
- **Privacy policy:** the website's privacy page.

Nothing else is collected: no device identifiers, location, contacts, photos, audio, crash logs or analytics. Camera pictures, microphone audio, templates and chats never leave the device.

### 5. Let CI upload (D22)

1. In Google Cloud, create a service account in a project used only for this, and create a JSON key for it.
2. In Play Console, under Users and permissions, invite the service account's email. Give it access to **this app only**, with permission to release to testing tracks, and nothing else.
3. Add the JSON key as the `PLAY_SERVICE_ACCOUNT_JSON` secret in the `release` environment, then delete the downloaded copy.
4. Set the repository variable `PLAY_UPLOADS_ENABLED` to `true`.

The next release uploads to internal testing by itself.

## When something goes wrong

- **Upload key lost or leaked:** request an upload key reset in Play Console, make a new key as in step 3, and replace the four secrets. The app signing key isn't affected.
- **Service account key leaked:** delete the key in Google Cloud, create a new one, and replace `PLAY_SERVICE_ACCOUNT_JSON`. Check Play Console's release history for uploads you didn't make.
- **Uploads failing:** set `PLAY_UPLOADS_ENABLED` to `false` to stop them. The APK on GitHub still ships, and the bundle is still kept as an artefact for a manual upload.
