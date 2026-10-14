package com.fiteasy.shared

private val demoKeys=listOf("Создать демонстрацию","Удалить демонстрацию","Удалить демонстрацию?","Изменённые образцы и ваши записи сохранятся. Неизменённые образцы попадут в корзину.")
private val demoRows=mapOf(
"en" to "Create demonstration|Remove demonstration|Remove demonstration?|Edited samples and your records will stay. Unchanged samples will move to trash.",
"be" to "Стварыць дэманстрацыю|Выдаліць дэманстрацыю|Выдаліць дэманстрацыю?|Змененыя прыклады і вашы запісы застануцца. Нязмененыя прыклады трапяць у кошык.",
"fr" to "Créer une démonstration|Supprimer la démonstration|Supprimer la démonstration ?|Les exemples modifiés et vos données seront conservés. Les exemples inchangés seront placés dans la corbeille.",
"de" to "Demonstration erstellen|Demonstration entfernen|Demonstration entfernen?|Bearbeitete Beispiele und Ihre Einträge bleiben erhalten. Unveränderte Beispiele werden in den Papierkorb verschoben.",
"es" to "Crear demostración|Eliminar demostración|¿Eliminar demostración?|Se conservarán los ejemplos modificados y sus datos. Los ejemplos sin cambios irán a la papelera.",
"it" to "Crea dimostrazione|Rimuovi dimostrazione|Rimuovere la dimostrazione?|Gli esempi modificati e i tuoi dati saranno conservati. Gli esempi invariati andranno nel cestino.",
"zh" to "创建演示数据|移除演示数据|移除演示数据？|修改过的示例及您的记录将保留。未修改的示例将移至回收站。",
"ja" to "デモを作成|デモを削除|デモを削除しますか？|編集済みのサンプルとご自身の記録は残ります。変更していないサンプルはごみ箱に移動します。",
"ko" to "데모 만들기|데모 삭제|데모를 삭제할까요?|수정한 예시와 사용자 기록은 유지됩니다. 변경하지 않은 예시는 휴지통으로 이동합니다.")
private val demoDictionary=demoRows.mapValues {(_,row)->demoKeys.zip(row.split('|')).toMap()}
fun demoTranslation(text:String,language:String)=demoDictionary[language]?.get(text)
