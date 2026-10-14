package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

private val noteNavigationKeys=listOf("Все заметки","По папкам","К папкам","Создать папку","Управление папками","Сортировка заметок","Сортировка папок","По имени","По созданию","По изменению","По типу","По количеству заметок","По возрастанию","По убыванию","Карточки","Компактный список","Сетка","Без папки")
private val noteNavigationRows=mapOf(
"en" to "All notes|By folders|Back to folders|Create folder|Manage folders|Sort notes|Sort folders|By name|By creation|By modification|By type|By note count|Ascending|Descending|Cards|Compact list|Grid|Unfiled",
"be" to "Усе нататкі|Па папках|Да папак|Стварыць папку|Кіраванне папкамі|Сартаванне нататак|Сартаванне папак|Па назве|Па стварэнні|Па змене|Па тыпе|Па колькасці нататак|Па ўзрастанні|Па змяншэнні|Карткі|Кампактны спіс|Сетка|Без папкі",
"fr" to "Toutes les notes|Par dossiers|Retour aux dossiers|Créer un dossier|Gérer les dossiers|Trier les notes|Trier les dossiers|Par nom|Par création|Par modification|Par type|Par nombre de notes|Croissant|Décroissant|Cartes|Liste compacte|Grille|Sans dossier",
"de" to "Alle Notizen|Nach Ordnern|Zurück zu Ordnern|Ordner erstellen|Ordner verwalten|Notizen sortieren|Ordner sortieren|Nach Name|Nach Erstellung|Nach Änderung|Nach Typ|Nach Notizanzahl|Aufsteigend|Absteigend|Karten|Kompakte Liste|Raster|Ohne Ordner",
"es" to "Todas las notas|Por carpetas|Volver a carpetas|Crear carpeta|Gestionar carpetas|Ordenar notas|Ordenar carpetas|Por nombre|Por creación|Por modificación|Por tipo|Por número de notas|Ascendente|Descendente|Tarjetas|Lista compacta|Cuadrícula|Sin carpeta",
"it" to "Tutte le note|Per cartelle|Torna alle cartelle|Crea cartella|Gestisci cartelle|Ordina note|Ordina cartelle|Per nome|Per creazione|Per modifica|Per tipo|Per numero di note|Crescente|Decrescente|Schede|Elenco compatto|Griglia|Senza cartella",
"zh" to "所有笔记|按文件夹|返回文件夹|创建文件夹|管理文件夹|笔记排序|文件夹排序|按名称|按创建时间|按修改时间|按类型|按笔记数量|升序|降序|卡片|紧凑列表|网格|未分类",
"ja" to "すべてのノート|フォルダー別|フォルダーに戻る|フォルダーを作成|フォルダー管理|ノートの並べ替え|フォルダーの並べ替え|名前順|作成日時順|更新日時順|種類順|ノート数順|昇順|降順|カード|コンパクト一覧|グリッド|未分類",
"ko" to "모든 노트|폴더별|폴더로 돌아가기|폴더 만들기|폴더 관리|노트 정렬|폴더 정렬|이름순|생성순|수정순|유형순|노트 수순|오름차순|내림차순|카드|간단 목록|격자|미분류"
)
fun noteNavigationText(label:String,language:String):String {
    val index=noteNavigationKeys.indexOf(label)
    return if(index<0)translate(label,language)else noteNavigationRows[language]?.split('|')?.get(index) ?: label
}
@Composable fun NoteSortDialog(value:NoteSort,folders:Boolean,onSave:(NoteSort)->Unit,onDismiss:()->Unit){
    val language=LocalLanguage.current
    fun text(s:String)=noteNavigationText(s,language)
    AlertDialog(onDismissRequest=onDismiss,title={Text(text(if(folders)"Сортировка папок"else "Сортировка заметок"))},text={Column {
        (listOf("name" to "По имени","created" to "По созданию","updated" to "По изменению")+if(folders)listOf("count" to "По количеству заметок")else listOf("type" to "По типу")).forEach {(key,label)->
            Row {RadioButton(value.key==key,{onSave(value.copy(key=key))});Text(text(label))}
        }
        Row {RadioButton(!value.descending,{onSave(value.copy(descending=false))});Text(text("По возрастанию"))}
        Row {RadioButton(value.descending,{onSave(value.copy(descending=true))});Text(text("По убыванию"))}
    }},confirmButton={TextButton(onClick=onDismiss){Text(translate("Закрыть",language))}})
}
