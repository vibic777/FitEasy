# Localization Plan

**Status: proposed requirement.** FitEasy should be localization-ready from the start, with a target of ten interface languages similar to Notio-Android. Final language list and delivery schedule need vibic777's review. Localization applies to Android, Windows, and later Linux; shared data stores stable language-neutral codes, never translated labels.

## Proposed initial language set

Use Notio-Android's current set as a starting proposal: English, Russian, Belarusian, French, German, Spanish, Italian, Simplified Chinese, Japanese, and Korean. This is a reference list, not a decision to copy translations. Review regional forms and launch priorities before release.

## Requirements

- Use stable translation keys and a declared source language; keep every platform's UI strings complete against the same key inventory.
- Localize dates, numbers, decimal separators, units, and plural forms through platform locale libraries. Store quantities in canonical units.
- Keep user content, food names, and standardized identifiers distinct from UI translation resources; catalogue localization is a separate data problem.
- Do not concatenate sentence fragments. Support long text, text expansion, right-to-left readiness, font scaling, and accessibility labels.
- Use font fallback and verify every supported script; do not assume Latin glyph coverage is sufficient.
- Show an in-app language choice where platform behavior allows; also integrate with Android per-app language settings.
- Track translation status and human review. Machine-generated translations require review by fluent speakers before public release, especially nutrition and safety text.
- Windows and Linux must use the same keys and equivalent meaning, even if resource formats differ.

## Workflow proposal

Keep a canonical key list and source-language strings; generate or export platform-specific resources from a documented translation source. Add a completeness check to the eventual build workflow. Do not select a translation service or upload strings to a third party until privacy, cost, and licensing are reviewed.

## Open decisions

- Confirm the ten languages and first-release subset.
- Choose canonical source language (English or Russian) and translation maintenance format/tool.
- Decide how to review nutrition terminology and updates across platforms.
- Decide catalogue translation completeness and fallback per language. Global cuisine coverage is required at launch; cuisine/source region is independent of UI language.
# Текущее состояние 2026-10-02

Общий I18n.kt содержит 100 основных UI-элементов на ru/en/be/fr/de/es/it/zh/ja/ko. Выбор языка устройство-локальный, не передаётся LAN. Для отсутствующего ключа используется английский перевод, если он существует; новые строки пока требуют переноса в словарь. Дополнительные пояснения/ошибки, справка и переводы названий USDA ещё не завершены. Инструмент tools/list_ui_strings.py выводит статические строки и размеры словарей; текущие размеры совпадают (100). Требуется полная проверка покрытия/длинных строк и редактура носителями языка перед релизом.

D101 (0.4.20/40): словарь InterfaceStrings2 — 249 текстов на девяти языках, включая подсказки и сообщения об ошибках, которые раньше показывались по-русски. Текст, собранный из слов и чисел, переводится по группам слов функцией fragmentTranslation, только если каждая русская группа известна словарям; иначе остаётся как есть, поэтому записи человека не переводятся. Постоянные тесты: Text("…") (InterfaceTranslationTest), сообщения в переменных и причины require (NotioParityTest). Вычитка носителями языка не выполнялась. Известный пропуск: строка упражнения в конструкторе комплекса содержит введённое имя и остаётся на языке ввода.

D105–D106 (0.4.22/42): NotioStrings.kt — 351 текст Notio на десяти языках, получен генератором tools/import_notio_strings.py из ресурсов Notio-Android (список ключей — tools/notio_strings.txt); название продукта заменено на FitEasy. Файл не правится вручную: для нового текста добавьте ключ в список и запустите генератор.

D117 / 0.4.33/53: подсказка значка держится 5 секунд (удержание пальца или задержка мыши) и по нажатию открывает справку на описании этого значка; справка — вкладки категорий, подвкладки разделов с обзором-меню и списком значков (рисунок, название, значение) на десяти языках. Описание — docs/HELP.md. Общий код Android/Windows; данные и форматы не менялись. Правило подтверждения синхронизации D116 проверено тестом на все восемь комбинаций.

