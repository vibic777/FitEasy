# Backup and Restore Proposal

0.3.7/14 D063: writer 4, reader 1–4 includes set count/rest/load/catalogue reference and actual set outcomes; old exercises default to one set. Unfinished local drafts and recovery credentials excluded. Old clients must upgrade. Encryption envelope unchanged. D062 raw writer schema 2 / reader 1–2 supersedes historical schema-1 entries below.

## 0.3.2/9: raw database and mail tools

Settings/Data contains database transfer tools. Export rebuilds a closed staging SQLite/SQLCipher schema-1 database from a committed Snapshot; empty export password means plaintext, otherwise SQLCipher v4. The live database key is never embedded. Database password can be shown/copied explicitly or changed (8–256 characters); platform sealed storage and recoverable conversion remain in use. Imported database is checked for schema 1, integrity, shared/local validation before a transactional save with typed replacement confirmation. Current device encryption/key and security credentials are retained. Imported phone baseline and demo cleanup registry are cleared. Raw files include local appearance/reminders; `.fiteasy` remains the preferred selective portable format. Real Android↔Windows raw encrypted-file compatibility still requires a device check.

User-triggered SMTP sends either a `.fiteasy` shared-data backup without appearance or a database attachment, with the entered optional file password. Recipient is shown in a confirmation. Existing device-local SMTP configuration is reused; credentials and file/database passwords are not sent in the body or journal. MIME attachment filenames are restricted; size limit is 25 MiB and mail servers can have lower limits. No external mail is sent during verification.

0.3.0/7: writer version 3, reader versions 1–3. Shared noteFolders and note folderId/created are included; COPIES remaps folder IDs and links together. Optional appearance includes local noteDisplay. Old notes default to unknown creation time, no folder. Old clients cannot read v3 backups; upgrade both clients. Progress periods/calendar filters remain viewing state. Encryption envelope is unchanged.

0.2.4/6: Appearance.charts is included only with the optional appearance selection; LAN never sends it. Database/category reset and force demo removal do not remove external backup files. Previously backed-up data can return when restored. See DATA_RESET_AND_CHARTS.md.

Demonstration registry is device-local and omitted from backups. Restored samples are ordinary records and are protected from demonstration cleanup on the receiving device; see DEMONSTRATION.md.

**Status: draft format; product choices accepted 2026-10-02.** Backup is a portable snapshot; synchronization is continuous change exchange. They have different UX and failure semantics.

## Accepted behavior

- File extension `.fiteasy`, portable between Android and Windows, later Linux.
- Password protection is optional, following Notio. Password-protected archives use authenticated encryption; without a password, the archive is not encrypted and the export screen must state that clearly.
- Restore modes: merge, add as copies, or replace, preserving/remapping relationships according to mode.
- Include appearance settings only with a separate export checkbox. Applying included appearance during restore is an explicit choice; map supported settings across platforms.
- Exclude app-lock credentials, SMTP/TOTP/recovery secrets, pairing secrets and database keys in every mode.
- App-lock recovery cannot recover a forgotten backup password.

Container, cryptography parameters and compatibility rules remain under design. Local database encryption does not automatically encrypt exports. LAN sync keeps appearance local even though backup can optionally carry it.

## Requirements

- Platform-neutral, versioned format usable by Android and FitEasy-Win.
- Include schema/format version, export timestamp, declared units/locale metadata, and validated records.
- Preserve stable UUIDs, timestamps, provenance, and historical nutrition snapshots.
- Offer optional password encryption; validate archives before importing. Define plain-archive integrity checks separately from authenticated encryption.
- Validate the complete archive before applying it. Import must be transactional or recoverable and must never leave a silently partial database.
- Define duplicate handling for restore into a non-empty profile and support migration from older backup versions.
- Exported files must not contain app secrets, SQLCipher keys, authentication tokens, or device private keys.

## Candidate container

An archive containing a versioned manifest plus structured data files and optional media. Candidate structured encoding is JSON or a documented compact format; select after testing size, schema evolution, and Windows implementation costs. Encryption should use a vetted authenticated encryption format and a user-provided passphrase or platform-supported wrapping strategy. Algorithms, KDF parameters, file extensions, and recovery UX remain open decisions.

## Restore modes to decide

- Replace current local profile after explicit warning and safety snapshot.
- Merge records by UUID with a defined collision/conflict policy.
- One personal profile is supported; define how restored identity maps to the installation's existing profile.

Do not equate a raw database copy with portable backup: the local database is encrypted and platform-specific. See `DECISIONS.md` before choosing serialization or cryptography details.
# Уточнение 2026-10-02

Writer version 2 включает schedules/notes, reader принимает версии 1 и 2. Прежние снимки получают пустые новые списки. Настройки напоминаний/состояния срабатываний/Windows IPC и close-to-tray исключены. Optional appearance не переносит их. Копии создают новые UUID вложенных строк/пунктов/штрихов. Старая версия приложения должна отклонять v2 вместо потери незнакомых записей.

D106 (0.4.22/42): страница «Резервная копия» по образцу Notio. «Экспорт всех данных» пишет .fiteasy версии 5; если включён «Пароль резервной копии», копия шифруется этим паролем (секрет устройства, в копию не входит). «Импорт всех данных» пробует сохранённый пароль, иначе спрашивает его; затем выбор «Объединить / Добавить как новые копии / Заменить всё». Объединение — побеждает более новое изменение записи, как в синхронизации. Формат файла не менялся. Встроенные записи каталога с невозможным значением нутриента (отрицательные углеводы «по разности» у десяти записей USDA) экспортируются с неизвестным значением: раньше одна такая запись прерывала экспорт продуктов и блюд.
