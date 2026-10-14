# Security and Health Data

**Status: draft.** FitEasy handles sensitive lifestyle and potentially health-related information. Optional, settings-controlled database encryption is required as a feature on Android, Windows, and the later Linux client. Minimize what is collected and keep local use independent from sync services.

## Local data

### Accepted direction: database encryption and app lock (2026-10-01)

Database encryption is enabled/disabled explicitly in settings, following Notio. **Accepted default: off** (2026-10-01). Changing the switch requires a safe SQLite/SQLCipher conversion with validation and recovery on failure. The setting is changed only after conversion succeeds. Generate and protect a separate database key using platform secure storage. Whether FitEasy exposes Show/Copy for this key, as Notio does, remains to be decided.

App lock is a separate configurable feature following Notio: PIN, password, graphical pattern, local login/password and optional biometrics where supported. Include lock timeout, retry delays and recovery settings. Keep credential hashes, biometric bindings, SMTP secrets and lock policy device-local and out of sync/ordinary exports.

### Wrong-attempt policy (accepted 2026-10-01; follows Notio)

Allow configuring free attempts, initial delay, total attempt limit and the action after reaching that limit. Notio's reference defaults are five free attempts, a ten-second initial delay, exponentially increasing waits capped at fifteen minutes, and no total limit (`maxAttempts = 0`). The default limit action is recovery-only; it activates only when a nonzero limit is configured. Persist counters and wait state; changing the wall clock or restarting must not bypass throttling.

Offer two limit actions: restrict entry to configured recovery methods, or wipe the app's own local data. Wipe is opt-in, off by default, and requires a separate typed confirmation explaining irreversible deletion before it can be enabled. Document exactly what is erased and cancel local reminders/background work. Treat this as a local security action: it must not propagate deletion to paired devices or erase external backups. Decide the detailed cleanup/reinitialization scope before implementation. Successful authentication/recovery resets the relevant lockout state according to the reviewed policy.

### SMTP recovery

Provide recovery of the app-lock credential through a one-time code sent using the user's configured SMTP server. Settings include host, port, transport security, login, password, sender and destination email, plus a user-triggered test-mail action. Protect SMTP credentials with OS secure storage. Support STARTTLS and SSL/TLS with certificate and hostname validation. Notio also offers plaintext SMTP with a warning; inclusion of that mode in FitEasy remains open.

Notio's reference uses a six-digit, single-use code with ten-minute expiry, limited attempts and resend throttling, storing only a hash. FitEasy should retain these controls and review exact limits before implementation. Successful recovery authorizes setting a new lock credential. It does not recover a lost SQLCipher key or forgotten backup passphrase. No email is sent during this design review.

### Security-question recovery (accepted 2026-10-01)

Offer an optional user-configured question and answer to recover the app lock locally, without network access. Store only a salted password-derived hash of the answer; keep the answer out of logs, sync and backups. Define answer normalization consistently at setup and verification and apply retry throttling. A correct answer authorizes choosing a new lock credential; it does not recover a lost database key or backup passphrase. Allow configuring both the question and SMTP recovery.

### TOTP recovery (accepted 2026-10-01)

Include optional recovery through a TOTP authenticator in the initial release, alongside the security question and SMTP code. Setup presents a QR code and a manual secret, then requires a valid authenticator code before activation. Protect the TOTP secret with platform secure storage; unlike passwords it must be available for code verification and cannot be stored only as a hash. Exclude it from sync, ordinary backups, logs and diagnostics. Allow replacement/removal only after authentication. Define clock tolerance, replay prevention and retry throttling before implementation. Successful verification authorizes setting a new app-lock credential; it does not recover database keys or backup passphrases.

- Proposed Android database: Room encrypted with SQLCipher.
- Define key generation, storage, rotation, backup interaction, biometric/device-lock behavior, and recovery before implementation. Never hard-code or log keys.
- Use platform secure storage for key material; do not treat obfuscation as encryption.
- Protect exported backup separately from the local database key.
- Keep health and food diary content out of logs, crash breadcrumbs, analytics, and clipboard by default.

## Integrations

- Health Connect is optional. Explain why each permission is needed and ask only when the user turns on the related feature.
- Handle denied/revoked permission and absent provider without blocking other app functions.
- Read the minimum required data and document whether imported records are retained locally.
- Avoid background collection unless needed for an enabled feature and clearly explained.

## Future sync service

Before implementation, document data categories uploaded, encryption in transit and at rest, account recovery, device removal, retention, deletion, access controls, breach response, and operational secrets management. Sync is opt-in; local data remains available when signed out.

## Product safety

Calorie, weight, hydration, and activity outputs are estimates, not clinical recommendations. Minor profiles require a separate reviewed experience; no weight-loss calorie deficit recommendations or adult BMI categories for minors.

## Release review

Complete a platform permission review, backup/restore threat review, data deletion review, and store disclosure review before public release. Applicable legal requirements depend on launch regions and need current specialist review.

D087 (0.4.13/33): способы входа — PIN-код (4–12 цифр), пароль, логин и пароль, графический ключ; биометрия и восстановление прежние. Способ хранится в LockSettings.kind только на устройстве. Экран будильника напоминаний (D086) показывается поверх блокировки, но не открывает дневник.

D099 (0.4.20/40): автоблокировка LockSettings.timeoutSeconds — 0 сразу (по умолчанию, прежнее поведение), 30/60/300/900 секунд, своё время до 23:59:59, −1 только при запуске. При значении 0 приложение блокируется в момент сворачивания; при остальных — при возврате, если прошло не меньше заданного времени; перевод часов назад блокирует. Пока открыт системный выбор файла или мелодии, вызванный из приложения, блокировка откладывается не более чем на пять минут. Контрольный вопрос выбирается из пяти готовых (как в Notio) или пишется свой; хранится текст вопроса и хеш ответа. Настройки остаются на устройстве.

D103 (0.4.21/41): шифрование базы включается и выключается после подтверждения, преобразование идёт вне потока интерфейса, отказ ничего не меняет. Новый ключ TOTP сохраняется только после верного 6-значного кода. Экран восстановления предлагает способы тремя строками; ошибки SMTP показаны по причине.

D106 (0.4.22/42): раздел настроек «Безопасность и приватность» по образцу Notio — блок «Блокировка приложения» (состояние, «Настроить блокировку», «Заблокировать») и блок «Уведомления-напоминания»: что напоминание показывает на экране блокировки — только общий текст (по умолчанию, как было), только название, название и текст. Выбор хранится секретом устройства; на Android он задаёт публичную версию уведомления, разблокированный экран показывает напоминание целиком. Шифрование базы перенесено на страницу «Резервная копия», как в Notio.

D107 (0.4.23/43): пароль базы виден в блоке «Шифрование базы данных» (скрыт, пока не нажат значок показа), копируется и по подтверждению отправляется открытым текстом на адрес восстановления через настроенный SMTP. Восстановления утерянного ключа нет: он создаётся приложением и хранится в Android Keystore или Windows DPAPI.

D111 / 0.4.27/47: запрос синхронизации с заданными заранее направлением, способом и разделами несёт отметку SyncMessage.preset, и второе устройство вопрос не показывает; при открытом выборе вопрос обязателен. Общий код Android/Windows; протокол LAN 7 прежний, обе стороны должны быть 0.4.27.

D130–D131 / 0.4.39/59: проверка кода и библиотек, находки и их состояние ведутся в [SECURITY_REVIEW.md](SECURITY_REVIEW.md); этот файл остаётся описанием замысла защиты.
