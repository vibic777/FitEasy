# FitEasy Development Rules

## Custom colors and backgrounds

Theme names must not display age ranges or age labels. Keep the seven named styles available to everyone; style names describe the look or colour, never an age or gender.

New notes and note folders receive an editable localized name with the first available positive suffix: Folder 1/3/4 produces Folder 2. Names are suggestions; stable UUIDs remain entity identity.

Every configurable color must offer presets and a custom RGB color (R, G, B 0–255 and RRGGBB). Appearance supports seven distinct styles with icon treatment, shapes and borders. Background color and image can be applied to all sections or any checked subset, including Profile and Settings; preferences remain device-local and optional in backups. Images must be prepared off the UI thread and bounded in size. Do not write slider values on every pointer movement.

## Language of built-in content

Interface texts, built-in product and dish names and exercise texts follow the language chosen in the app; a new installation starts in the system language. When the app has no text in that language it shows English, never Russian or a mixture. Records typed by the person are never translated.

## Icons instead of buttons

Actions are icons, not text buttons, wherever an icon can say it: every icon carries a hint in the selected one of the ten languages — shown on long press on a touch screen and on pointer hover on an ordinary screen — and the same text as its screen-reader label. A drawing or scene editor keeps its canvas on screen without page scrolling; tools sit in a panel that can be moved to any of the four edges and hidden. Lists inside one category are flipped with previous / next arrows. The rule covers buttons and choice chips alike. Only values that are data themselves — units, weekdays, periods, language and style names, tags, device names, numbers and dates — stay as text.

Exception set by the owner on 2026-10-05: the pages that repeat Notio — Synchronization, Security and privacy, Backup — keep Notio's look: titled blocks, a line of explanation, and wide buttons with their names written on them. The settings menu itself keeps its icon rows.

## Hints and help are kept with the code

Set by the owner on 2026-10-10 (D120; the mechanics are D117 and D118, described in docs/HELP.md). Every new or changed function a person can see gets, in the same step, on Android and Windows and in all ten languages:

- a hint on every new icon and button (the section above);
- its place in the help: the text of its section says what it is and when to use it, and the icon is listed in that section with its drawing, its name and its meaning (HelpTree.kt);
- the way from the hint into the help: a press on the shown hint opens the section the icon belongs to;
- the contextual search: the help is searched by words and shows what was found in its surroundings (searchHelp), so the text of the section must contain the words a person would search the function by.

A function without a hint and without its help is not finished. The Linux journal gets its row as for any decision.

## Background work and the battery

Set by the owner on 2026-10-10 (D121): working in the background in the best way and not draining the battery comes first. The fourteen rules, each with the Android guide it comes from, are in docs/BATTERY.md: an alarm instead of a timer or a service that only stays alive; an inexact alarm wherever the minute does not matter; no setAlarmClock without need; few wake-ups (more than ten an hour is excessive); a wake lock only for as long as its work lasts; a foreground service only for what the person expects now; the search of the network and the announcement on it stopped when the app is not on the screen; the screen held no longer than needed; no request to be let out of battery optimisation; after a restart only the alarms are set again; a process woken by an alarm does only what it came for.

New code that works in the background, sets an alarm, holds a service, a wake lock, the network or the screen is checked against that table before it is written; a departure from it is recorded in docs/DECISIONS.md with its reason.

## Security review is kept in a file

Set by the owner on 2026-10-11 (D131): every conclusion about security is written into docs/SECURITY_REVIEW.md, and the file is kept up from then on. It holds the checklist ("Что проверять"), the state by area, the findings with their state (fixed with the version, left with the reason and whose decision it is, or open), the table of libraries with the way to repeat their check, what was not verified, and the log of reviews. The design of the protection stays in docs/SECURITY.md.

A change that touches permissions or the manifest, Android components and intents, the network, encryption, keys, the app lock or recovery, import or export of any file, a path or address that arrives with data, the journal, starting other programs, the installer, or a library is checked against that checklist before it is written, and its result is entered in the file in the same step. A new or updated library passes the library check first: known vulnerabilities, the published checksum, and the permissions and addresses of the built release APK. A finding is never deleted, only its state changes. Only what was actually verified is written as verified; secrets and real data never go into the file.

## 1. Contributor identity

Primary contributor: **vibic777**  
Repository Git author: `vibic777 <vibic@yandex.ru>`

Use the contributor handle exactly as written in contributor-facing project records. Git author identity is configured per repository; never set it globally for FitEasy work.

## 2. Cross-platform compatibility

FitEasy-Android and FitEasy-Win are the first planned clients; Linux is planned later. Every product feature, domain-model change, database change, backup change, and sync change MUST be evaluated for FitEasy-Win and the future Linux client.

Update the applicable files:

- `docs/windows/FITEASY_WIN_PLAN.md`
- `docs/windows/FEATURE_PARITY.md`
- `docs/windows/DATABASE_COMPAT.md`
- `docs/windows/PORTING_NOTES.md`
- `docs/linux/LINUX_PLAN.md` — always, since 2026-10-08: its section “Журнал для Linux: что повторить” gets a row for every decision in the same delivery, saying what is shared code and what the Linux client must do itself.

Android-specific implementation details must not enter shared data semantics, backup formats, or sync contracts unless there is a documented reason. Both clients must preserve the same meaning for shared records.

## 3. Health and wellness product behavior

- Support gradual, low-impact activity for ordinary fitness levels; do not make running or intense exercise a prerequisite.
- Explain that calorie, BMI, hydration, and activity values are estimates. Avoid medical claims and guaranteed outcomes.
- Do not prescribe weight-loss calorie deficits to minors. Do not apply adult BMI interpretation to minors.
- Goals are user-controlled and can be edited or disabled. Avoid shame, food moralizing, and punitive streak mechanics.

## 4. Data and database

- Database encryption is off by default and can be enabled/disabled in settings. App-lock recovery supports a local security question, configured SMTP email and TOTP authenticator; recovery credentials are device-local.

- The app is local-first and must retain core functionality offline.
- User-controlled database encryption is a core feature on every client; it can be enabled and disabled in settings. Choose platform-appropriate encrypted storage while preserving portable backup and sync semantics.
- The proposed Android local store is Room over SQLCipher. Confirm library, key lifecycle, and recovery design before implementation.
- Database migrations are explicit, ordered, and versioned. Never ship a breaking schema change without a migration plan.
- Use stable UUID identifiers for entities that can be backed up or synchronized.
- Store units explicitly and define timestamp/time-zone semantics. Do not rely on device locale to interpret stored values.
- Separate database schema from network protocol.

## 5. Synchronization

- Design sync for offline writes and later retry.
- Initial transport is direct Wi-Fi/LAN with wait/find pairing, direction selection, and merge/copy/replace modes. Server transport is planned later.
- Define identity, revision/version, deletion, ordering, idempotency, and conflict behavior before server implementation.
- Synchronize records or operations at an appropriate entity granularity; do not replace an entire user's database to apply one change.
- Make permissions and account state clear to the user. Local use must not require an account.

## 6. Backup compatibility

- Backup/export formats are versioned and platform-neutral.
- Define encryption, integrity checks, import validation, and migration policy before implementation.
- Changes affecting backup compatibility must be documented for both Android and Windows.
- A backup is distinct from continuous device synchronization.

## 7. Design-first workflow

Maintain `ИСПРАВЛЕНИЯ.md` in Russian with the reason for each material change, actual verification results and remaining limitations. Record performance fixes explicitly, including slider persistence.

For a feature: define user behavior and edge cases → define shared data semantics → review Android, Windows, and future Linux impact → record decisions → implement → verify when requested → update documentation.

As of 2026-10-02 vibic777 authorized autonomous resolution of technical questions and implementation. Record rationale and compatibility implications before implementing each contract; keep TODO current. Product decisions remain as agreed.

## 8. Repository hygiene

- App-lock attempt limits follow Notio: configurable delays, recovery-only or opt-in local wipe. Wipe defaults off, requires typed confirmation, and must not delete records on paired devices or external backups.

- Keep secrets, signing material, encryption keys, personal health data, and real exports out of version control.
- Configure Git identity locally for this repository as `vibic777 <vibic@yandex.ru>`; do not use `--global`.
- vibic777 authorized development commits on 2026-10-02. Commit only reviewed source/docs; exclude secrets and build deliverables.
- Document user-visible behavior and compatibility impact alongside code changes.

## 9. Required delivery formats

- Windows uninstall offers an explicit localized data-removal choice, default No. Silent removal preserves data unless FITEASYDELETEDATA=1 is explicitly provided. Cleanup runs only after successful uninstall, never upgrade/repair, and only within the current user's standard FitEasy data directory. External backups, portable data and other Windows profiles are excluded.

- Every delivery includes five artifacts from the same application source revision: Android debug APK, Android release APK, Windows EXE installer, Windows MSI installer, and Windows portable ZIP.
- Use managed EXE/MSI tasks so the interactive running-app dialog is included. Portable ZIP includes the application and bundled runtime; it runs after extraction without installation or a separately installed Java.
- Portable mode keeps data in its own data folder beside the application, separate from the installed client's LOCALAPPDATA folder. Windows DPAPI secrets remain bound to the Windows user/machine; use the shared backup format to move data between machines.
- Record version/build/source revision, actual build results, sizes and SHA-256 in delivery documentation/manifest. Provide links to all five formats. Explicitly label release APK test signing while production signing is absent.
- Never include existing user data, secrets, signing material or machine-local configuration in a portable archive. Build outputs and archives stay outside Git.


