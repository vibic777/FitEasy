# Feature Parity Ledger

D073/0.4.1/21: общий компактный RigEditor Android/Windows, жёсткие крепления бёдер/плеч, отдельные предметы/суставы и общий projected hit test. Иконки/подсказки десяти языков, закреплённая сцена, четыре панели. 102 JVM-теста и compilation успешны; реальная мышь/палец/маленький экран отдельно открыты. Форматы прежние.

D072/0.4.0/20: Windows меню трея использует общий AppNavigation, ожидающий разблокировки; Android меню трея не имеет, обычная навигация сохранена. Десять языков меню, активные напоминания открывают общий ScheduleHub со статусами. 96 JVM-тестов/Android compile успешны; реальное UI взаимодействие остаётся открытым. Новый каркас/полноразмерные заметки и обмен справочниками имеют общий код, но не закрывают все требования наполнения/редактора продуктов.

D070: общий интерфейс обмена справочниками в Настройки → Данные; одинаковые экспорт оба/продукты/блюда и импорт слияние/копии/замещение. JVM проверки и Android компиляция успешны; реальные файловые диалоги ещё требуют проверки. Azure F0/вход Microsoft остаются будущим планом на обоих клиентах.

D065 / 0.3.9/16: Android/Windows один офлайн справочник (20 карточек, 10 языков), выбор/собственные строки, редактируемые параметры и только один пример по нажатию. Локальный вариант обновляется событием; Canvas останавливается в фоне/при выключении/невидимости. Очистка после удаления managed Windows — платформенная; Android использует системное удаление приложения, portable сохраняет отдельную папку.

D063 / 0.3.7/14: shared set-aware editor/player, actual repetitions/time, set/next rest and recoverable draft. Android Keystore / Windows DPAPI; queued writes invalidated on save/discard/wipe. New labels include ten languages. Catalogue/silhouettes remain next work; real UI/kill recovery still requires devices.

0.3.6/13 D060–D062: routine row edit/copy, monotonic timing, off-UI password backup and transactional SQL migration are shared Android/Windows. Physical schema 2 is aggregate-normalized, portable Snapshot/backup/LAN unchanged. Hardware UI checks and remaining D058 features still open.

0.3.4/11: shared Help exports JSON/TXT/LOG; Windows and Android artwork has a white outer heart and transparent corners. Android alone offers counter/detector/software sensor modes and technical diagnostics; Windows only displays imported phone steps. No shared storage or transfer contract change. Hardware accuracy and launcher mask verification remain pending.

D058 design only: exercise catalog/program/set result/session draft are shared Android/Windows contracts; avatar/animation preferences stay device-local. Per-movement silhouette correctness and hardware efficiency must be verified separately. See EXERCISE_LIBRARY.md/TODO.

0.3.3/10: shared chart editor applies kind/color immediately and previews the selected renderer. All-kind action preserves individual colors. Same behavior Android/Windows; no storage/LAN/backup schema change.

2026-10-02, in-progress 0.3.2/9: shared LAN UI now groups Connection/Data/Options, device cards and expandable manual address. Same selection/direction/replace validation and protocol 4 on Android and Windows. Device visual and LAN verification pending; raw database and SMTP attachment additions remain TODO.

0.3.1/8 prerelease: shared five appearance styles (shapes, icon badges/strokes, card borders), vivid/custom RGB, per-section color/image backgrounds with multi-selection, optional finite transitions and metric animation. Settings search includes new preferences; both clients retain independent local Appearance. Android committed cache/sensor burst coalescing are platform-specific. UI initial loading and reminder polling run off the UI thread; foreground controls polling/animation. Device visual/performance checks remain open.

0.3.0/7: day/week/month/year/multi-year/all/custom ranges, reference date and direction; calendar marks diary types/counts excluding trash. Note folders/move/type filters/all-folders/cards-list-grid/time stamps are shared Android/Windows. Full contracts ../PROGRESS_PERIODS.md and ../NOTES.md. Update both clients for protocol 4 / Transfer 3 / backup 3. Device visual checks remain open.

0.2.4/6: shared Android/Windows settings menu/search includes protection and LAN; eight per-metric chart colors, bars/line/area/dots and global style; weekly/monthly demo; force demo removal by local UUID, two-confirmation full reset and category reset. Appearance/security/backups retained; recipe dependencies kept and removed-plan reminders detached/disabled. See ../DATA_RESET_AND_CHARTS.md. Device UI checks remain pending.

Windows installer 0.2.3/5: one managed EXE/MSI wizard supports en/ru/be/fr/de/es/it/zh/ja/ko, picks Windows UI language and falls back to en. Unicode caption/action tests run through MSI; data/Android contracts are unchanged. The EXE embeds the final MSI and supports /quiet /passive /norestart /lang=code.

Demonstration and cleanup share one Android/Windows implementation: UUID + original SHA-256, preserve edits/user records, disabled reminder examples, protect schedules referenced by retained reminders. Two JVM tests pass (2026-10-02); practical UI/device checks remain pending.

2026-10-02: shared icon action panels and Notio-derived light/dark accent palettes are compiled for Android and Windows. Tooltips use the selected app language and accessibility labels. Actual hover/long-press and wide-screen visual checks remain pending.

**Status: initial plan.** A check means planned, not implemented. Linux is a later client and will follow the same shared contracts. Platform-specific integration means a capability may differ while preserving shared records and meaning.

| Feature | Android | Windows | Shared behavior / note |
|---|---|---|---|
| Onboarding and preferences | Planned | Planned | Units, locale, privacy choices |
| Profile and goals | Planned | Planned | One personal profile; optional setup; calculations require inputs |
| Today dashboard | Initial scope | Initial scope | Nutrition/water/steps/activity/goals; quick add; device-local hide/reorder |
| Primary navigation | Initial scope | Initial scope | Six sections including separate Water; platform-appropriate layout |
| Wide-screen layout | Initial scope | Initial scope | Tablet/Windows close visual and navigation parity; compact phone adaptation |
| Detailed help | Initial scope | Initial scope | Ten languages; offline essential content, search and contextual links |
| App/internal icons | Initial scope | Initial scope | Shared visual identity; platform-specific launcher assets |
| Calendar/history | Initial scope | Initial scope | Past-day edits/backdating; food/workout copy with new IDs and explicit destination |
| Product catalogue and search | Planned | Planned | Separate catalogue; replenishment, custom entries, stable IDs/provenance |
| Dish catalogue and search | Planned | Planned | Separate prepared-food catalogue; declared or recipe-derived nutrients |
| Search filters, recents, favorites | Initial scope | Initial scope | Text/category/cuisine discovery across separate catalogues |
| Barcode lookup | Initial scope | Initial scope | Android camera scanning; Windows manual entry/scanner input, camera support to be specified |
| Food diary | Planned | Planned | Calories/BJU, known fiber; editable meal categories/servings; stable nutrition snapshots |
| Custom foods and recipes | Initial scope | Initial scope | Ingredients/steps/yield/portions; whole/100g/portion nutrition; save recipe as Dish |
| Water and drinks | Planned | Planned | All drinks; quick/custom volume; one entry feeds nutrition; selectable water-only/all-drinks goal and separate totals |
| Manual walks/activity | Planned | Planned | Shared activity records |
| Health Connect step import | Initial scope; optional permission | Not applicable | Android data source; shared records remain usable |
| Walk timer | Initial scope | Initial scope | Manual/timed records with overlap handling |
| Phone-native pedometer | Initial scope | Display synchronized results | Capability/permission dependent; source deduplication |
| Guided low-impact sessions | Initial scope | Initial scope | Built-in/custom routines; ordering, duration/repetitions/rest; completion snapshots |
| Measurements and trends | Initial scope | Initial scope | Weight/waist/hips/chest; weekly/monthly charts; optional metrics and device-local visibility |
| Adult BMI/energy tools | Planned, adult-only | Planned, adult-only | Manual/automatic calculation; confirmation default, optional auto-apply; inputs/history |
| Backup and restore | Initial scope | Initial scope | `.fiteasy`; optional password/appearance; merge/copy/replace |
| Direct LAN device sync | Initial scope | Initial scope | Section checkboxes/All data; send/receive/both; merge/copy/replace |
| Sync conflict review | Initial scope | Initial scope | Local/incoming/both; optional automatic policy for defined simple records |
| Appearance/language/reminders | Device-local | Device-local | Excluded from every sync mode, including replace |
| Theme customization | Initial scope | Initial scope | Calm default; system/light/dark; palettes/custom color; text size; background images |
| Trash | Initial scope | Initial scope | Restore/configurable retention; dependency-safe deletion and durable sync tombstones |
| Units | Initial scope | Initial scope | kg/lb, cm/in, ml/l/US and Imperial fl oz, km/mi, kcal/kJ; device-local display preferences |
| Default units | Initial scope | Initial scope | Liters for drink totals, kilograms for weight, grams for food |
| PDF/CSV reports | Initial scope | Initial scope | Selected sections/date range; separate from restorable backup |
| Database encryption toggle | Initial scope | Initial scope | Off by default; safe enable/disable conversion; independent device keys |
| App lock, question, SMTP and TOTP recovery | Initial scope | Initial scope | Device-local credentials; OS-specific biometric support |
| Wrong-attempt delays/limits | Initial scope | Initial scope | Notio defaults; recovery-only or opt-in local wipe with typed confirmation |
| Server/account sync | Later | Later | Future shared transport |
| Notifications/reminders | Initial scope | Initial scope | Water/meals/walk/warm-up/measurements; days/time, quiet hours, snooze; device-local OS scheduling |
| Flexible reminder recurrence | Initial scope | Initial scope | Day/week/month rules; shared meaning, device-local schedules |

Update this table whenever a feature, shared entity, or contract changes. Record deliberate platform gaps with rationale.

## Implementation update 2026-10-02

Structured routines, session controls and immutable workout results share the same Compose code and serialization on Android/Windows. Both preserve imported phone/Health Connect daily records and select one step source. Windows has no native phone sensor or Health Connect provider; collection happens on Android and records transfer through LAN/backup. Android's raw phone counter remains device-local. Windows reminders still require the app process to remain open. Full feature parity has not been achieved.

The shared reminder editor includes date boundaries and snooze duration. Windows now keeps its process running in the tray when its window closes, with Open/Exit/Snooze commands; startup after login is not implemented. Progress supports 7/30-day periods and device-local hidden metrics on both platforms.

Both clients share nutrient/weight goals with pause/disable, selected-period PDF/CSV, optional normalized background images and unit conversions for diary/profile/goal input. Windows PDF pages are raster; Android uses native PdfDocument. Reports retain canonical units. See UNITS.md for remaining surfaces and MVP_STATUS.md for readiness gaps.

0.3.8/15: общий конструктор/плеер содержит оригинальные векторные силуэты, локальный выбор варианта и экономичный Canvas; общий первый свободный номер автоимени/пять тем без возрастных чисел. Windows состояния напоминаний обновляются по событиям. Android разрешения/шторка платформенные; Windows сохраняет tray/beep и требование работающего приложения. Звуковая служба Notio целиком ещё не перенесена; аппаратные проверки открыты.
# Уточнение 2026-10-02: расписания, заметки, окно

Общий Compose UI: календарь месяца/повестка, произвольные расписания и строки, привязка локального напоминания, состояния/включение/подтверждение/откладывание/прерывание. Заметки: текст, свободное рисование пером, список с галочками/комментариями/порядком, корзина. Android — AlarmManager/notification actions; Windows — executor/tray actions и системный beep, без аппаратной вибрации. Окно Windows: локальная галочка закрытия в трей и IPC второго запуска. Managed MSI/EXE добавляют интерактивное закрытие до замены файлов. Реальная установка/уведомления ещё не проверены.
# Portable выдача 2026-10-02

Тот же Windows клиент/UI/БД, ZIP полного app image со встроенным runtime. Marker portable.txt выбирает data рядом с EXE; отдельный instance.lock и настройки. DPAPI остаётся привязанной к Windows пользователю/машине. SharedData/бэкап/LAN не меняются. Проверка запуска из распакованного архива ещё требуется.
# 0.3.10/17

0.4.4/24 D077/D078: общие верхняя/правая панели шести разделов с подсказками/подсветкой, ходьба и заметки на существующем Canvas; боковой профиль, контакты пола и подъём тела в новых шаблонах упражнений. Android/Windows одинаковы, старые сцены не переписываются. Растры/численная геометрия и headless Compose проверяются; реальные мышь/телефон/батарея остаются отдельной проверкой.

0.4.4/24 D076: десять полных юридических уведомлений, первая статья справки и полноэкранное ознакомление до дневника. Полная очистка повторяет экран, частичная сохраняет ознакомление. Общие UI/LocalData Android/Windows; Linux использует тот же контракт. Юридическая проверка переводов по странам остаётся открытой.

0.4.3/23 D075: более стройный женский контур груди/талии/бёдер и короткая шея обоих персонажей общие для Android/Windows; исходные каркасы и совместимость не меняются.

0.4.2/22 D074: общие векторные фигуры с открытой шеей, женскими топом/мини-юбкой и мужскими майкой/шортами. Фронт/профиль/затылок и наклон головы одинаковы на Windows/Android. Новых форматов данных или кадровых таймеров нет.

Оригинальные цветные взрослые фигуры и прежние техники общие для Windows/Android. События доставки выделены в Platform.reminderChanged: Windows использует свой событийный scheduler, Android не перечитывает напоминания при изменении шагов. Форматы SQL/LAN/backup прежние.
# 0.3.11/18

0.3.12/19: коррекция женских пропорций общей векторной фигуры, без изменения мужской фигуры, контракта данных или частоты анимации.

Общий NotesHub на Android/Windows: прямой вход в папки/возврат, все заметки независимо от папок, локальная сортировка каждого контекста и папок, иконки с десятью языками подсказок. SQL/LAN/backup прежние; папки пока плоские.

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
