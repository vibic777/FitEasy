# Notio-Android Reference Review

2026-10-03: vibic777 просит перенос функциональности уведомлений/статусов/прерывания. Проверены RemindersScreen, BackgroundRestriction, ReminderNotifier, ReminderStatusNotification, ReminderRingService и MIT LICENSE (Copyright (c) 2026 vibic777). В 0.3.8/15 адаптированы независимо разрешения/тихая сводка/действия/переходы, без копирования исходников/ассетов. Полная звуковая служба/свои мелодии остаются открыты; при последующем переносе кода сохранить исходное MIT уведомление в источниках и поставке. CloneNames.next также проверен: принцип первого свободного номера реализован независимо для новых заметок/папок по уточнению пользователя.

0.3.2/9 transfer implementation completed independently: raw staging database, validation before import, explicit key show/copy/change and confirmed SMTP attachment. No Notio database/UI/mail source copied. See BACKUP_FORMAT.md and ИСПРАВЛЕНИЯ.md for actual checks and pending hardware interoperability.

2026-10-02: Android SyncSettings.kt and Windows SyncPage.kt reviewed for grouped connection actions, device cards, separate transfer options and readable waiting code. Adapt the layout independently; FitEasy selection, local-only settings and protocol remain unchanged. Settings database transfer reviewed for password show/copy, validated import and SMTP attachment; implementation and Windows tests completed in 0.3.2/9; real device/mail verification remains pending.

2026-10-02: Android AppLog/CrashLog and settings AppLogOptions/AppLogDialog plus Windows shared/AppLog inspected. Adapt independent writer/rotation/levels/always-FATAL/daily cleanup/copy and Android previous exit reporting. FitEasy implementation uses fixed event IDs and exception class/stack without exception messages or health/credential values. See APP_JOURNAL.md; no logger source copied.

2026-10-02: StampOptions in SettingsScreen.kt inspected: four separate switches for creation/modification time of notes/folders. FitEasy adapts independent switches with localized formatting and explicit unknown legacy creation time; folders/filter/view are implemented independently. No note-browser code/assets copied.

2026-10-02: SettingsScreen.kt reviewed for SettingsSection/SettingEntry and localized search matching titles/hints/choices. FitEasy independently implements eight groups and search results naming matched settings; protection and LAN are accessed inside settings. No source copied for this menu.

**Purpose:** vibic777 asked to use interesting Notio-Android ideas as inspiration for FitEasy. This is a product and engineering reference, not a request to copy its implementation wholesale.

## Repository inspected

Local reference: `D:\Notio-Android`. Its README, translation guide, Android locale configuration, and license were reviewed on 2026-10-01. Notio identifies itself as an offline Android notebook by vibic777. Its source repository declares the MIT License; check the exact license and notices for any specific material before reuse.

## Ideas worth adapting to FitEasy

- **Ten-language workflow:** English default plus Russian, Belarusian, French, German, Spanish, Italian, Simplified Chinese, Japanese, and Korean. Notio keeps some locale files directly and generates others from TSV sources, checks key completeness, and warns that translations need native-speaker review. FitEasy should adopt the completeness and review discipline; choose its own tooling after the localization decisions.
- **Local-first behavior:** Notio keeps core data available offline. FitEasy should preserve offline food search and logging while syncing only when enabled and available.
- **Layered app organization:** domain models and repository interfaces are kept apart from Room data and feature screens. FitEasy's proposed domain/repository boundary follows the same useful separation.
- **Database encryption:** Notio offers optional SQLCipher encryption with key material sealed by Android Keystore. FitEasy requires a user-controlled database encryption feature; review the threat model, default-off behavior, key recovery, migration, and non-Android equivalents before choosing the precise mechanism.
- **Explicit permission timing:** ask for sensitive permissions when a user enables the related action. Apply this to Health Connect and notifications.
- **Practical portability:** Notio distinguishes regular backups from raw encrypted database transfer. FitEasy should favor a versioned cross-platform backup; raw DB files cannot serve as Android/Windows/Linux interchange.
- **Device pairing:** Notio's same-Wi-Fi, code-paired direct sync is a useful reference for pairing UX. FitEasy's requirement is broader: synchronization across Android, Windows, and later Linux devices, across OSes on the same LAN initially; server transport later. The shared protocol and service/discovery choice need separate design; do not assume Notio's local-network implementation meets it.
- **Appearance system:** Notio uses Material 3 color schemes with light/dark/system modes, selectable color palettes plus a custom color, and configurable text size. It checks palette contrast and supports backgrounds. FitEasy can borrow user choice, readable contrast, and scalable text, while creating its own calmer wellness palette and dashboard-focused layout.

## Sync details observed in Notio

The Notio security review describes discovery only while its Sync settings page is open, using mDNS on the local network. Devices are paired by showing/entering a code and confirming the peer name. The code itself is not sent; the handshake uses fresh ECDH P-256 keys and HKDF-SHA256, authenticates both sides before data, then encrypts framed traffic with AES-256-GCM and directional counters. Pairing secrets are sealed with Android Keystore. This is a strong reference for a nearby-device pairing/transport flow. It is still same-network direct sync, which fits the initial FitEasy LAN scope; remote server transport comes later.

Notio's database encryption is currently an optional setting, off by default: it converts the database to SQLCipher in place and protects the generated key with Android Keystore. FitEasy now follows optional, settings-controlled encryption like Notio; the initial default is off, accepted on 2026-10-01. We should reuse the design lessons—transactional conversion, key separation, explicit recovery, and testing—while evaluating user-controlled encryption and platform-specific key custody.

## Appearance details observed

Notio builds light/dark Material 3 color schemes from named palettes and has a custom-color path that adjusts shades against contrast thresholds. Its palette includes green, pink, orange, red, light blue, blue, lime, black, gray, white, yellow, lilac, and magenta. Theme mode can follow the system or be forced light/dark; text scale has five app-level steps. A separate background feature can show user-selected imagery behind the page. Its typography uses a bundled Roboto family for consistent bold/italic rendering with system fallback for other scripts. These are implementation examples, not a mandate to copy the exact palette, font, or background feature.

For FitEasy, transferable ideas are clear system/light/dark choice, a small tasteful accent palette, accessible contrast, user/system text scaling, and robust font fallback for all ten languages. Decorative backgrounds are lower priority because nutrition dashboards need visual calm and data readability. Review FitEasy screens before deciding whether personalization needs more than a few theme choices.

Updated decision 2026-10-02: custom palettes/colors and background images are included in initial FitEasy scope, alongside system/light/dark and text size. Use calm defaults and maintain readable surfaces over images. This supersedes the earlier lower-priority background proposal.

## Reuse and attribution rule

### Selection and settings review (2026-10-01)

Notio's SyncDialog offers All folders or individual folder checkboxes, combines local/peer folder lists, remembers choices for each pairing and disables starting with an empty selection. Replace is permitted only for all folders in one direction. The sync adapter uses BackupService: selected folders include their notes, related records and reminders; export also includes settings. Import applies settings in REPLACE mode and restores reminder scheduling. Thus Notio can carry reminders and replace device settings. FitEasy's accepted design excludes appearance, language and reminders from every sync mode, including replacement.

Use Notio features as prompts for FitEasy-specific design. Do not copy code, translations, artwork, or substantial text without checking the exact license, preserving required notices, and recording provenance. MIT-licensed code can generally be reused under its terms with copyright/license notices, but any reused files or substantial portions must be identified and reviewed before inclusion. Prefer independently implementing the product behavior when only the idea is needed.

## Notio does not determine FitEasy decisions

FitEasy has its own nutrition domain, cross-OS sync requirement, encrypted database requirement, backup compatibility, and health-safety needs. Notio's optional database encryption and local Wi-Fi sync are references only; they do not set FitEasy's defaults, threat model, sync topology, or conflict policy.

2026-10-04: Notio-Windows `desktopApp/.../WindowChrome.kt` and AGENT.md D14 reviewed for the unified title bar. FitEasy adopts the same JetBrains Runtime custom title bar mechanism with its own implementation (`WindowChrome.kt`, shared `WindowBar.kt`); no Notio source file copied. See DECISIONS.md D082.

2026-10-04: Notio-Android `ReminderStatusNotification.kt` and `TagUseCases.kt` reviewed. FitEasy adopts the quiet ongoing summary (extended with steps, today count, waiting series and an interrupt action) and the tag naming rules with an independent implementation; no Notio source copied. See DECISIONS.md D085.

2026-10-04: Notio-Android settings for the status notification and the reminder alarm (full-screen over the lock screen, overlay status, 10-second test), its full-screen settings pages and its sign-in kinds (PIN, password, login and password, pattern) reviewed. FitEasy implements them independently; see DECISIONS.md D086 and D087. No Notio source copied.

2026-10-05: Notio-Android RemindersScreen.kt, ReminderRingService.kt, ReminderSounds.kt, RingRamp.kt, SyncSettings.kt, LockSettingsScreen.kt, RecoverySettings.kt и русские строки просмотрены повторно. В FitEasy перенесены: карточки и диалог напоминания, служба мелодии с громкостью и нарастанием, выбор мелодии, строка «Звучит сейчас», компоновка страницы синхронизации, таймаут автоблокировки и готовые вопросы. Служба, выбор мелодии и расчёт нарастания адаптированы из исходников Notio (MIT, тот же автор) — запись в NOTICE.md; остальное написано заново. См. DECISIONS.md D097, D099, D100.

2026-10-05 (второй проход): Notio-Windows shared/sync (SyncEngine, SyncStore, SyncAddress), SettingsPages.kt, DesktopSettings.kt, DatabaseEncryptionPage.kt и ресурсы строк Notio-Android. Перенесены движок синхронизации и тексты (запись в NOTICE.md); страницы настроек написаны заново по их компоновке. См. DECISIONS.md D105, D106.

