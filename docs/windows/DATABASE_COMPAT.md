# Database Compatibility Notes

0.4.0/20: SQL schema 2 прежняя; Exercise.rig и SharedData.exerciseAssets — необязательные JSON поля с пустыми defaults. Для сохранения новых сцен использовать оба обновлённых клиента: LAN protocol 6 / Transfer 5 / backup writer 5, reader 1–5. Локальный переход из трея/язык/статус окна не входит в синхронизируемые данные. См. SKELETAL_ENGINE.md; нормализация exerciseAssets в отдельную таблицу остаётся до окончательного релиза.

D070: отдельный JSON fiteasy-catalogue v1 одинаков для Android/Windows и будущего Linux. Импорт меняет только выбранные продукты/блюда; замещение создаёт tombstones для отсутствующего stock, дневник сохраняет снимки. SQL schema 2 не меняется. Размер 32 MiB/20000 записей, полная валидация до записи. Этот файл не является снимком всей БД или резервной копией.

D065: catalogueId/technique уже предусмотрены; встроенный каталог — только ресурс программы, выбранная строка хранит собственный текст и параметры. Новых полей/SQL-таблиц/версий переноса нет. Windows очистка после деинсталляции не создаёт sync tombstones и не удаляет копии на Android, внешние backup или portable.

D064: физическая schema 2 / LAN 5 / Transfer 4 / backup 4 не меняются. Выбранная схема движения использует уже существующий Exercise.technique. Мужской/женский вариант хранится только в локальном защищённом exercise-avatars, не в SharedData и не в переносе. Автоимена — обычные локализованные строки, UUID не меняются. Темы сохраняют прежние ID после удаления возрастных метаданных.

D063 / 0.3.7/14: Exercise adds sets/setRestSeconds/loadKg/catalogueId/technique with backward defaults; WorkoutResult adds immutable per-set outcomes. Physical schema 2 remains. LAN 5 / Transfer 4 / backup writer 4 reader 1–4 refuse older clients that could drop set fields. Local workout draft is sealed separately and excluded from transfer; restore pauses. Session UUID is reused as diary entry ID to prevent repeated-save duplicates.

0.3.6/13 D062: physical SQL schema 2 uses separate aggregate tables and singleton metadata; transactional migration 1→2 verifies the complete snapshot before dropping app_state. Android/Windows share SqlStateStore with SQLCipher/JDBC adapters. Raw reader accepts schema 1/2; raw writer and key conversion create schema 2. Old clients must use shared .fiteasy backups, not new raw files. LAN 4 / Transfer 3 / backup 3 remain unchanged. Nested historical snapshots remain JSON intentionally; this is aggregate normalization, not full relational decomposition. See ../STORAGE_MIGRATIONS.md.

D058 planned: migrate legacy Exercise to one set without changing historic meaning; define per-set actual results and session draft. Before shipping, implement explicit capability/version rejection so old clients cannot silently discard new program details. No schema or protocol bump has been made at the design stage.

0.3.3/10: chart changes use existing Appearance.charts/ChartStyle.kind; immediate save changes no schema. Existing optional appearance backup behavior and LAN exclusion remain.

0.3.2/9: SQLCipher JDBC uses SQLiteMCSqlCipherConfig v4 with the password supplied before initial connection pragmas. Raw export/import verifies schema 1 and integrity; import preserves current device key/security and clears phone baseline/demo registry. Same shared tools exist on Android. Real encrypted raw-file exchange with Android is pending device verification; `.fiteasy` remains preferred.

0.3.1/8: additive local Appearance.style/animations/sectionBackgrounds/backgroundImages with defaults; image references SHA-256, 1 MiB per image and 8 MiB total unique images. Common validation and serialization on Android/Windows. Old general background remains fallback; section override may intentionally remove the image. LAN excludes Appearance; opt-in appearance backup envelope stays 3, older apps ignore new fields and cannot preserve them when rewriting. Shared schema/LAN 4/Transfer 3/SQLite DDL 1 unchanged. APPEARANCE.md has the contract; normalization/migrations remain pending.

0.3.0/7: SharedData.noteFolders and optional Note.folderId/created; old Note creation time stays null. Appearance.noteDisplay is local (opt-in appearance backup only). LAN protocol 4 / Transfer 3 require both clients updated; backup writer 3 / reader 1–3. Folder UUIDs and note references are remapped together in COPIES, reconciled in rebase, included only in notes selection. Notes category reset clears folders. Orphan/deleted folder references appear unfiled. SQLite DDL remains 1, additive JSON fields default safely. Progress filters/ranges are viewing state. See ../NOTES.md and ../PROGRESS_PERIODS.md.

0.2.4/6: optional Appearance.charts map (eight metric keys, ChartStyle color ARGB + kind bars/line/area/dots) is device-local and excluded from LAN; opt-in appearance backup carries it. Older readers ignore the field. LocalData.demo hashes remain after safe cleanup for force cleanup. Physical reset uses atomic Snapshot mutation, retains device secrets/encryption; selected stored catalogues retain recipe dependencies. No shared schema/protocol version change. See ../DATA_RESET_AND_CHARTS.md.

2026-10-02: LocalData.demo is an optional device-local UUID → original SHA-256 registry. Android/Windows preserve edited demonstration records during cleanup. It is excluded from SharedData, LAN and Backup; transferred samples become ordinary records. Existing JSON reads default to an empty registry. Desktop snapshot caching publishes only committed state; encryption verification explicitly reads the database.

Portable 0.2.1: меняется только корневая папка локальных файлов — data рядом с приложением при наличии portable.txt. Форматы БД/JSON/LAN/.fiteasy не меняются. DPAPI и один экземпляр применяются к собственной папке; в архиве поставки нет существующих DB/секретов.

2026-10-02: shared JSON snapshot добавляет schedules (DaySchedule/ScheduleRow UUID, календарные правила, порядок) и notes (Note/NoteItem/InkStroke UUID, нормализованные точки, текст/галочки, tombstones). Значения по умолчанию пустые, прежние снимки читаются; SQLite DDL остаётся прототипом v1. Бэкап writer v2/reader v1–2, Transfer v2/LAN protocol v3, sections schedules/notes. Локальные напоминания с scheduleId/rowId и состояния серий не входят в перенос. Windows закрытие в трей — отдельный DPAPI secret; instance.port/lock — только файлы координации, не пользовательские записи. Нормализация и явные миграции по-прежнему нужны до готовности первого релиза.

The Windows client may use a different database engine and schema layout. Compatibility is defined by record meaning and portable serialization, not byte-identical database files.

| Concern | Shared rule | Android proposal | Windows decision |
|---|---|---|---|
| IDs | UUID for portable/syncable user records | Room entities | Pending |
| Time | UTC instants plus local event-day context as needed | Kotlin time types to be selected | Pending |
| Units | Explicit canonical value/unit and conversion rules | Domain value objects | Pending |
| Nutrition | Nullable nutrient values; unknown differs from zero | Room columns | Pending |
| Catalogues | Separate Product and Dish entities; explicit diary source type; Recipe composition | Schema pending | Same domain distinction |
| History | Diary keeps logged-value snapshot | Embedded/related snapshot | Pending |
| Delete | Tombstone semantics for synced records | Soft-delete columns | Pending |
| Migration | Versioned and documented | Room migrations | Pending |
| Encryption | At-rest protection required for personal data | SQLCipher proposed | Pending |
| Backup | Shared versioned format | Import/export adapter | Import/export adapter |

Never use the Android SQLCipher database file itself as the cross-platform backup or sync contract. Record any changes to shared rules in this document and `DATABASE.md`.

D076 (0.4.4/24): LocalData.legalNoticeVersion default 0 в JSON metadata SQL 2. Старый payload без поля читается с 0, новой SQL migration нет. SharedData/LAN/backup/appearance не содержат ознакомления; импорт сырой DB сбрасывает привезённый флаг в 0. Полный reset/wipe повторяет экран, частичный сохраняет. Android/Windows одинаковы.

D097/D099 (0.4.20/40): Reminder.soundUri (null), volumePercent (100), rampSeconds (0) хранятся в JSON local_reminders SQL 2; LockSettings.timeoutSeconds (0) — в секрете app-lock устройства. Старые записи читаются со значениями по умолчанию, новой SQL-миграции нет. soundUri — адрес на этом устройстве (content:// или file:// на Android, путь к файлу на Windows): после импорта сырой базы на другом устройстве мелодии по такому адресу нет, напоминание звучит системным тоном. SharedData, LAN, Transfer и .fiteasy этих полей не содержат.

D102/D103 (0.4.21/41): SqlStateStore.write пишет только изменённые строки таблиц и строку metadata при изменении; содержимое файла после записи то же, что при полной записи (проверено тестом с переоткрытием). Reminder.noteId (null) хранится в JSON local_reminders; после переноса сырой базы связь действует, если заметка с этим UUID есть в базе. SharedData, LAN, Transfer и .fiteasy не изменены.

D107 / 0.4.23/43: корзина, справка, «Справка и о приложении» и блок шифрования базы повторяют Notio; значки — набор Notio (NotioIcons.kt). LocalData.trashRetentionDays (0 = никогда, по умолчанию) в JSON metadata SQL 2, старые записи читаются с 0; прежнее trashDays не используется. Общий код Android/Windows; LAN 7, Transfer 5, backup 5 прежние.

D108 / 0.4.24/44: LocalData.remindersOff — общий выключатель напоминаний устройства; раздел синхронизации «Напоминания» (Transfer.reminders, необязательное поле, версия Transfer 5 прежняя; мелодия не переносится); формы редактирования — полноэкранные страницы, на Windows слой во всё окно (LocalDesktopWindow) вместо диалога с полями. Общий код Android/Windows; LAN 7, backup 5, SQL 2 прежние; резервная копия .fiteasy напоминаний по-прежнему не содержит.

D113 / 0.4.29/49: новая установка начинает с выключенными напоминаниями (LocalData.remindersOff=true только в первой записи новой базы; существующие данные не меняются), создание напоминания при выключенных сразу показывает подсказку «Включить / Не включать». Замещение при синхронизации возможно для выбранных разделов, по-прежнему только в одну сторону: в выбранных разделах принимающее устройство получает ровно принятое, остальные не трогаются (replaceSections). Reminder.updated — время изменения напоминания (0 у прежних): слияние берёт более позднее, мелодия устройства сохраняется. Общий код Android/Windows; SQL 2, LAN 7, Transfer 5, backup 5 прежние. Запрос выборочного замещения устройство до 0.4.29 отклоняет, поэтому обновлять оба.

D119 / 0.4.35/55: фигура упражнения — один целый человек в любой позе и с любой стороны. Плечи держатся на грудной клетке и наклоняются вместе с ней (rigTorsoFrame); силуэт соединяет плечи, бёдра и шею с туловищем, скелет — шею с позвоночником и бёдра с тазом. В редакторе виды: спереди, оба бока, сзади и сверху (RigPose.pitch, по умолчанию 0; rigProject с наклоном сцены). Общий код Android/Windows. Поле pitch необязательное: SQL 2, LAN 7, Transfer 5, backup 5 прежние; версия до 0.4.35 сцену читает, но показывает её без наклона.

D122–D126 / 0.4.36/56: установщик пишет каждый файл заново (REINSTALLMODE=amus), первый класс программы Launcher.java проверяет файлы class path и при пропаже сообщает на языке системы, код завершения 3 (D122). Мелодия напоминания: MP3 (mp3spi, jlayer) и OGG Vorbis (JOrbis, OggVorbis.kt) наравне с WAV, AIFF, AU; распаковывается только время звучания (D123). Ключ TOTP показан QR-кодом, ZXing core, общий код (D124). Запись базы — в своём потоке (SaveQueue), проверка и состояние в памяти сразу; перед закрытием и заменой файла запись дожидается; формат базы, LAN 7, Transfer 5, backup 5 прежние (D125). Обратный отсчёт экрана блокировки не тикает, когда окно свёрнуто (D121). В справочнике упражнений 15 карточек с описаниями техники, общий код (D126). На Windows руками не проверялось: установка поверх работающей программы с перезагрузкой, звучание MP3/OGG, считывание QR-кода с экрана.

D127–D128 / 0.4.37/57: «Настройки → Права» — общий код (RightsUI.kt, Platform.rights / grantRight / revokeRight / revokeAllRights). На Windows отдельных прав нет: страница открывает параметры уведомлений (ms-settings:notifications) и список разрешённых приложений брандмауэра, состояние приложению неизвестно; «Отозвать все права» показывается только на Android (D127). Заголовок окна и подсказка в трее — «ФитИзи» при русском языке Windows, иначе «FitEasy» (appSystemName); ярлык в «Пуске» и имя в списке программ остаются «FitEasy» (D128). База, LAN, backup не менялись.

D129 / 0.4.38/58: пакеты Windows собираются только на JetBrains Runtime SDK 25 — иначе шапка приложения не становится заголовком окна и Windows рисует свою полосу (так вышли 0.4.36 и 0.4.37). Упаковка и сценарий поставки это проверяют. Код не менялся.
D130 / 0.4.39/59: мелодия напоминания открывается только из папки звуков Windows и из `reminder_sounds` в папке данных; сетевой путь не трогается (адрес приходит и из импортированного файла базы). Чужой файл базы читается с `PRAGMA trusted_schema=OFF`. Запрет снимков экрана при блокировке и правила переноса данных — только Android.
