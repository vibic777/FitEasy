import pathlib, re, sys
sys.stdout.reconfigure(encoding='utf-8')
texts=set()
for source in pathlib.Path('shared/src').rglob('*.kt'):
    texts.update(re.findall(r'Text\("([^"\n]+)"',source.read_text(encoding='utf-8-sig')))
print('\n'.join(sorted(value for value in texts if '$' not in value)))
catalogue=pathlib.Path('shared/src/com/fiteasy/shared/I18n.kt').read_text(encoding='utf-8-sig')
source=re.search(r'private val source="(.*?)"\.split',catalogue).group(1).split('|')
print('\nDictionary counts: source',len(source))
for locale,row in re.findall(r'"([a-z]{2})" to "([^"\n]*\|[^"\n]*)"',catalogue):
    print(locale,len(row.split('|')))
