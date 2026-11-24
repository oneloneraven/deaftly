# Google Play

How Deaftly gets onto Google Play and how releases reach it ([ADR-0014](../adr/0014-distribute-through-google-play-and-github-releases.md)). The one-time setup is for the owner: it needs the Play Console account, the signing key and Google Cloud.

## How releases flow

1. A release tag runs `release-artefacts.yml`. It attaches the signed APK to the GitHub Release, as before.
2. If the Play upload key secrets are set, it also builds `deaftly-X.Y.Z.aab`, signed with the upload key, and keeps it as the `play-bundle` workflow artefact for 30 days.
3. If `PLAY_UPLOADS_ENABLED` is `true`, it uploads the bundle to the **internal testing** track as a completed release named after the tag.
4. The owner promotes a build from internal testing to closed testing, or to production, in Play Console.

### Reviews

Sources are Google's Play Console Help pages: [managed publishing](https://support.google.com/googleplay/android-developer/answer/9859654), [publish your app](https://support.google.com/googleplay/android-developer/answer/9859751) and [testing](https://support.google.com/googleplay/android-developer/answer/9845334).

- **Internal testing:** "App updates on internal test tracks are not subject to reviews but may be subject to retroactive reviews after going live" (managed publishing).
- **The first release:** "If your app's first release roll-out is on an Internal test track, the submission must be reviewed before it can be published" (managed publishing). A rejected submission also means the next one is reviewed before it's published.
- **Other tracks:** "All app changes need to be processed before they can be published" (managed publishing), and only internal testing is named as exempt. The pages don't list closed testing, open testing and production by name.
- **How long:** "a few hours or up to seven days (or longer in exceptional cases)", and Google suggests "a buffer period of at least a week" before going live (managed publishing). Submitting another change while one is in review can push the app to the back of the queue.
- **Managed publishing**, under Publishing overview, lets you "decide when to publish approved changes". It can't be used for the first publication, and "Internal test tracks are not covered by Managed publishing" (managed publishing).

Promote a build only once it has been tried on internal testing, and allow at least a week for review before any date it's needed, such as a user testing session (#56).

## One-time setup

### 1. Create the app

In Play Console, create the app with the package name `com.oneloneraven.deaftly`, as a free app.

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

Before Play accepts a release, Play Console asks for the app's details: the store listing, content rating, target audience, app access, ads, and the Data safety form. Apps "exclusively active" on internal testing "do not need to complete the Data safety form", but apps on closed, open or production tracks must ([Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469)).

### Data safety answers

Everything the app does stays on the device, except a feedback or issue report the user writes and sends ([privacy](../privacy.md#feedback-from-the-app)). Play defines collecting as "transmitting data from your app off a user's device", and lists no exemption for data the user chooses to send, so the form declares the report ([Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469)):

| Data type | Collected | Optional | Purposes |
|---|---|---|---|
| App activity > Other user-generated content (the message) | Yes | Yes | App functionality, Analytics |
| App info and performance > Other app performance data (the app version, sent with the message; Play has no type for it, so this is our choice) | Yes | Yes | Analytics |
| Personal info > Email address | Yes | Yes | App functionality |
| Personal info > Name | Yes | Yes | App functionality |

- **Optional:** Play allows this "where a user chooses whether to manually provide that data type". The whole report is optional, since nothing is sent unless the user writes it and taps Send, and the app works without it.
- **Purposes:** the message and app version are used to understand and fix problems (Analytics) through the app's feedback feature (App functionality). The email address is only for replying. The name is only used to credit a quote on the website, if the user agrees to be quoted. Play's purposes cover how the app itself uses data, and its Advertising or marketing purpose is defined by ads, promotional notifications and advertising partners, none of which applies. The privacy page discloses the quoting.
- **Shared:** no. The system that receives reports processes them on One Lone Raven's behalf, and Play doesn't count transfers to "an entity that processes user data on behalf of the developer" as sharing.
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
