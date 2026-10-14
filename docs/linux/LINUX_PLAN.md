# FitEasy-Linux Plan

D063: set fields/state machine/results, LAN 5/Transfer 4/backup 4 are portable. Provide sealed local draft storage; restore pauses. Linux remains future scope.

D062: implement StateSql for Linux JDBC to reuse aggregate schema 2 and migration 1→2; keys remain platform-specific. Monotonic clock and backup jobs are already portable Kotlin/JVM. Linux implementation remains future scope.

0.3.4/11 compatibility: journal JSON/TXT/LOG export is portable shared code; Linux does not run Android sensors. White heart artwork is available from the native icon generator. Shared entity/backup/LAN versions are unchanged.

D058 planned: same portable exercise/program/set/result models and vector animation assets; device-local avatar/animation preferences. Linux notification/sound and monotonic clock adapters must preserve shared execution semantics. Implementation remains future work.

0.3.2/9 shared contracts: grouped LAN UI, bounded privacy-safe journal, database staging tools and SMTP MIME attachments are portable. Linux must supply secure key storage, raw SQLCipher v4 adapter and lifecycle visibility; no phone sensor tracking. Windows SOFTWARE_FAST default is platform-specific. Linux client remains future work.

0.3.1/8: future client should retain local Appearance.style/animations/sectionBackgrounds/backgroundImages and validate image references/bounds consistently; optional appearance backup stays envelope 3. Independent system/light/dark modes and RGB/custom palettes remain portable; finite animations must stop while backgrounded. Android sensor worker and Windows jdk.jfr are platform-specific.

Portable contracts updated 0.3.0/7: progress ranges use ISO dates/calendar arithmetic, epoch note/folder stamps display in device timezone, flat folders use stable UUIDs. Future client must support LAN protocol 4 / Transfer 3 and backup writer 3 / reader 1–3. Appearance.noteDisplay remains local. Linux runtime/UI remain future work.

**Status: future client; not in the first implementation wave.** Linux should reuse FitEasy's platform-neutral domain meaning, localization keys, versioned backup, and sync protocol.

Before implementation, choose supported distributions/packaging, UI toolkit, encrypted local storage and key management, auto-update strategy, and accessibility expectations. Record feature parity and data compatibility with Android and Windows. No Linux stack is selected yet.

D097–D101 (0.4.20/40): для Linux понадобятся свои выбор и проигрывание мелодии напоминания (Platform.pickReminderSound, previewReminderSound, ringing, stopRinging) и признак открытого системного диалога (Platform.pickerOpen); общий интерфейс, словари и расчёт нарастания громкости уже платформенно-нейтральны.

## Журнал для Linux: что повторить

Правило (RULES.md, раздел 2): каждое изменение записывается сюда в той же поставке — что уже общее и достанется Linux без работы, и что придётся сделать заново. Журнал отставал: после D101 записей не было; 08.10.2026 пропуск D102–D118 восполнен ниже по docs/DECISIONS.md. Стек Linux по-прежнему не выбран; если это будет настольный клиент на том же Kotlin/Compose, строки «общий код» достанутся ему как есть.

Что Linux должен дать сам (сводно, по интерфейсу Platform):

- Хранилище секретов устройства (на Android — Keystore, на Windows — DPAPI): пары синхронизации и её журнал (sync-store), sync-always, язык, переключатели уведомлений, пароль резервной копии, состояния напоминаний. Кандидат — Secret Service / libsecret; решить, что делать без него.
- База SQLCipher v4 через свой адаптер StateSql, ключ базы — в том же хранилище секретов.
- Напоминания: таймеры, показ уведомления, звук, значок в области уведомлений и его меню, поведение при закрытии окна.
- Выбор файлов и мелодий, экспорт и импорт, отчёт PDF, подготовка фоновых изображений.
- Признак «приложение на экране» (Platform.foreground) и имя устройства (Platform.deviceName).
- Упаковка и удаление с выбором «удалить данные», по умолчанию «нет» (RULES.md, раздел 9).

По решениям:

| Решение | Общий код, достанется без работы | Повторить в Linux |
| --- | --- | --- |
| D102 — скорость сохранения и отрисовки | Запись только изменённого (SqlStateStore), кеш переводов | Выбрать способ отрисовки по умолчанию и проверить его на Linux; одно соединение с базой на процесс |
| D103 — напоминание принадлежит заметке | Reminder.noteId, переход к заметке | Открывать заметку из уведомления |
| D104 — меню настроек, пункт «Заглушить» | Меню настроек | Пункт «Заглушить напоминание» в меню значка области уведомлений |
| D105 — движок синхронизации Notio | SyncEngine, SyncSession, SyncStore, обнаружение JmDNS, порт 47821, LAN 7 / Transfer 5 | Хранение sync-store в секретах; проверить mDNS и брандмауэр дистрибутива; Platform.multicast не нужен |
| D106, D107 — резервная копия, безопасность, корзина, справка | Страницы и форматы (backup 5) | Диалоги выбора файла; экспорт и импорт файла базы; пароль базы в хранилище секретов |
| D108 — выключатель напоминаний, раздел «Напоминания» в синхронизации, страницы во весь экран | LocalData.remindersOff, Transfer.reminders | Планировщик должен снимать таймеры при выключателе; страница — слой во всё окно, как на Windows |
| D109–D111, D116 — заданный выбор соединения и правило подтверждения | Целиком общий код (Pairing.ask*, allSetBeforehand, SyncMessage.preset) | Ничего |
| D112 — постоянное ожидание синхронизации | SyncHost | Решить, как на Windows: принимать, пока приложение запущено, в том числе свёрнутое в область уведомлений; вопрос при скрытом окне |
| D113 — напоминания выключены после установки, выборочное замещение, время изменения напоминания | SqlStateStore.newInstallation, replaceSections, Reminder.updated | Ничего сверх своего адаптера базы |
| D114, D116 — значки синхронизации и напоминаний в верхней панели | Общий код | Ничего |
| D115 — экономия батареи и надёжность напоминаний | ringTickDelay, OpenedTexts, расчёт сводки | Таймеры после сна компьютера: сверка часов раз в минуту и повторная установка (на Windows — DesktopReminders.beat); проигрывание мелодии с таймером только на время нарастания; открывать секреты один раз на процесс, если хранилище секретов медленное |
| D117 — подсказка на 5 секунд и справка вкладками | Hint.kt, HelpTree.kt, HelpPage, тексты на десяти языках | Проверить наведение мыши и долгое нажатие на сенсорном экране; слой подсказки не должен забирать фокус |
| D118 — контекстный поиск в справке | searchHelp, helpMatches, выделение найденного | Ничего |
| D119 — целая фигура в любой позе; виды с обоих боков и сверху | RigInteraction.rigTorsoFrame/rigAttached, RigDrawing, RigPose.pitch, rigProject/rigUnproject, значки и подписи редактора | Ничего: рисунок и редактор общие |
| D120 — правило: подсказки и справка ведутся вместе с кодом | RULES.md, AGENTS.md (кода нет) | То же правило действует для Linux: подсказка, раздел справки, переход и поиск для каждой функции |
| D121 — правила работы в фоне и расхода батареи | docs/BATTERY.md, RULES.md, AGENTS.md (кода нет) | Для Linux действуют правила 1, 10 и 14: таймеры ждут, а не опрашивают; срок пересчитывается после сна компьютера |
| D121, сверка — отсчёт экрана блокировки только на экране; экран будильника закрывается сам | LockGate(foreground), alarmScreenMillis | Передавать в LockGate, видно ли окно; экран будильника на Linux — если будет, с теми же 60 / 15 / 5 секундами |
| D122 — файлы программы проверяются при запуске | Нет (Launcher.java в desktopApp, REINSTALLMODE — только MSI) | Свой первый класс или сценарий запуска: проверить файлы пакета и сказать на языке системы; для deb/AppImage обновление поверх работающей программы проверить отдельно |
| D123 — мелодии MP3 и OGG | DesktopSound.decode, OggVorbis.kt (в desktopApp, чистая Java) | Взять те же классы и библиотеки (mp3spi, jlayer, jorbis — LGPL, отдельными jar); проверить вывод звука javax.sound на PipeWire/PulseAudio |
| D124 — ключ TOTP QR-кодом | QrCode.kt, DeviceLock.totpUri, ZXing core | Ничего |
| D125 — запись базы в своём потоке | SaveQueue, Platform.saveFailed / flushSaves, проверка только новых записей дневника | В своём адаптере базы: отдать запись в SaveQueue, отдельная блокировка соединения, flush перед закрытием и заменой файла, при отказе вернуть состояние и сообщить |
| D126 — 15 карточек упражнений с описаниями | ExerciseTexts.kt, rigTemplate, ExerciseCatalogue | Ничего |
| D127 — «Права» в настройках: выдача и отзыв | RightsUI.kt, RightsStrings.kt, RightsHelp.kt, AppRight, методы Platform.rights / grantRight / revokeRight | Решить, что показывать: порталы xdg-desktop-portal (уведомления, фон) для Flatpak либо, как на Windows, места настроек системы; вернуть список из rights() |
| D128 — имя приложения по языку системы | appSystemName | В .desktop-файле: Name=FitEasy и Name[ru]=ФитИзи; заголовок окна — appSystemName |
| D129 — упаковка только на JetBrains Runtime | Проверка в desktopApp/build.gradle.kts | Собирать пакет Linux на JetBrains Runtime SDK 25 для linux-x64; проверить, поддержан ли там свой заголовок окна, иначе оставить системный |
| D130 — проверка безопасности | DesktopSound.own, `PRAGMA trusted_schema=OFF` в readDatabase | Мелодия — только из своей `reminder_sounds` и из папки звуков системы (например `/usr/share/sounds`); пути вне их и адреса не открывать. Файл базы читать с `trusted_schema=OFF`. Секреты — через Secret Service, права файлов данных 0600 |
| D131 — выводы по безопасности в отдельном файле (правило) | docs/SECURITY_REVIEW.md, RULES.md, AGENTS.md (кода нет) | При появлении клиента Linux пройти 12 пунктов «Что проверять», добавить строки Linux в «Состояние по областям» и библиотеки Linux в таблицу |
| D132 — порт этого устройства: автоматически или вручную | SyncStore.port, SyncEngine.portBusy / portFree / validPort, portStatus, PortDialog | Общий код, работает как есть; в документации Linux указать, как открыть порт в брандмауэре (ufw, firewalld) |
