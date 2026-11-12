# Installing and updating Deaftly

Deaftly is not on Google Play. You download it from this project's GitHub
Releases page and install it yourself. This guide takes you through it.
It needs Android 8 or newer.

## Install

1. On your phone, open the
   [Releases page](https://github.com/oneloneraven/deaftly/releases/latest).
2. Under **Assets**, tap the file ending in `.apk`, for example
   `deaftly-1.0.0.apk`. Your phone may warn that this type of file can harm
   your device. That warning appears for every app installed this way. Tap
   **Download anyway** if you are happy to continue.
3. When the download finishes, tap **Open**, or open the file from your
   Downloads folder.
4. Android will say it can't install apps from this source. Tap
   **Settings**, then switch on **Allow from this source** for the app you
   used to download the file, such as Chrome or Files. Go back and tap
   **Install**.
5. When it says the app is installed, tap **Open**.

The wording and menu names differ between phone makers. If you can't see the
option, open Settings and search for "Install unknown apps".

Once the app is installed you can switch **Allow from this source** off again.
Doing so doesn't affect Deaftly.

## Check it is genuine

Only install Deaftly from the Releases page above. Copies from anywhere else
may be fakes.

Every official Deaftly release is signed with the same key. Its fingerprint
(a SHA-256 certificate fingerprint) is:

```text
CD:70:A2:F4:8E:E6:1F:8D:90:31:91:CE:BF:D8:D0:D7:2B:D2:57:C9:11:D8:DC:10:0F:A6:FE:AD:F7:6A:53:EB
```

If you are comfortable with developer tools, you can check an APK against it
with `apksigner verify --print-certs deaftly-1.0.0.apk` from the Android SDK
build tools. The certificate SHA-256 digest it prints must match the
fingerprint above.

## Update

Deaftly does not update itself. When a new version is released:

1. Download the new `.apk` from the Releases page, as above.
2. Open it and tap **Update** or **Install**.

The new version installs over the old one. Your templates and settings
are kept.

If Android says the app can't be installed because it conflicts with an
existing package, or that the signatures don't match, stop. It means the file
was signed with a different key from the app you already have, so it did not
come from the Deaftly project. Don't uninstall your copy to get around it.
Download the file again from the Releases page.

## What Deaftly does not do

Deaftly doesn't check for updates or tell you when a new version is out.
Check the Releases page, or What's new on deaftly.com, now and then.
