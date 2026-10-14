# Catalogue import audit — 2026-10-02

Official USDA FoodData Central Foundation Foods April 2026 and FNDDS 2021–2023 (October 2024). Downloads: https://fdc.nal.usda.gov/download-datasets/ . USDA publishes FDC data under CC0: https://fdc.nal.usda.gov/api-guide/ .

The bundled datasets retain source names, FDC IDs, per-100g nutrient values and unknown nutrient values. Items without an energy value are omitted. `tools/import_usda.py` performs a provisional name-based product/prepared-dish split; the separate output files do not imply this classification is fully curated. Full regional coverage, Russian names/aliases, barcode mapping and ten-language catalogue translations are NOT complete. No claim of maximal worldwide completeness is made.

This is a real-data foundation for further catalogue review and additions, not an invented seed. Before public release audit classification, duplicates, recipe variants and licence notices; expand Russian/Belarusian/European/East Asian sources.
