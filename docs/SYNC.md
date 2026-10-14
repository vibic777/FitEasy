# Synchronization Design

0.3.7/14 D063: LAN protocol 5 / Transfer 4 prevents older clients from losing sets/outcomes. Upgrade both clients. Selection/direction/import modes unchanged; unfinished sessions and visual preferences remain local. SQL schema 2 is separate from network version.

0.3.0/7: LAN handshake protocol 4, Transfer version 3. Upgrade both clients: protocol 3 is refused before any data exchange. The notes section includes noteFolders and Note.folderId/created. COPIES remaps folder IDs and references together; rebase retains concurrent local folder edits. Appearance.noteDisplay remains local. See NOTES.md and windows/DATABASE_COMPAT.md. Existing peer secrets do not change; handshake version negotiation prevents loss by old readers.

Agreed direction (2026-10-01): initial direct Wi-Fi/LAN sync across Android and Windows; Linux later. Server transport is a future phase. Initial sync needs no cloud account.

## Pairing and roles

Either device can initiate. One selects Wait for device and displays a code; the other selects Find device, chooses the peer and enters the code. Confirm peer identity before exchange. Wait/find roles do not determine data direction. Pairings can be remembered and removed. Proposed discovery: mDNS plus manual address fallback; listen/discover while the sync screen is active, following Notio.

## Directions and modes

Choose Send, Receive, or Both directions. The other device confirms direction, scope and mode before records change. No permanent master is required.

- Merge: match stable UUIDs and add missing records. Concurrent edits and deletion tombstones need a defined conflict policy; device clocks alone are insufficient.
- Combine / add copies: add incoming records with fresh UUIDs and remap relationships. Preserve existing recipient records. Warn about duplicate diary entries and inflated totals if repeated.
- Replace: after validation make the receiving dataset match the sender. Only one-way full-dataset replacement is allowed, with explicit confirmation and a recoverable safety snapshot. Settings inclusion remains open.

These match Notio modes MERGE, NEW_IDS and REPLACE. FitEasy must preserve recipe/food/diary references and historical nutrition snapshots. Use a portable versioned dataset, rather than raw database files or database keys.

## Transfer selection (accepted 2026-10-01)

Offer checkboxes for nutrition/recipes, hydration, activity, measurements and goals, plus All data. All data means all shared user records. Appearance, language and reminder preferences remain local to each device in every mode, including replace. Replacement affects only the agreed shared dataset.

Include referenced custom foods/recipes and other dependencies needed to interpret selected diary records. Show automatically included dependencies in the transfer summary. Reject an empty selection. Final section boundaries and profile transfer details remain under design.

Catalogue dependencies distinguish Product and Dish IDs and include any required Recipe composition. Separate catalogue update/version semantics must preserve user overrides and logged snapshots.

Activity selection includes custom workout routines and referenced exercise definitions where needed; completed sessions retain the routine snapshot used. Built-in catalogue versions and custom routine dependencies must be negotiated without duplicating shared definitions.

Beverage records can contribute to both hydration and nutrition. Selected transfers must include required linked records and deduplicate by stable consumption identity, so transferring both sections never doubles drink calories. Exact linkage is under design.

## Security and reliability

Authenticate peers and encrypt transfers. Evaluate Notio's fresh ECDH/key derivation/AES-GCM approach for a portable protocol. Pairing keys and database keys are separate and protected by each OS. Detailed cryptographic protocol is still proposed.

Validate integrity, schema compatibility, units and relationships before transactional application. Interrupted transfers must not leave partial replacements; merge retries must not duplicate records. Logs describe peers and outcomes without health content.

## Shared metadata and future server

Trash is included in domain deletion design. Preserve deletion evidence independently from user-visible trash retention so an offline peer cannot silently resurrect purged records. Define restore-versus-delete conflicts and tombstone acknowledgments before implementation. Security-triggered local wipe must not generate propagated deletions.

### Conflict interaction (accepted 2026-10-02)

When concurrent edits to the same record cannot be merged safely, show both versions and allow Keep local, Accept incoming or Keep both. Keep both creates a distinct record with remapped dependencies as needed; warn about duplicate nutrition/activity totals. Offer optional automatic conflict resolution for explicitly defined simple-record types. Its policy is still to be designed and must not rely solely on device wall clocks. Default unresolved conflicts require user choice and remain recoverable across interrupted sessions. Define edit-versus-delete handling separately; copy mode and explicit replace retain their agreed semantics.

Use UUIDs, origin device/change IDs, version evidence and tombstones. Revision semantics, conflict policy and deletion retention remain open. Keep shared serialization independent of transport so server sync can be added later. Accounts, remote device management, background scheduling and server retention are future work.

## Open points

Detailed section boundaries and profile transfer, conflict/deletion policy, copy duplication UX, replacement recovery, protocol compatibility and key custody on each OS.
# Текущий прототип 2026-10-02

Реализован отдельный FitEasy LAN protocol v2 на адаптированном криптографическом транспорте Notio: mDNS _fiteasy-sync._tcp.local., ждать/искать, ручной IP/порт, код первой пары и подтверждение имён. После подтверждённой первой пары стороны сохраняют общий 32-byte random secret через Keystore/DPAPI; дальнейший ECDH handshake аутентифицируется этим секретом. Хранятся выбранные разделы и режимы приёма по доверенному устройству. Удаление доверия доступно локально; если другая сторона забыла пару, нужен новый код.

Передаются только SharedData и выбранные разделы. Ссылки на исходные продукты материализуются; Appearance/LocalData/секреты не передаются. Замещение требует всех разделов и одного направления. Слияние пока использует явно выбранную политику local/incoming для ID; отдельное review конфликтов ещё не подключено. При слиянии/копировании изменения, сделанные локально во время передачи, сохраняются поверх принятого результата; замещение при изменении общих данных отклоняется с предложением повторить. Практическая проверка двух устройств ещё не выполнена; ниже сохранены полные требования целевого протокола.

# Уточнение 2026-10-02

Текущий LAN protocol 3 / Transfer version 2 добавляет sections schedules и notes. Оба клиента должны обновиться вместе; несовместимая версия отклоняется до переноса. Графики/заметки — общие записи UUID; локальные привязки уведомлений и runtime не передаются. Копии создают новые ID расписаний/строк и заметок/пунктов/штрихов. При удалении общей строки/графика локальный планировщик отменяет привязку.

D100 (0.4.20/40): страница синхронизации перестроена по образцу Notio — «Это устройство», «Новое соединение» (Ждать устройство / Найти устройство), сохранённые соединения с «Синхронизировать…» и «Удалить», один диалог перед передачей (адрес, код, направление, разделы, режим приёма). Протокол LAN 6 / Transfer 5 и состав разделов не менялись. Приём по-прежнему одноразовый: и для сохранённого соединения другое устройство должно нажать «Ждать устройство». «Удалить» убирает пару только на этом устройстве.

D105 (0.4.22/42): синхронизация заменена перенесённым из Notio-Windows движком. Устройство слушает порт 47821 (или свободный) и объявляет себя по mDNS, только пока открыта страница и приложение на переднем плане. Пара создаётся один раз: одно устройство показывает код, другое вводит его; ключ пары (32 байта) хранится в секрете устройства sync-store. Дальше синхронизацию запускает любое из двух: выбираются направление, способ (слияние / копии / замещение) и разделы; другое устройство показывает вопрос с этими условиями и после разрешения выполняет свою часть — «Ждать» нажимать не нужно. Замещение возможно только для всех разделов и в одну сторону; принимающее устройство становится копией отправляющего, его локальные настройки, напоминания и защита сохраняются. Слияние сопоставляет записи по UUID, побеждает более позднее изменение (поле updated), поэтому правки и удаления доходят в обе стороны; профиль и цели берутся только пока на устройстве значения по умолчанию. Есть «Проверить связь» (сеть, приём, поиск, подключение, шифрование, часы, ответ), ручной адрес пары, журнал на 200 строк без кодов и секретов, удаление пары на обоих устройствах. Протокол LAN 7 несовместим с 0.4.21 и раньше: старые пары не действуют. Transfer 5 и состав разделов прежние; предел передачи 64 МиБ.

D108 (0.4.24/44): в списке разделов синхронизации появился раздел «Напоминания» — все напоминания устройства разом, выбирается как любой другой раздел; «Все данные» включает и его. Слияние добавляет напоминания, которых на устройстве нет (у напоминаний нет времени изменения, поэтому существующие не перезаписываются); копии получают новые идентификаторы; замещение делает список таким, как на отправляющем устройстве. Файл или адрес мелодии не переносится: напоминание звучит системным тоном. Устройство с версией до 0.4.24 поле не понимает и напоминаний не примет. Расписания («папки задач») остаются своим отдельным разделом.

D109 (0.4.25/45): у каждого соединения кнопки «Направление», «Как», «Разделы» — «выбирается» или заданное заранее значение. «Синхронизировать…» спрашивает только незаданное; когда заданы все три, синхронизация начинается сразу. Второе устройство по-прежнему спрашивает разрешение.

D110 / 0.4.26/46: принимающее устройство не показывает вопрос, если его собственный заранее заданный выбор для соединения точно совпадает с запросом. Общий код Android/Windows; протокол LAN 7 прежний.

D111 / 0.4.27/47: запрос синхронизации с заданными заранее направлением, способом и разделами несёт отметку SyncMessage.preset, и второе устройство вопрос не показывает; при открытом выборе вопрос обязателен. Общий код Android/Windows; протокол LAN 7 прежний, обе стороны должны быть 0.4.27.

D112 / 0.4.28/48: один движок синхронизации на время работы приложения (SyncHost); переключатель «Постоянно ждать синхронизацию» — секрет устройства sync-always, по умолчанию выключен. Windows принимает синхронизацию, пока приложение запущено, включая трей; Android — пока приложение на экране, без фоновой службы. Протокол LAN 7 и форматы прежние.

D113 / 0.4.29/49: новая установка начинает с выключенными напоминаниями (LocalData.remindersOff=true только в первой записи новой базы; существующие данные не меняются), создание напоминания при выключенных сразу показывает подсказку «Включить / Не включать». Замещение при синхронизации возможно для выбранных разделов, по-прежнему только в одну сторону: в выбранных разделах принимающее устройство получает ровно принятое, остальные не трогаются (replaceSections). Reminder.updated — время изменения напоминания (0 у прежних): слияние берёт более позднее, мелодия устройства сохраняется. Общий код Android/Windows; SQL 2, LAN 7, Transfer 5, backup 5 прежние. Запрос выборочного замещения устройство до 0.4.29 отклоняет, поэтому обновлять оба.

D114 / 0.4.30/50: значок «Синхронизация» в верхней панели рядом с профилем открывает ту же страницу синхронизации, что и пункт меню настроек; пункт меню остался. Общий код Android/Windows; данные, протокол LAN 7 и форматы не менялись.

D116 / 0.4.32/52: значок «Напоминания» в верхней панели рядом с синхронизацией и профилем открывает «Настройки → Напоминания», как будильник в верхней панели Notio. Подтверждение синхронизации: второе устройство не спрашивает только тогда, когда на запускающем заданы все три выбора; при хотя бы одном «выбирается при синхронизации» вопрос обязателен — исключение D110 (совпадение с заданным на принимающем устройстве) снято. Общий код Android/Windows; протокол LAN 7 и форматы прежние.

D132 / 0.4.40/60: порт, на котором устройство ждёт синхронизацию, — автоматически (47821 или свободный) либо вручную (1024–65535); страница пишет, свободен порт или занят. Общий код (SyncEngine.portBusy, portFree, SyncStore.port); на Windows брандмауэр спросит разрешение для нового порта.
