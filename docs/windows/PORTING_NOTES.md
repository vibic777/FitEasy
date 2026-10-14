# Windows Porting Notes

D073: RigInteraction разделяет hit test и drag суставов/предметов, использует render projection; крепления структурные. Общий Kotlin, без новых runtime/фоновых сервисов. Заголовки Canvas кешируются по тексту, undo ограничен 50 снимками и один на жест; persistence только на save.

D072: AWT PopupMenu/TrayIcon вызывает общий событийный запрос AppNavigation; очередь держит последний запрос до входа, потребление по token исключает очистку новой команды старым обработчиком. Меню обновляется на EDT по изменению языка/статусов, без таймеров. При отсутствии SystemTray действует обычное окно. Linux позднее сможет подключить тот же контракт; Android по умолчанию имеет пустой Flow. Никаких изменений физической схемы/переноса для трея.

D065: shared offline catalogue/figure render and click preview need no raster downloads/runtime dependencies; Linux can reuse them. Windows uninstall uses the current user's standard data path, not a synced data change. Test cleanup conditions without deleting the real development data; interactive removal remains a separate user-machine check.

D063: WorkoutDraftStore saves small snapshots through sealed local secrets rather than mutating shared DB each timer tick. Epoch rejects queued writes after save/discard/wipe. Set fields/results/state machine are common Kotlin; transfer versions refuse older peers.

D062: JdbcStateSql adapts shared aggregate migration/storage to SQLiteMC SQLCipher. Migration owns one transaction; ordinary writes use the caller's transaction and last committed snapshot. Raw import is query_only and supports physical schemas 1/2 without mutating source. Connection closes on migration failure. D060/D061 share monotonic timing and IO backup work with Android.

D058 planned: monotonic exercise/rest state machine, pause/resume and local recovery draft shared with Android. Vector silhouettes with individual key poses; animate only visible selected card, max 20 fps, respect disable/background. Windows does not infer physical repetitions from animation.

0.3.3/10: chart editor removes stale draft state and uses current appearance with immediate save. Production drawProgressSeries is shared and raster tested for four distinct kinds.

0.3.2/9: default SOFTWARE_FAST rendering may avoid VMware virtual GPU stalls; user can choose Direct3D/OpenGL, restart required. Actual cursor improvement is unverified. Phone tracking controls are Android-only; imported/manual steps remain. Shared journal, grouped LAN UI, database/password/SMTP tools available; native fatal crashes cannot guarantee Java journal execution.

0.3.1/8: initial state/device ID/catalogue and background decoding are off the UI thread; daily entries/goals/catalogue filters/color schemes/hint dictionaries are remembered. No-op save does not reopen/write SQLite. Reminder status polls in IO every 2 seconds only while foreground; manual walking uses monotonic elapsed time, with no hidden-window UI tick. Five styles use Material shapes plus original decorated icons/card borders. Finite animations are optional/default off, disabled in background. jdk.jfr is bundled for manual diagnostics; no automatic recording. Synthetic benchmark is not a mouse/GPU benchmark. APPEARANCE.md/PERFORMANCE_REVIEW.md record limits.

0.3.0/7: calendar dates ISO and inclusive, month/year use java.time; multi-year aggregation is bounded, records grouped before bucket lookup rather than scanned for every date. Lazy note rows render only visible cards; drawing thumbnails cap preview stroke/point rendering without modifying saved artwork. Stamps use locale and device timezone; shared timestamps stay epoch ms. Note folder contracts/old null timestamps/backup reader documented in ../NOTES.md. Note editor/canvas behavior remains portable.

0.2.4/6: the settings entry owns protection/LAN callbacks; global text toolbar removed. Reset calls atomic mutateSnapshot, stopping Android step tracking before activity deletion. Platform save reconciles reminder schedulers to disable/remove affected alarms. Chart preferences stay local and backup appearance is optional. See ../DATA_RESET_AND_CHARTS.md.

Installer 0.2.3/5 uses Windows-only .NET Framework 4 custom actions and a thin EXE around the final MSI. Language policy is en/ru/be/fr/de/es/it/zh/ja/ko from user UI language, unknown → en. This dependency does not enter shared contracts; Android/Linux packaging remains independent. Standard translations/DTF license MS-RL is included in packaged licenses.

2026-10-02: shared demonstration creation/cleanup and slider persistence after drag completion apply to both clients. Windows caches committed snapshots; Android sensor handling uses its own thread and requests five-second hardware batching. Device performance/battery measurements remain pending.

Keep a record of decisions that affect implementing shared FitEasy behavior on Windows.

## Current guidance

- Port product semantics and domain rules, not Compose screens or Android framework classes.
- Preserve food IDs, provenance, units, nullable nutrient values, and diary snapshots.
- Health Connect is an Android data provider; Windows can show and synchronize source-tagged imported activity without implementing Health Connect.
- Windows local storage and encryption are open decisions. It must support the same backup format and sync behavior.
- Review `FEATURE_PARITY.md` and `DATABASE_COMPAT.md` before changing shared contracts.

## Decision log

No Windows implementation decisions accepted yet. Add dated decisions here when target OS support, UI framework, storage, packaging, or integration choices are made.
# Реализация 2026-10-02

Windows-клиент реализуется в desktopApp, общие модели/UI в shared/src. JVM Compose Desktop, SQLCipher-compatible JDBC и DPAPI. Собираются EXE/MSI. Реализованные доменные экраны используют тот же код, что Android; отдельные адаптеры обеспечивают файлы, защищённые секреты и уведомления. Напоминания пока требуют запущенного приложения. См. ../MVP_STATUS.md и ../DECISIONS.md для актуального состояния и ограничений; исходные предложения ниже не являются доказательством готовой функции.

# Расписания и заметки, 2026-10-02

Контракты/редакторы в shared/src. Runtime reminders Android защищены Keystore; Windows reminder-runtime — DPAPI. Прерывание останавливает текущую серию, future primary остаётся; выключение/удаление отменяет primary/follow. ОС контролирует выдачу уведомлений; нет custom melody/volume/ramp/fullscreen. Рисунок использует квадрат и координаты 0..1, не экранные пиксели. Заметки нужно сохранять перед фоном/блокировкой: восстановление незавершённого редактора пока отсутствует. IPC один экземпляр реализован независимо от Notio; установщик не применяет Stop-Process.

# Portable 0.2.1

packagePortable архивирует createDistributable, marker/readme/licenses; packageDelivery зависит от всех пяти форматов. Корень определяется относительно расположения JAR app image. fiteasy.home переопределяет выбор для разработки; текущий рабочий каталог не используется. Папка должна быть доступна для записи. Межмашинный перенос ключей DPAPI не поддерживается; используйте .fiteasy.
# D074 — 0.4.2/22

D077/D078 (0.4.4/24): верхние/правые навигационные иконки используют MainNavigationIcons/mainSections и один screen; правая колонка отдельно прокручивается, без нижней дублирующей панели. Ходьба/запись заметки — общие векторы/существующий clock. Новые контактные шаблоны и боковой renderer общие, протоколы прежние. Headless pointer-тест ширин 360/960 проверяет смену выбранного раздела; аппаратную UX проверку не заменяет.

D076 (0.4.4/24): общий LegalNoticeScreen после загрузки и разблокировки, до дневника; язык выбирается сразу. Версия ознакомления хранится локально в metadata БД, запись в IO транзакцией с сохранением параллельных изменений. Не переносится LAN/.fiteasy; сырой DB импорт сбрасывает флаг. Новых polling/таймеров нет.

D075 (0.4.3/23): узкая женская талия — точка контура, топ повторяет грудь/талию, конечности тоньше, юбка меньше расклешена. Визуальная голова обоих персонажей приближена к плечам; исходные суставы и их редактор прежние. Общий renderer Android/Windows, переносимый контракт Linux не меняется.

Одежда, волосы, шея и ракурсы лица исправлены в shared RigDrawing, одинаково для Android/Windows. Данные и таймеры прежние; Linux сможет использовать тот же renderer. Новых библиотек и ресурсов нет. Растровые проверки не заменяют проверку на физическом устройстве.

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
