# Architecture decision records

Significant, hard-to-reverse decisions, one per file, in
[Nygard's format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions):
Title, Status, Context, Decision, Consequences. Numbered `NNNN-title.md`.

An accepted ADR is never edited. A changed decision gets a new ADR, and the
old one's status becomes "Superseded by ADR-NNNN".

| ADR | Decision |
|---|---|
| 0001 | Record architecture decisions |
| 0002 | Screen dependencies and vendors against BDS boycott lists |
| 0003 | Build a native Android app with Kotlin and Jetpack Compose |
| 0005 | Store templates and settings with DataStore and JSON |
| 0006 | Wire dependencies by hand |
| 0007 | Distribute signed APKs on GitHub Releases (superseded by 0014) |
| 0008 | Allow Android backup of templates and settings |
| 0009 | Ship the MVP on 0.x |
| 0010 | Recognise text with PaddleOCR on ncnn |
| 0011 | Review permissions against an allowlist |
| 0012 | Back up kept chats |
| 0013 | Transcribe speech with sherpa-ncnn, bundled in the app |
| 0014 | Distribute through Google Play and GitHub Releases |

Every ADR is kept in the private vault, at `oneloneraven-vault/deaftly/adr/`. This index lists their numbers and titles only.
