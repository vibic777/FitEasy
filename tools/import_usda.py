"""Build separate catalogues from official USDA archives, keeping provenance.
Names stay in source English until reviewed translations are added. Classification is
explicitly provisional and reported for review; it never changes nutrient values.
"""
import json, pathlib, re, hashlib

root = pathlib.Path(__file__).resolve().parents[1]
all_foods = []
for filename, key in [('build/catalogue/foundation/FoodData_Central_foundation_food_json_2026-04-30.json', 'FoundationFoods'), ('build/catalogue/fndds/surveyDownload.json', 'SurveyFoods')]:
    obj = json.loads((root / filename).read_text(encoding='utf-8'))
    rows = obj.get(key, next(iter(obj.values())))
    for f in rows:
        if not isinstance(f, dict):
            continue
        values = {n['nutrient']['id']: n.get('amount') for n in f.get('foodNutrients', [])}
        kcal = values.get(1008, values.get(2047, values.get(2048)))
        if kcal is None or kcal < 0:
            continue
        name = f['description'].strip()
        cooked = bool(re.search(r'cooked|fried|baked|boiled|roasted|stew|soup|salad|sandwich|pizza|sushi|noodles|casserole|with sauce|stir.fry|prepared|pancake|omelet|cappuccino|latte', name, re.I))
        kind = 'dish' if cooked else 'product'
        cat = f.get('foodCategory') or f.get('wweiaFoodCategory') or {}
        category = cat.get('description', '') if isinstance(cat, dict) else str(cat)
        all_foods.append({'id': 'usda:'+str(f['fdcId']), 'name': name, 'kind': kind, 'category': category, 'nutrients': {'kcal': kcal, 'protein': values.get(1003), 'fat': values.get(1004), 'carbs': values.get(1005), 'fiber': values.get(1079)}, 'source': 'USDA FoodData Central / '+f.get('dataType','')+' / FDC '+str(f['fdcId']), 'updated': 0})
unique = {f['id']: f for f in all_foods}
out = root / 'shared/resources'
out.mkdir(parents=True, exist_ok=True)
for kind in ('product','dish'):
    data = [f for f in unique.values() if f['kind']==kind]
    (out / ('dishes.json' if kind == 'dish' else 'products.json')).write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
    print(kind, len(data))
(root/'docs/CATALOGUE_SOURCES.md').write_text('''# Catalogue import audit — 2026-10-02

Official USDA FoodData Central Foundation Foods April 2026 and FNDDS 2021–2023 (October 2024). Downloads: https://fdc.nal.usda.gov/download-datasets/ . USDA publishes FDC data under CC0: https://fdc.nal.usda.gov/api-guide/ .

The bundled datasets retain source names, FDC IDs, per-100g nutrient values and unknown nutrient values. Items without an energy value are omitted. `tools/import_usda.py` performs a provisional name-based product/prepared-dish split; the separate output files do not imply this classification is fully curated. Full regional coverage, Russian names/aliases, barcode mapping and ten-language catalogue translations are NOT complete. No claim of maximal worldwide completeness is made.

This is a real-data foundation for further catalogue review and additions, not an invented seed. Before public release audit classification, duplicates, recipe variants and licence notices; expand Russian/Belarusian/European/East Asian sources.
''', encoding='utf-8')

