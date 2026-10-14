# FitEasy

[English](README.md) · [Русский](README.ru.md)

**Лицензия:** [PolyForm Noncommercial 1.0.0](LICENSE). Некоммерческое использование разрешено. Коммерческое использование — только по отдельному соглашению с vibic777.
**License:** [PolyForm Noncommercial 1.0.0](LICENSE). Noncommercial use is allowed. Commercial use requires a separate agreement with vibic777.


> A weight-loss diary that started with a request at home. The brief stayed soft. The interesting part is already visible.

**FitEasy** began as a personal request: a diary for losing weight. The specification was never pinned down, so the project grows from ideas that appear while it is being built.

What already looks worth keeping:

- an exercise editor anyone can shape
- routines a person assembles for themselves
- a library of movements that can keep growing
- exercises with animation

The aim is something pleasant to open every day. It is not there yet. Many details are still rough, some of the current result is not liked, and the work keeps catching on small things. This is an MVP being found by hand.

The first clients are Android and Windows. Linux may follow. Data, backups, and sync should stay compatible across clients.

## Project status

**Implementation in progress.** Android debug/release APK and Windows EXE/MSI have been built. These are intermediate builds; the agreed MVP is not complete. See [actual status](docs/MVP_STATUS.md).

## Build locally

Use JBR/JDK 25 and a configured Android SDK for `gradlew.bat :desktopApp:packageDelivery`: Android debug/release APK, managed Windows EXE/MSI and portable ZIP. Individual tasks remain available; JDK 21 can compile Android. Managed installers include the running-app dialog; ordinary packageMsi/packageExe remain standard. SDK path belongs in ignored local.properties; signing configuration belongs in ignored keystore.properties. The supplied debug keystore signs testing builds only.

Outputs: app/build/outputs/apk and desktopApp/build/managed-installer. Version 0.2.0 adds calendars, schedules, text/drawing/checklist notes, reminder controls and Windows instance/tray settings. Backup v2 / LAN v3 require updating both clients. Build outputs and all secrets are excluded from Git.

Windows portable ZIP is under desktopApp/build/portable. Extract the entire archive and run FitEasy.exe; runtime is bundled and portable data stays beside the app in data/. Version 0.2.1, build 3 adds portable mode. DPAPI secrets are still user/machine bound; cross-machine transfer uses .fiteasy.

Version, build number and date come from `version.properties`. `generateBuildInfo` creates the shared UI identity in ignored build output; Android and Windows packaging read the same metadata. Profile → Help provides offline search; Profile → About shows version/build/date/contributor. Profile → Language switches ten languages. Full translation coverage still needs work.

## Project documents

- [Product and screen map](docs/PRODUCT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Database and ER model](docs/DATABASE.md)
- [Sync protocol](docs/SYNC.md)
- [Backup format](docs/BACKUP_FORMAT.md)
- [Security and health data](docs/SECURITY.md)
- [Security review: findings and state](docs/SECURITY_REVIEW.md)
- [Windows implementation plan](docs/windows/FITEASY_WIN_PLAN.md)
- [Feature parity](docs/windows/FEATURE_PARITY.md)
- [Database compatibility](docs/windows/DATABASE_COMPAT.md)
- [Porting notes](docs/windows/PORTING_NOTES.md)
- [Design decisions](docs/DECISIONS.md)
- [Localization plan](docs/LOCALIZATION.md)
- [Notio-Android inspiration notes](docs/NOTIO_INSPIRATION.md)
- [Project TODO](TODO.md)
- [Contributor rules](RULES.md)

## Contributor

Development updates: [CHANGELOG](CHANGELOG.md), [current implementation status](docs/MVP_STATUS.md). Intermediate delivery files are kept outside Git under `dist/`; APK release signing is for test installation. Full agreed MVP readiness is tracked separately from successful builds.

Primary contributor: **vibic777**  
Git author for this repository: `vibic777 <vibic@yandex.ru>`

