# Работа в фоне и батарея — правила и откуда они

Записано 10.10.2026 по указанию vibic777 (D121): самое важное — оптимально работать в фоновом режиме и не сажать
аккумулятор; найти лучшие решения, применить их в Notio и записать сюда, потому что FitEasy это касается так же.
Правила взяты из руководств самого Android. В Notio они сверены с кодом и применены (решение Notio Android D353,
`docs/BATTERY_RU.md` того проекта). Код FitEasy сверен со всеми 14 правилами 10.10.2026 (0.4.36/56); результат — в разделе «Сверка кода FitEasy» ниже.

## Источники

- [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms/schedule)
- [Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [NsdManager](https://developer.android.com/reference/android/net/nsd/NsdManager)
- [Excessive partial wake locks](https://developer.android.com/topic/performance/vitals/excessive-wakelock)
- [Battery Technical Quality Enforcement](https://android-developers.googleblog.com/2026/03/battery-technical-quality-enforcement.html)
- [Android vitals в Play Console](https://support.google.com/googleplay/android-developer/answer/9844486?hl=en)

## Правила

| № | Правило | Что говорит источник |
| --- | --- | --- |
| 1 | В фоне нет таймеров, опросов и службы «чтобы жить». То, что должно случиться в срок, — будильник `AlarmManager` | «Don't start a foreground service just to prevent the system from determining that your app is idle» |
| 2 | Неточный будильник везде, где минута не важна; точный — только для того, что человек сам назначил на минуту | «It's highly recommended that you create an inexact alarm whenever possible» |
| 3 | `setAlarmClock` не брать без нужды: телефон выходит из сна перед каждым таким будильником | «can significantly affect the device's resources, such as battery life» |
| 4 | В глубоком сне телефон отдаёт будильники одного приложения не чаще раза в 9 минут | «…cannot fire alarms more than once per nine minutes, per app» |
| 5 | Будить телефон редко: больше 10 пробуждений в час Android vitals считает чрезмерным | «more than 10 wake-ups per hour» |
| 6 | Блокировку сна держать только пока идёт то, ради чего она взята; больше 2 часов частичной блокировки за сутки — чрезмерно (звук, место и пользовательские задачи JobScheduler не считаются) | «more than two hours in a 24-hour period» |
| 7 | Служба переднего плана — только для того, чего человек ждёт прямо сейчас | «Only use a foreground service for tasks the user expects the system to execute immediately or without interruption» |
| 8 | Поиск устройств в сети останавливать, когда приложение не на экране | NsdManager: поиск «will continue until the application calls stopServiceDiscovery»; останавливать «whenever the application is paused or stopped» |
| 9 | Объявление себя в сети (mDNS) тоже снимать в фоне: пока оно есть, Wi-Fi отдаёт телефону каждый групповой пакет сети и будит его. В фоне ждёт только порт; сопряжённое устройство приходит по адресу прошлой синхронизации | NsdManager: регистрация и поиск — отдельные вызовы, каждый снимается своим |
| 10 | Экран — самое дорогое: держать его не дольше, чем нужно. В Notio окно будильника держит экран 60 секунд, на втором неотвеченном звонке 15, дальше по 5 | — (общее правило расхода; решение владельца 10.10.2026) |
| 11 | Не просить вывести приложение из оптимизации батареи | исключение допустимо только когда «the core function of the app is adversely affected» |
| 12 | После перезагрузки будильники ставить заново, и только это | «By default, all alarms are canceled when a device shuts down» |
| 13 | В сне сети нет: фоновая работа не должна на неё рассчитывать | Doze: «Suspends network access» |
| 14 | Процесс, разбуженный будильником, делает только то, за чем пришёл | — (следствие 1 и 5) |

## Сверка кода FitEasy (10.10.2026, 0.4.36/56)

| № | Что в коде | Итог |
| --- | --- | --- |
| 1 | Напоминания — только `AlarmManager`; служба одна и живёт, пока звучит мелодия (до минуты). Таймеры интерфейса (секундомер, статус, анимации, тренировка) остановлены вне экрана. Обратный отсчёт задержки на экране блокировки тикал и в фоне | Исправлено: `LockGate(foreground)` |
| 2 | Точный будильник — для времени, назначенного человеком, и его повторов; в режиме экономии и для проверки будильника — неточный | Соблюдено |
| 3 | `setAlarmClock` в коде нет (проверяет тест) | Соблюдено |
| 4, 5 | Повтор неотвеченного напоминания задаёт человек, от 1 минуты: при интервале меньше 9 минут пробуждений больше 10 в час, во сне Android отдаст их не чаще раза в 9 минут | Оставлено с причиной (DECISIONS.md, «D121, сверка»); в редакторе напоминания об этом сказано |
| 6 | Своих блокировок сна нет; `MediaPlayer.setWakeMode` держит процессор только пока звучит мелодия и отпускает на паузе и в конце | Соблюдено |
| 7 | Служба переднего плана одна — `ReminderRingService`, тип mediaPlayback, до минуты | Соблюдено |
| 8, 9 | Поиск, объявление в сети и блокировка групповых пакетов сняты, когда приложение не на экране (D112) | Соблюдено |
| 10 | Экран будильника не ставил `FLAG_KEEP_SCREEN_ON`, но гас только по таймауту телефона | Исправлено: страница закрывается сама через 60 / 15 / 5 секунд (`alarmScreenMillis`) |
| 11 | Запроса на выход из оптимизации батареи нет; кнопка «Настройки батареи» открывает страницу приложения в системе | Соблюдено |
| 12 | После перезагрузки ставятся будильники и без звука возвращаются уведомления, ждущие ответа (D115) | Соблюдено; возврат уведомлений — записанное решение D115 |
| 13 | В фоне сеть не используется | Соблюдено |
| 14 | Приёмник будильника читает запечатанные значения один раз и показывает напоминание; базу не открывает | Соблюдено |

Не измерено: расход на телефоне командами из раздела «Как проверить на телефоне» — открытый пункт TODO.

## Как применять

Новый код, который работает в фоне, ставит будильник, держит службу, блокировку сна, сеть или экран, сверяется с этой
таблицей до реализации; отступление записывается в docs/DECISIONS.md с причиной. Для Windows и Linux действуют правила
1, 10 и 14 (таймеры ждут, а не опрашивают; после сна компьютера срок пересчитывается).

## Как проверить на телефоне

```powershell
adb shell dumpsys batterystats --reset                 # вечером: начать отсчёт
adb shell dumpsys batterystats <пакет> > battery.txt   # утром
adb shell dumpsys alarm | Select-String -Context 0,6 "<пакет>"
adb shell dumpsys deviceidle force-idle                # усыпить телефон; вернуть: unforce
```

В `battery.txt` смотреть `Wake lock`, `Wakeup alarm`, `Foreground services`, `Wifi` / `Multicast`, `Screen`.

