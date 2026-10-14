# Architecture Proposal

**Status: draft.** Android and Windows are the first planned clients; Linux follows later. The architecture keeps platform implementations replaceable while sharing stable product and data semantics across all clients.

## Proposed Android stack

- Kotlin and Jetpack Compose with Material 3.
- Single activity with Navigation Compose.
- Screen state owned by ViewModels and exposed as StateFlow; UI sends events and renders state.
- Domain use cases for calculations and policies that need deterministic behavior.
- Repository interfaces separate domain behavior from storage and external integrations.
- Room over SQLCipher for encrypted local relational storage, subject to key-management decision.
- DataStore for non-sensitive preferences.
- Coroutines for asynchronous work; WorkManager for retryable background sync when sync exists.
- Hilt for dependency injection, subject to project owner confirmation.
- Health Connect behind an optional activity data source.
- Food catalogue initially bundled/local; any remote catalogue source is an adapter, not a domain dependency.
- Localization resources are managed as a first-class product surface; see `LOCALIZATION.md`.

## Layering

```text
Compose screens
    ↓ user events / UI state
ViewModels
    ↓
Domain use cases and models
    ↓ repository interfaces
Data adapters: Room/SQLCipher, DataStore, Health Connect, later API
```

The UI must not query Room directly. Domain calculations should be testable without Android framework dependencies. Health Connect and network failures must not prevent local logging.

## Modules (initial proposal)

Start with a small `app` module and clear packages for `nutrition`, `hydration`, `activity`, `workouts`, `progress`, `profile`, and `data`. Split Gradle modules only when build ownership, reuse, or compile time justifies it. Do not begin with a large multi-module skeleton before domain boundaries are known.

## Data flow and offline-first behavior

UI reads repository-backed local state. User writes commit locally first. When a remote sync service is later added, a durable change journal/outbox queues syncable changes and a worker retries when connectivity and account state allow. The server must not be required for ordinary logging, search of the bundled catalogue, or viewing history.

## Cross-platform boundary

Share product terminology, entity meaning, units, UUID policy, backup semantics, and sync protocol. Do not require Windows to share Kotlin/Android UI or persistence libraries. See `docs/windows/`.

## Observability and failure behavior

- Avoid logging food diaries, measurements, credentials, tokens, or encryption keys.
- Surface understandable local-storage and restore errors; never silently discard user data.
- Health Connect absence/denial degrades activity data only; it does not block app use.
- Sync errors remain retryable and visible without hiding local changes.

## Decisions required before foundation

Phone-native pedometer is accepted for the first release (2026-10-02). Android supports TYPE_STEP_COUNTER on devices providing the sensor; permission and background lifecycle must be handled. Health Connect also supports on-device counting on Android 14 with SDK extension 20+ where available. Detect capability at runtime and define source precedence so phone and wearable totals are not summed blindly. Windows/Linux can display synchronized steps without requiring equivalent sensors. Sources: https://developer.android.com/health-and-fitness/fitness/basic-app/read-step-count-data and https://developer.android.com/health-and-fitness/health-connect/read-data .

Confirm Android minimum/target SDK policy, package identity, modularization, DI choice, SQLCipher distribution and key lifecycle, supported Android devices, and whether the first release is fully local-only. Track unresolved decisions in `DECISIONS.md`.

