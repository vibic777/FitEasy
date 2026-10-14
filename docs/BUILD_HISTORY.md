# История сборок FitEasy

## 0.3.7 / 14 — 2026-10-03

Исходники `4f9721b1a081a5f3ea1effc3b7dbbebd20eaaba0`, vibic777. D063: подходы и фактические результаты/защищённый черновик, новые десять словарей и справка, LAN 5/Transfer 4/backup 4. 69 тестов, Android/Windows компиляция 18 с. Все пять пакетов: 1 мин 33 с. APK signatures (оба Android Debug/test), metadata/Windows BuildInfo, SHA-256, ZIP CRC/runtime/no user data и managed installer languages/checkbox/silent conditions проверены. Установка не запускалась; устройства не проверены. Размеры/хеши в `dist/2026-10-03-0.3.7/manifest.json`.

## 0.3.6 / 13 — 2026-10-03

Исходники `a2f13074d2689fefd61b37d43fe718da75de038c`, vibic777. Шагомер/backend/диагностика, JSON/TXT/LOG, сердечный значок, правка строк комплексов/монотонный таймер, backup в IO и физическая схема SQL 2. 60 тестов прошли; Android/Windows Kotlin скомпилирован за 19 с. Пять пакетов собраны за 1 мин 29 с (101 задача, 36 выполнено, 65 актуальны).

| Пакет | Байты |
| --- | ---: |
| Android debug APK | 48053535 |
| Android release-test APK | 38707116 |
| Windows managed EXE | 91331584 |
| Windows managed MSI | 91156480 |
| Windows portable ZIP | 89514058 |

`dist/2026-10-03-0.3.6/manifest.json`: SHA-256 сверены для всех пяти файлов; APK проверены apksigner, оба Android Debug/test, SHA-256 сертификата `e146abb83b22728ed8ab345d0d8443902505c539dd93b2ed52219d9d73b0f979`. Windows BuildInfo и Android metadata совпадают 0.3.6/13/2026-10-03. ZIP: 291 запись, CRC/runtime/portable marker, без DB/ключей/секретов/журналов. Установщик: десять языков, региональные варианты, реальный ru и fallback en, Unicode/галочка/порядок, точное включение MSI в EXE и silent-аргументы проверены без установки. Устройства/Huawei/реальные лаги/SMTP/LAN/интерактивная установка не проверены.

## 0.3.3 / 10 — 2026-10-02

Исходники `b52cfa49407aff5f8bb1f752e844f53ff8c3eb37`, vibic777. Исправление немедленного применения вида/цвета графиков с примером. 40 тестов без ошибок; финальная Android/Windows компиляция 14 с. Пять пакетов + verifyManagedInstallerLanguages: 1 мин 31 с, 101 задача (32 executed, 69 up-to-date).

| Пакет | Байты |
| --- | ---: |
| Android debug APK | 47938731 |
| Android release-test APK | 38625076 |
| Windows managed EXE | 91226624 |
| Windows managed MSI | 91058176 |
| Windows portable ZIP | 89421504 |

Версии Android/Windows 0.3.3/10 совпадают, оба APK подписаны Android Debug certificate SHA-256 `e146abb83b22728ed8ab345d0d8443902505c539dd93b2ed52219d9d73b0f979` (release — тестовая подпись). SHA-256 файлов в `dist/2026-10-02-0.3.3/manifest.json`. ZIP полностью проверен: 291 запись, runtime/portable marker, без пользовательских баз/ключей/логов. Десять языков MSI/EXE и fallback en проверены без установки. Реальная UI-проверка графиков на устройствах не выполнена.

## 0.3.2 / 9 — 2026-10-02

Исходники приложения: `b9d1115214db8f254e4178f98170833c9b5b7ab4`, vibic777. Все пять пакетов в `dist/2026-10-02-0.3.2`; SHA-256 записаны в manifest.json. Gradle `:desktopApp:verifyManagedInstallerLanguages :desktopApp:packageDelivery` прошёл за 1 мин 26 с, 101 задача (36 executed, 65 up-to-date). 38 JVM-тестов без ошибок; финальная debug Kotlin компиляция прошла за 13 с.

| Пакет | Байты |
| --- | ---: |
| Android debug APK | 47938731 |
| Android release-test APK | 38625076 |
| Windows managed EXE | 91226624 |
| Windows managed MSI | 91058176 |
| Windows portable ZIP | 89421731 |

Оба APK: версия 0.3.2/build 9, Android Debug certificate SHA-256 `e146abb83b22728ed8ab345d0d8443902505c539dd93b2ed52219d9d73b0f979`; release использует тестовую подпись. Windows BuildInfo совпадает. ZIP проверен полностью (291 запись), runtime/portable marker есть, базы/ключи/логи отсутствуют. Проверка MSI/EXE подтвердила десять языков, русский UI системы, fallback en, точное вложение MSI и условия silent/галочки; установка не выполнялась. Устройства/мышь/телефон/батарея/реальная LAN/SMTP требуют практической проверки. Нормализация хранения и остальные ограничения полного MVP остаются в TODO/MVP_STATUS.

## 2026-10-02 — 0.3.1, сборка 8, пререлиз

Исходники `9cbe1b03a75dbb3fb811f3bbf531f2281bf15f6c`, **vibic777**. Пять тем/насыщенные цвета/RGB/фоны выбранных разделов/отключаемые конечные анимации, IO-загрузка/кеш/no-op/объединение пачек шагомера/оптимизация опроса в фоне. 27 JVM-тестов без ошибок; компиляция Android/Windows прошла. Итог packageDelivery + verifyManagedInstallerLanguages: BUILD SUCCESSFUL за 1 мин 54 с, 101 задача (38 выполнены / 63 up-to-date).

Выдача `dist/2026-10-02-0.3.1`: APK debug 47692955 байт, release-test 38461212, EXE 91066880, MSI 90898432, ZIP 89260590. Версия/сборка обоих APK и Windows BuildInfo — 0.3.1/8. Обе APK-подписи проверены Android Debug. Пять размеров/SHA повторно проверены; ZIP CRC/launcher/runtime/jdk.jfr/лицензии проверены, пользовательских баз/секретов/ключей нет. Managed EXE содержит финальный MSI, проверка десяти языков/Unicode/fallback/silent/галочки прошла без install/execute.

Подробности и синтетический замер в ИСПРАВЛЕНИЯ.md. Реальное устранение задержек мыши/FPS, UI устройств, батарея, установка/upgrade и LAN ещё не подтверждены. Android по ADB не подключён. Это пререлиз, не заявление о завершении всего согласованного MVP.

## 2026-10-02 — 0.3.0, сборка 7

Исходники `6825d3706e6fb4e704020498c7977e0efd1ec50f`, **vibic777**. Периоды прогресса/точка/диапазон/календарь данных, папки/фильтры/виды/штампы заметок. 15 JVM-тестов прошли, общий код скомпилирован Android/Windows. packageDelivery + verifyManagedInstallerLanguages: BUILD SUCCESSFUL за 1 мин 22 с, 101 задача (36 выполнены, 65 up-to-date).

В `dist/2026-10-02-0.3.0`: debug APK 47496347 байт, release-test APK 38330140, EXE 90485248, MSI 90316800, portable ZIP 88684110. Оба APK metadata 0.3.0/7, apksigner проверил тестовый сертификат Android Debug. SHA-256/размеры/source и protocol 4 / Transfer 3 / backup writer 3 / reader 1–3 в manifest; пять SHA проверены повторно. ZIP содержит exe/portable marker/runtime/лицензии, не содержит пользовательских баз/секретов/ключей.

Десять языков, региональные варианты, два fallback, фактический UI-язык Windows ru, Unicode MSI и условия silent/галочки прошли без Install/ExecuteAction. Проверки настоящих устройств/UI/LAN, установки/upgrade и батареи ещё открыты. Оба клиента нужно обновить для обмена папками.

## 2026-10-02 — 0.2.4, сборка 6

Исходники `870b5917a4f59f3ad9b1cf28025e9d7c04146a86`, контрибьютер **vibic777**. Демо 7/30, принудительная очистка по UUID, reset/категории, четыре вида графиков/цвета, меню настроек/поиск/защита/LAN. Семь JVM-тестов прошли; общий код скомпилирован обоими клиентами. Итоговые verifyManagedInstallerLanguages + packageDelivery: BUILD SUCCESSFUL, 1 мин 7 с, 101 задача (19 выполнены / 82 up-to-date).

Комплект `dist/2026-10-02-0.2.4`: debug APK 49296616 байт, release-test APK 38133536, EXE 90333696, MSI 90165248, portable ZIP 88532205. Оба APK metadata 0.2.4/6, подписи apksigner проверены: Android Debug (тестовая, не production). SHA-256/размеры/source в manifest, SHA256SUMS перепроверены. ZIP имеет runtime/launcher/portable marker/лицензии и не содержит баз/ключей/данных.

Проверки 10 языков, региональных вариантов, двух fallback и реального языка Windows ru, Unicode MSI и silent/галочки прошли без Install/ExecuteAction. Полная установка/upgrade, внешний вид устройств и измерения батареи остаются открытыми. Русский журнал — ИСПРАВЛЕНИЯ.md.

## 2026-10-02 — 0.2.3, сборка 5

Исходники `37f7735`, контрибьютер **vibic777**. Десять языков общего EXE/MSI мастера: en/ru/be/fr/de/es/it/zh/ja/ko. UI-язык пользователя Windows выбирается до диалогов; неподдерживаемые языки → en. EXE содержит окончательный MSI, переведены галочка запуска и проверка запущенной программы. FileVersion EXE 0.2.3.5 / CompanyName vibic777.

Итоговая команда `:desktopApp:verifyManagedInstallerLanguages :desktopApp:packageDelivery` — BUILD SUCCESSFUL, 47 секунд, 101 задача. В движке MSI вызвана упакованная custom action для 13 случаев: десять языков с региональными вариантами, два неподдерживаемых языка, автоматический текущий Windows UI language (ru). Проверены все Unicode-параметры, форматирование контролов/пути, порядок действий, условие запуска для UI 2/3/4/5 и снятой галочки. EXE сверён с вложенным MSI; `/QUIET /invalid` вернул 1603 без UI/установки. Только тестовые engine sessions и action выбора языка, Install/ExecuteAction не выполнялись; тестовый ProductCode отдельный от установленного FitEasy.

Комплект `dist/2026-10-02-0.2.3`: debug APK 47022422 байта, release-test APK 38002460, EXE 90231296, MSI 90062848, portable ZIP 88433214. Manifest содержит SHA исходников/размеры/SHA256/политику языка, рядом SHA256SUMS и лицензии MIT/MS-RL. Оба Android metadata: versionName 0.2.3 / versionCode 5; apksigner verify прошёл, сертификат Android Debug (тестовая подпись). Portable содержит exe/marker/runtime/WiX license, без data/DB/секретов/ключей.

Полная установка/обновление, работа проверки запущенного приложения на практике и визуальная проверка размещения переводов ещё не выполнены. Системные окна UAC и кнопки MessageBox используют язык Windows. Подробный русский журнал — ИСПРАВЛЕНИЯ.md; весь согласованный MVP ещё не завершён.

## 2026-10-02 — 0.2.2, сборка 4

Исходники `f083f07` (оформление в `7e1af62`), контрибьютер **vibic777**. Общие палитры/иконки с подсказками, безопасная демонстрация, сохранение ползунков после движения, кеш Windows и отдельный поток датчика Android. Русский журнал: ИСПРАВЛЕНИЯ.md.

`:desktopApp:jvmTest :app:compileDebugKotlin` — BUILD SUCCESSFUL; два теста, 0 ошибок/пропусков. Последняя упаковка `:desktopApp:packageDelivery` — BUILD SUCCESSFUL, 1 мин 1 с, 100 задач. При первом добавлении русской подписи галочки WiX завершился ошибкой 311 из-за кодировки английского шаблона; пока подпись английская. Десять языков установщика и автоматический выбор языка ещё не готовы.

Пять артефактов сохранены в `dist/2026-10-02-0.2.2`: debug APK 47019448 байт, release-test APK 38000805, EXE 90354688, MSI 89757672, portable ZIP 88429957. Manifest и SHA256SUMS содержат полный SHA исходников/файлов. ZIP проверен: launcher, portable.txt, runtime присутствуют; data и ключи подписи отсутствуют.

В MSI статически проверено условие запуска: `WIXUI_EXITDIALOGOPTIONALCHECKBOX = 1 AND NOT Installed AND UILevel >= 5`. Галочка включена по умолчанию; silent-режим не исполняет действие интерактивной кнопки завершения. Саму установку/обновление/silent-процесс на устройстве не запускали. Android release подписан тестовым debug.keystore. Визуальные жесты, уведомления, расход батареи и скорость на устройствах не измерены; согласованный MVP ещё не завершён.

## 2026-10-02 — 0.2.0, сборка 2

Исходники: `b10c533` (календарь/заметки/Windows в `1828149`, единый version.properties и отображение версии в `b10c533`). Контрибьютер **vibic777**.

Итоговая команда `:app:assembleDebug :app:assembleRelease :desktopApp:packageManagedMsi :desktopApp:packageManagedExe` — BUILD SUCCESSFUL, 1 мин 15 с, 99 задач. Среда JBR SDK 25.0.4.1. Android output-metadata обоих APK содержит versionName 0.2.0 / versionCode 2. Общий BuildInfo из version.properties показывает версию/сборку/дату в профиле/справке/«О программе»; справка и смена десяти языков доступны офлайн.

Выдача `D:\FitEasy\dist\2026-10-02-0.2.0`: debug APK, release-test APK, managed EXE/MSI, README, manifest.json с SHA исходников/номером сборки/размерами/SHA256, SHA256SUMS и профильные документы/лицензия. APK release подписан предоставленным debug.keystore для тестовой установки; production подпись отсутствует. Предыдущая выдача 0.1.0 сохранена отдельно.

Код/компиляция подключают календарь/расписания, управление сериями напоминаний, три вида заметок и Windows lifecycle. Практическая проверка установки/обновления, уведомлений, жестов, LAN новых записей и UI на устройствах пока не выполнена. Автоматические тесты не добавлялись/не запускались. Полный MVP остаётся в работе; см. MVP_STATUS.md/TODO.md. Gradle сообщает deprecations перед переходом на 10.

## 2026-10-02 — промежуточная 0.1.0

Исходники: `d88a0b5`. Контрибьютер **vibic777**.

Успешно выполнены `:app:assembleDebug`, `:app:assembleRelease`, `:desktopApp:packageDistributionForCurrentOS`; Gradle сообщил BUILD SUCCESSFUL. Полная последняя сборка заняла 1 мин 15 с. Среда упаковки: локальный JBR SDK 25.0.4.1; ранее debug/компиляция Windows также выполнены на JBR 21.0.11.

Файлы сохранены в `D:\FitEasy\dist\2026-10-02`: debug APK, release-test APK, Windows EXE/MSI, manifest.json с полным SHA исходников/размерами и SHA256SUMS.txt. Файлы сборок исключены из Git.

Release APK использует предоставленный владельцем debug.keystore для тестовой установки. Production подпись не настроена. Инсталляция и поведение на устройствах не проверены; полный согласованный MVP не завершён. Фактические ограничения — MVP_STATUS.md/TODO.md.

Предупреждения сборки: устаревший Android biometric API, неиспользуемое Unit-выражение, избыточный non-null assertion и Gradle deprecations перед переходом на Gradle 10. Ошибок сборки нет. Автоматические тесты не добавлялись и не запускались.

