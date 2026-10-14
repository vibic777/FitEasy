# FitEasy-Win Plan

D073: тот же RigEditor/жесты выбора и перемещения предметов, что Android; hover подсказки AWT/Compose, на телефоне long-press. Никаких отдельных Windows костей или схемы. Практически проверить мышь/малый размер окна; интеграция и 102 автоматических проверки не равны UX одобрению.

D071: будущий автоперевод не требует Azure ресурса/ключа пользователя. Оценить локальный движок с загрузкой моделей средствами FitEasy; проверить десять языков/лицензии/скорость/память. ML Kit не считать Windows SDK. Единый интернет-провайдер через будущий сервис FitEasy требует отдельного бюджета. Сейчас только план, AUTO_TRANSLATION_PLAN.md.

D065: общий справочник из 20 локализованных карточек/поиск/фильтры/выбор в программу и контурные фигуры с показом по нажатию. Windows managed removal получает вопрос данных с сохранением по умолчанию; очистка после успешного finalize только текущей стандартной папки, без upgrade/repair. Расширение до 60/экспертная визуальная проверка и реальные деинсталляции остаются открытыми.

D064 / 0.3.8/15: общий конструктор/плеер силуэтов, защищённое локальное оформление, автоимена с первым свободным номером и темы без возрастных ярлыков. Runtime состояния напоминаний отправляют события без двухсекундного опроса. Windows tray/beep сохраняются; постоянная шторка/Android разрешения специфичны Android. Полная звуковая служба и физическая производительность остаются задачами.

D063: shared set-state machine and DPAPI workout draft; upgrade both clients for LAN 5/Transfer 4/backup 4. Session ID makes repeated diary saves idempotent. Recovery pauses; no closed time counted.

2026-10-03 D060–D062: shared routine row edit/copy and monotonic clock; password backups run off UI with replace conflict guard; shared SQL aggregate schema 2 and explicit migration 1→2. Windows raw import reads 1/2, exports 2, so both clients must update for new raw DB interchange. Device UI/physical LAN/silent installation checks remain pending.

D058 planned: shared exercise library/card filters and ordered program builder, sets/repetitions/timers/rest/load, per-card male/female original animated silhouettes and ten-language descriptions. Same semantics as Android; Windows uses wide layouts. EXERCISE_LIBRARY.md defines actual scope; no implementation is claimed yet.

0.3.3/10: immediate chart selection and preview shared with Android. Verify four kinds, independent colors and database reopen; no Windows-only chart implementation.

0.3.2/9: implemented shared filled icons, voluntary 16/25/30/40/50 styles, visible-only 20 fps subject scenes, compatibility renderer selection, device journal, grouped LAN UI and raw database/SMTP attachment tools. Windows does not activate phone sensors. Delivery checks and hardware responsiveness verification are tracked separately in TODO/ИСПРАВЛЕНИЯ.md.

0.3.1/8 prerelease: five shared styles, vivid palettes/RGB, per-section backgrounds and optional short animations implemented. Cache/no-op saves and IO startup reduce avoidable work; runtime includes jdk.jfr for opt-in profiling. Actual mouse/frame/battery measurements and installer/UI checks remain pending; do not present the reported Windows stutter as conclusively resolved.

0.3.0/7: shared progress periods/data calendar and note organization implemented. Calendar has month/year jump, typed diary indicators and filters. Notes have folders/move/filter/view/stamp preferences; protocol 4 requires updated Android too. Practical device/UI/install checks remain open.

0.2.4/6: shared grouped settings/search, configurable progress charts, weekly/monthly demo and local reset are implemented for both clients. See ../DATA_RESET_AND_CHARTS.md; practical UI/install verification remains open. Installer ten-language behavior remains unchanged.

Installer language implementation (0.2.3/5): managed MSI initializes Unicode captions before UI; managed EXE embeds that same package. User UI language determines the default among ten codes, otherwise English. Regional forms resolve to their primary language. See INSTALLER_LANGUAGES.md; native Windows UAC/MessageBox button labels follow the OS.

2026-10-02 design update: follow Notio-Windows compact icon action panels and the same named palettes as Notio-Android. Use the shared FitEasy IconAction for translated hover/long-press hints and screen-reader descriptions; keep main navigation and destructive confirmation labels visible.

**Status: design placeholder, updated alongside Android planning.** Windows is a first-wave peer client alongside Android. It must implement shared behavior and data contracts without copying Android-specific UI or storage technology. Linux is planned later against the same contracts.

## Product goals

Выдача обязательно включает managed EXE/MSI и portable ZIP с runtime. Portable хранит data рядом с приложением, установленная версия — LOCALAPPDATA/FitEasy. См. RULES.md раздел 9 и desktopApp/portable/README-portable.md. Android выдача одновременно содержит debug/release APK.

Уточнение 2026-10-02: общие календарь/расписания и три вида заметок — SCHEDULES.md/NOTES.md. Локальная настройка закрытия в трей, один экземпляр на папку и диалог установщика — WINDOW_LIFECYCLE.md. Код подключён; практическая проверка Windows/Android остаётся задачей готовности.

Match Android tablet layout and navigation closely; phone uses a compact adaptation. Respect keyboard/accessibility and window resizing within the shared visual structure. Include the same detailed ten-language help.

- Provide the same core nutrition, water, activity, recipe, progress, and data portability semantics.
- Work locally and offline.
- Import/export the shared versioned backup format.
- Implement LAN pairing and send/receive/both with merge/copy/replace, interoperable with Android. Server/accounts come later.
- Use Windows-appropriate interaction patterns, keyboard support, and accessible layouts.
- Provide the shared localization coverage and language preference behavior defined in `../LOCALIZATION.md`.

## Candidate phases

1. Review this project's accepted domain model, food catalogue, backup, and sync contracts.
2. Select Windows UI/runtime and encrypted local database after requirements are known.
3. Implement profile, goals, food/recipe diary, hydration, manual activity, and progress.
4. Implement compatible export/import and sync, then validate cross-client scenarios.

## Shared contract requirements

- Use the same separate globally populated Product/Dish catalogues and stable source IDs as Android, with matching regional variants, provenance and update semantics.

- UUIDs, timestamps, units, nutrition semantics, tombstones, and revision handling match `DATABASE.md` and `SYNC.md`.
- Android-only details such as Room, Health Connect, Compose, WorkManager, and Android permission APIs stay behind adapters.
- Imported Health Connect data is represented as source-tagged activity data; Windows need not provide that source to preserve the record.
- Every feature/parity gap is tracked in `FEATURE_PARITY.md`.

## Open questions

D076 реализуется общим экраном/справкой Android/Windows: десять языков, локальное ознакомление, повтор после полного обнуления/wipe и сырого DB импорта. Документ LEGAL_NOTICE.md; обязательная ответственность по закону не исключается.

Windows versions, desktop-only versus packaged app, preferred UI framework, local encryption/key strategy, and expected shared catalogue update path remain unselected. Resolve with vibic777 before Windows implementation.

D079 / 0.4.5/25: общий Canvas ходьбы — опора назад/перенос вперёд, передний сгиб коленей с постоянной длиной ног и встречные руки. 117 JVM-тестов и Android компиляция успешны; 20 кадров полного цикла просмотрены. Прежний foreground/отключение и таймер, без новых записей/фоновых задач. Форматы данных прежние; физический просмотр/энергозамеры остаются открытыми.

D080 / 0.4.6/26: Windows держит одно соединение SQLCipher на время работы и закрывает его перед сменой шифрования, удалением данных и выходом; анимация раздела останавливается в окне без фокуса. Энергосбережение шагомера — только Android; Windows/Linux датчики не запускают, форматы SQL/LAN/backup прежние.

D081 / 0.4.7/27: общий рисовальщик — анатомический скелет и атлетичные фигуры, общий полноэкранный редактор каркаса на иконках с подсказками при наведении (Windows) и удержании (Android) на десяти языках. Данные сцены и форматы прежние.

D082 / 0.4.8/28: шапка приложения служит заголовком окна Windows (JetBrains Runtime CustomTitleBar, как в Notio-Windows); системные кнопки поверх, цвет по теме. Только Windows; общий код читает LocalWindowBar, Android/Linux без изменений.

D083 / 0.4.9/29: общий рисовальщик и редактор — кости кистей и ступней, изображение хватов, предмет в ладони, поворот головы, сгиб пальцев стоп, ручки направления; два упражнения для ступней. Новые поля позы необязательные, форматы прежние.

D084 / 0.4.10/30: общий значок лёгкого бега для раздела «Активность», чистые тона стилей и стили «Розовая» / «Фиолетовая». Одинаково на Android и Windows; форматы прежние.

D085 / 0.4.12/32: теги заметок и меню «Теги», сводка напоминаний подсказкой значка в трее, полный перевод текстов интерфейса на десять языков, русские названия части каталога продуктов. Общий код; форматы прежние, у заметки новое необязательное поле tags.

D086–D087 / 0.4.13/33: настройки «Уведомления и батарея» (на Windows — выключатель сводки в подсказке трея и режим экономии без анимаций; будильник поверх блокировки только Android), полноэкранные страницы настроек, защиты и синхронизации, вход по PIN и по логину с паролем. Общий код; форматы прежние.

D088 / 0.4.14/34: общий ToolDock, редактор каркаса и полотно рисунка на весь экран, стрелки в справочнике упражнений, подсказка с количеством продуктов и блюд. Общий код; форматы прежние.

D089–D091 / 0.4.15/35: кнопки-иконки с подсказками на всех экранах, названия каталога на девяти языках, полноэкранная страница напоминания. Шагомер был только на Android и вынесен из сборки; R8 касается только Android. Общий код; форматы прежние.

D092 / 0.4.16/36: меню внутри «Питания» и «Воды», новое меню настроек, PIN-панель, полный словарь каталога; в пакетах Windows драйвер базы без библиотек других систем. Общий код; форматы прежние.

D093 / 0.4.17/37: плашки выбора (категории, типы, режимы) показываются значками с подсказкой, как кнопки. Общий код Android/Windows; форматы прежние.

D094 / 0.4.18/38: справочник упражнений на весь экран, значки в ряд, режим «Показать упражнение». Общий код Android/Windows; форматы прежние.

D095–D096 / 0.4.19/39: упражнения двигаются при экономии батареи; экран входа и настройки защиты по образцу Notio, LockSettings.pinLength — локальное поле устройства. Общий код Android/Windows; форматы обмена прежние.

D097–D101 / 0.4.20/40: напоминания по образцу Notio (карточки, мелодия, громкость, нарастание), строка о режиме экономии батареи на экране упражнений, автоблокировка и готовые контрольные вопросы, страница синхронизации по образцу Notio, переводы подсказок и ошибок. Общий код Android/Windows. Reminder.soundUri/volumePercent/rampSeconds и LockSettings.timeoutSeconds локальны для устройства; LAN 6, Transfer 5, backup 5 и SQL 2 прежние. Отличия Windows: мелодия — звуки Windows или WAV/AIFF/AU, проигрывается один раз; службы переднего плана и системного выбора рингтона нет.

D102–D104 / 0.4.21/41: Windows рисует окно в режиме «Автоматически» (Direct3D, откат библиотеки; ручной выбор сохраняется); запись в базу только изменённых строк, схема SQL 2 и формат прежние; Reminder.noteId — локальная связь напоминания с заметкой; шифрование базы преобразуется вне потока интерфейса; меню настроек без стрелок; пункт трея «Заглушить напоминание» — только Windows. Общий код Android/Windows; LAN 6, Transfer 5, backup 5 прежние.

D105–D106 / 0.4.22/42: синхронизация повторяет Notio — движок SyncEngine/SyncStore (приём, пока открыта страница; пара по коду один раз; запрос на втором устройстве; проверка связи; адрес вручную; журнал; удаление пары на обоих устройствах), протокол LAN 7, порт 47821; формат Transfer 5 прежний; слияние — побеждает более новое изменение записи. Страницы «Безопасность и приватность» и «Резервная копия» повторяют Notio; пароль резервной копии и выбор «что напоминание показывает на экране блокировки» — секреты устройства. Общий код Android/Windows. Старые пары 0.4.21 не переносятся: устройства соединяются кодом заново, оба клиента обновляются вместе.

D107 / 0.4.23/43: корзина, справка, «Справка и о приложении» и блок шифрования базы повторяют Notio; значки — набор Notio (NotioIcons.kt). LocalData.trashRetentionDays (0 = никогда, по умолчанию) в JSON metadata SQL 2, старые записи читаются с 0; прежнее trashDays не используется. Общий код Android/Windows; LAN 7, Transfer 5, backup 5 прежние.

D108 / 0.4.24/44: LocalData.remindersOff — общий выключатель напоминаний устройства; раздел синхронизации «Напоминания» (Transfer.reminders, необязательное поле, версия Transfer 5 прежняя; мелодия не переносится); формы редактирования — полноэкранные страницы, на Windows слой во всё окно (LocalDesktopWindow) вместо диалога с полями. Общий код Android/Windows; LAN 7, backup 5, SQL 2 прежние; резервная копия .fiteasy напоминаний по-прежнему не содержит.

D109 / 0.4.25/45: у сохранённого соединения хранится заранее заданный выбор синхронизации (Pairing.askDirection/askMode/askFolders в секрете устройства sync-store). Общий код Android/Windows; протокол LAN 7 и форматы прежние.

D110 / 0.4.26/46: принимающее устройство не показывает вопрос, если его собственный заранее заданный выбор для соединения точно совпадает с запросом. Общий код Android/Windows; протокол LAN 7 прежний.

D111 / 0.4.27/47: запрос синхронизации с заданными заранее направлением, способом и разделами несёт отметку SyncMessage.preset, и второе устройство вопрос не показывает; при открытом выборе вопрос обязателен. Общий код Android/Windows; протокол LAN 7 прежний, обе стороны должны быть 0.4.27.

D112 / 0.4.28/48: один движок синхронизации на время работы приложения (SyncHost); переключатель «Постоянно ждать синхронизацию» — секрет устройства sync-always, по умолчанию выключен. Windows принимает синхронизацию, пока приложение запущено, включая трей; Android — пока приложение на экране, без фоновой службы. Протокол LAN 7 и форматы прежние.

D113 / 0.4.29/49: новая установка начинает с выключенными напоминаниями (LocalData.remindersOff=true только в первой записи новой базы; существующие данные не меняются), создание напоминания при выключенных сразу показывает подсказку «Включить / Не включать». Замещение при синхронизации возможно для выбранных разделов, по-прежнему только в одну сторону: в выбранных разделах принимающее устройство получает ровно принятое, остальные не трогаются (replaceSections). Reminder.updated — время изменения напоминания (0 у прежних): слияние берёт более позднее, мелодия устройства сохраняется. Общий код Android/Windows; SQL 2, LAN 7, Transfer 5, backup 5 прежние. Запрос выборочного замещения устройство до 0.4.29 отклоняет, поэтому обновлять оба.

D114 / 0.4.30/50: значок «Синхронизация» в верхней панели рядом с профилем открывает ту же страницу синхронизации, что и пункт меню настроек; пункт меню остался. Общий код Android/Windows; данные, протокол LAN 7 и форматы не менялись.

D115 / 0.4.31/51: проверка кода напоминаний на расход батареи и надёжность. Android: значения, запечатанные ключом устройства, открываются один раз на процесс; будильник переставляется только у изменённого напоминания; смена часов не снимает уведомления; при открытии приложения будильники ставятся заново; звук своей мелодии будит приложение только на время нарастания громкости; экран будильника показывает последнее сработавшее напоминание. Windows: после сна компьютера или смены часов таймеры ставятся заново, пропущенное не более получаса назад показывается один раз; таймер мелодии тот же, что на Android. Данные, SQL 2, LAN 7, Transfer 5, backup 5 не менялись. Новое разрешение Android WAKE_LOCK — только на время звучания мелодии.

D116 / 0.4.32/52: значок «Напоминания» в верхней панели рядом с синхронизацией и профилем открывает «Настройки → Напоминания», как будильник в верхней панели Notio. Подтверждение синхронизации: второе устройство не спрашивает только тогда, когда на запускающем заданы все три выбора; при хотя бы одном «выбирается при синхронизации» вопрос обязателен — исключение D110 (совпадение с заданным на принимающем устройстве) снято. Общий код Android/Windows; протокол LAN 7 и форматы прежние.

D117 / 0.4.33/53: подсказка значка держится 5 секунд (удержание пальца или задержка мыши) и по нажатию открывает справку на описании этого значка; справка — вкладки категорий, подвкладки разделов с обзором-меню и списком значков (рисунок, название, значение) на десяти языках. Описание — docs/HELP.md. Общий код Android/Windows; данные и форматы не менялись. Правило подтверждения синхронизации D116 проверено тестом на все восемь комбинаций.

D118 / 0.4.34/54: контекстный поиск в справке — результат показывает найденное внутри фразы с выделением, первыми идут результаты из категории текущего места, в открытом разделе искомое остаётся выделенным. Общий код Android/Windows, десять языков. Журнал для Linux (docs/linux/LINUX_PLAN.md) восполнен за D102–D118 и ведётся в каждой поставке.

D119 / 0.4.35/55: фигура упражнения — один целый человек в любой позе и с любой стороны. Плечи держатся на грудной клетке и наклоняются вместе с ней (rigTorsoFrame); силуэт соединяет плечи, бёдра и шею с туловищем, скелет — шею с позвоночником и бёдра с тазом. В редакторе виды: спереди, оба бока, сзади и сверху (RigPose.pitch, по умолчанию 0; rigProject с наклоном сцены). Общий код Android/Windows. Поле pitch необязательное: SQL 2, LAN 7, Transfer 5, backup 5 прежние; версия до 0.4.35 сцену читает, но показывает её без наклона.

D122–D126 / 0.4.36/56: установщик пишет каждый файл заново (REINSTALLMODE=amus), первый класс программы Launcher.java проверяет файлы class path и при пропаже сообщает на языке системы, код завершения 3 (D122). Мелодия напоминания: MP3 (mp3spi, jlayer) и OGG Vorbis (JOrbis, OggVorbis.kt) наравне с WAV, AIFF, AU; распаковывается только время звучания (D123). Ключ TOTP показан QR-кодом, ZXing core, общий код (D124). Запись базы — в своём потоке (SaveQueue), проверка и состояние в памяти сразу; перед закрытием и заменой файла запись дожидается; формат базы, LAN 7, Transfer 5, backup 5 прежние (D125). Обратный отсчёт экрана блокировки не тикает, когда окно свёрнуто (D121). В справочнике упражнений 15 карточек с описаниями техники, общий код (D126). На Windows руками не проверялось: установка поверх работающей программы с перезагрузкой, звучание MP3/OGG, считывание QR-кода с экрана.

D127–D128 / 0.4.37/57: «Настройки → Права» — общий код (RightsUI.kt, Platform.rights / grantRight / revokeRight / revokeAllRights). На Windows отдельных прав нет: страница открывает параметры уведомлений (ms-settings:notifications) и список разрешённых приложений брандмауэра, состояние приложению неизвестно; «Отозвать все права» показывается только на Android (D127). Заголовок окна и подсказка в трее — «ФитИзи» при русском языке Windows, иначе «FitEasy» (appSystemName); ярлык в «Пуске» и имя в списке программ остаются «FitEasy» (D128). База, LAN, backup не менялись.

D129 / 0.4.38/58: пакеты Windows собираются только на JetBrains Runtime SDK 25 — иначе шапка приложения не становится заголовком окна и Windows рисует свою полосу (так вышли 0.4.36 и 0.4.37). Упаковка и сценарий поставки это проверяют. Код не менялся.
D130 / 0.4.39/59: мелодия напоминания открывается только из папки звуков Windows и из `reminder_sounds` в папке данных; сетевой путь не трогается (адрес приходит и из импортированного файла базы). Чужой файл базы читается с `PRAGMA trusted_schema=OFF`. Запрет снимков экрана при блокировке и правила переноса данных — только Android.
D131: выводы по безопасности, в том числе по Windows (хранение секретов DPAPI, один экземпляр, запуск программ, библиотеки звука), ведутся в docs/SECURITY_REVIEW.md; изменение в этих областях записывается туда тем же шагом. Код не менялся.

D132 / 0.4.40/60: порт, на котором устройство ждёт синхронизацию, — автоматически (47821 или свободный) либо вручную (1024–65535); страница пишет, свободен порт или занят. Общий код (SyncEngine.portBusy, portFree, SyncStore.port); на Windows брандмауэр спросит разрешение для нового порта.

