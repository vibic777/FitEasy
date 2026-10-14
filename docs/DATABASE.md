# Shared Data Model and Database Proposal

Implemented physical aggregate schema 2 (D062, 2026-10-03) supersedes the single app_state prototype: shared SqlStateStore, separate Product/Dish and other aggregate tables, indexed diary columns, metadata singleton, transactional migration 1→2 with snapshot comparison and rollback. Nested historical objects remain JSON; full relational ingredient/stroke decomposition is future refinement. Raw reader 1/2 and writer 2; portable Snapshot/backup/LAN unchanged. See STORAGE_MIGRATIONS.md for actual migration and limitations.

**Status: draft.** This describes platform-neutral records. Android may store them in Room/SQLCipher; FitEasy-Win may use a different database while preserving meaning and export/sync behavior.

## Data conventions

Accepted unit coverage: kg/lb, cm/in, ml/l/US fl oz/Imperial fl oz, km/mi and kcal/kJ. Persist canonical values with defined precision and explicit unit semantics; preserve entered quantity/unit where useful for audit and diary snapshots. Unit switches are presentation conversions, not destructive rewrites. Household serving conversions remain food-specific. Separate mass ounces from fluid ounces if mass-ounce entry is added later.

Recoverable deletion requires a trash lifecycle for agreed record types, with deletion time and restore/purge semantics. Sync tombstones have a separate retention requirement: purging a visible trash item must not immediately remove evidence needed to prevent stale peers resurrecting deleted data. Keep related records consistent when deleting/restoring beverages linked to nutrition and dishes linked to recipes. Bundled catalogue entries are not user-owned trash items. Exact schema and policies remain under design.

- IDs for user-owned and synchronizable entities: UUID, generated on the client.
- Timestamps: UTC instant for creation/update; user-entered event date/time also retains the relevant local date/time-zone context where day grouping matters.
- Quantities: explicit numeric amount and unit code; define rounding and unit conversion centrally.
- Nutrition facts: nullable per-100g values in canonical units (kcal, grams). Unknown is null, never silently zero.
- Soft delete: tombstone timestamp for syncable entities; local-only deletion behavior to be specified.
- Source/provenance: distinguish bundled catalogue, imported source, and user-entered values.
- Schema version and migration history are explicit.

## Proposed entities

Workout design must include exercise definitions, built-in/user-created routine definitions, ordered routine steps with duration/repetition/rest parameters, and completed-session snapshots. Stable IDs and explicit built-in/custom provenance support catalogue updates and LAN transfer. Exact entities and schema are pending; recipes and workout routines remain distinct domain concepts.

Accepted 2026-10-01: one personal profile per installation, with optional age, height, weight and activity level. Enforce the single-profile constraint in the eventual schema. Weight history belongs to Measurement; the current weight used by calculators must have a documented relationship to that history. Missing profile inputs remain unknown, not zero. Sync must map the paired devices' personal profile deliberately; never create a second active profile during import. Profile identity and merge/copy/replace behavior still need definition.

| Entity | Purpose | Important relationships |
|---|---|---|
| `Profile` | Local user preferences and optional measurements context | owns goals and settings; account is optional |
| `Goal` | User-defined target by metric and period | profile-owned; may be disabled |
| `Product` | Separate basic-product catalogue (apple, banana, potato) | referenced by diary items and recipe ingredients |
| `Dish` | Separate prepared-food catalogue (boiled potato, baked apple, fried banana) | declared nutrition or optional recipe-derived nutrition; referenced by diary items |
| `ProductAlias` / `DishAlias` | Search terms scoped to the respective catalogue | reference the respective stable entity ID |
| `DiaryEntry` | Logged food at a date/time and meal | parent for one or more food/recipe portions |
| `DiaryItem` | Product or dish quantity and nutrition snapshot | belongs to entry; explicit source type and stable source ID |
| `Recipe` | User-defined named recipe | owns ingredient lines |
| `RecipeIngredient` | Product (or prepared dish), amount and unit | belongs to recipe; explicit source type/ID; avoid cyclic recipe dependencies |
| `BeverageEntry` | Drink type and volume at date/time, including plain water | profile-owned; optional Product/Dish reference and linked nutrition record |
| `ActivityEntry` | Walk or other manual/imported activity | stores source and optional external source ID |
| `WorkoutSession` | Completion of a guided low-impact session | references optional workout definition |
| `Measurement` | Optional weight/body measurement | profile-owned; metric and unit explicit |
| `Device` | Paired-peer identity/trust for LAN sync | required in initial sync; account registration later |
| `ChangeRecord` | Durable sync outbox/inbox metadata | references entity type/UUID and revision |

## ER outline

```text
Profile 1──* Goal
Profile 1──* DiaryEntry 1──* DiaryItem *──0..1 Product or Dish
Dish    *──0..1 Recipe 1──* RecipeIngredient *──1 Product or Dish
Product 1──* ProductAlias
Dish    1──* DishAlias
Profile 1──* BeverageEntry
Profile 1──* ActivityEntry
Profile 1──* WorkoutSession
Profile 1──* Measurement
Account 1──* Device
ChangeRecord ── references (entityType, entityId)
```

## Historical accuracy and catalogue updates

Recipe scope accepted 2026-10-02: ingredient lines/quantities, ordered preparation steps, cooked yield with explicit mass unit and portion count. Dish can reference its recipe and calculated nutrition version. Derive whole-dish, per-100g and equal-portion values from valid inputs; retain nutrition snapshots in logged portions. Define recipe edit/version behavior and safeguards against ingredient cycles before implementation.

Accepted 2026-10-01: products and dishes are distinct catalogue entities, not a single catalogue distinguished only by labels. They have independent creation/search/update workflows and nutrient provenance. Prepared single-ingredient foods belong to Dish. A dish can have directly declared values or recipe-derived values; cooking yield and additions must be explicit. Define physical tables and shared nutrient/serving components during schema design.

Both catalogues need versioned replenishment/update support and custom user entries. Catalogue imports must validate duplicates, provenance and licensing and preserve user overrides, source IDs and diary snapshots. Migration from the earlier draft's generic Food terminology must be completed before schema implementation.

Global initial coverage requires separate metadata for source region, cuisine labels, translated names/aliases and preparation/composition variant. Do not use interface language as a cuisine identifier or deduplicate distinct dish variants solely by name. Detailed taxonomy and source crosswalks remain under design.

Initial discovery also requires product barcode mappings with provenance/validation, stable category/cuisine identifiers, favorites and recent-use state. Define their schema and sync scope before implementation. Barcodes identify catalogue items, not portions; branded package sizes and nutrient bases must remain explicit. Keep recents distinct from diary records and avoid creating duplicates just to store search history.

Diary items should preserve a snapshot of the name, amount, unit, energy and nutrient values used when logged. This prevents catalogue edits from changing past totals. Recipe ingredient references support editing; recipe-derived diary portions also retain a calculated nutrition snapshot. Food catalogue IDs and provenance need a stable import/update strategy before a large bundled catalogue is selected.

## Food values and quantity handling

Accepted nutrition scope: energy, protein, fat and carbohydrates, plus fiber when available. Unknown values remain nullable. Model configurable meal categories with stable IDs, labels, order and archive status; preserve historical references after renaming/archiving. Model food-specific serving definitions with explicit mass conversion and user-defined labels. Exact schema remains to be designed.

User food/recipe edits must not silently rewrite diary nutrition snapshots. Catalogue updates preserve user overrides and provenance. Selected sync includes the categories, servings and custom food/recipe dependencies needed to interpret diary records. Settings such as interface language stay device-local.

Store normalized nutrient values per 100 g for mass-based foods, then calculate portion values from explicit quantity and conversion. Household measures require a food-specific gram conversion; a generic “cup” conversion is invalid. Packaged item values may use another declared basis, which must be normalized or represented explicitly.

## Questions before schema freeze

Accepted progress scope: dated weight/waist/hip/chest measurements with explicit units and optional values. Metric visibility is a local presentation preference, distinct from deletion. Step data needs source identity, time intervals and deduplication rules; store enough provenance to prevent imported phone data being counted again after LAN sync. Detailed interval/aggregation schema remains open.

Accepted 2026-10-02: hydration goals have an explicit scope (plain water or all beverages). Store beverage classification and volume so both summaries can be calculated without changing records. Preserve goal scope/effective dates when edited and include scope in backup/sync contracts. Detailed nutrition linkage remains open.

Accepted 2026-10-01: track all beverage categories, including milk coffee, cappuccino/latte, juice and soft drinks. One consumption record must feed volume and nutrition summaries without counting calories twice. Define the linkage between BeverageEntry and DiaryItem and its sync dependency/ownership before implementation. Keep separate plain-water and total-beverage volumes; users can select plain-water or all-beverage goals. Earlier WaterEntry terminology is superseded by BeverageEntry.

- How is the single personal profile identified/mapped between paired devices and during restore?
- Which nutrients are in MVP (energy only vs protein/fat/carbohydrate/fiber/salt)?
- How should servings, liquid foods, and cooked/uncooked variants be represented?
- Are meal categories customizable and synchronizable?
- How should nutrition-linked drinks be edited/deleted atomically? Both plain-water and all-beverage goal modes are accepted.
- Which activity data is imported, and how are overlapping sources deduplicated?
- Are profile measurements and daily diary records syncable by default or opt-in?

No SQL DDL or migration should be generated until these choices are settled.

