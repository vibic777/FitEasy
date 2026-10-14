package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun CatalogueTransferPanel(platform:Platform,stock:List<Food>) {
    var categories by remember {mutableStateOf(catalogueKinds)}
    var pending by remember {mutableStateOf<CatalogueDocument?>(null)}
    var baseline by remember {mutableStateOf<List<Food>>(emptyList())}
    var mode by remember {mutableStateOf(ImportMode.MERGE)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    val language=LocalLanguage.current
    fun label(key:String)=catalogueTransferText(key,language)
    Hint("Продукты и блюда вместе с вашими правками — отдельным файлом, чтобы перенести только справочники.")
    catalogueKinds.forEach {kind->Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {Checkbox(kind in categories,{categories=if(it)categories+kind else categories-kind});androidx.compose.material3.Text(label(kind))}}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        androidx.compose.material3.OutlinedButton(modifier=Modifier.fillMaxWidth(),enabled=!busy && categories.isNotEmpty(),onClick={
            val selected=categories.toSet();busy=true
            platform.launchFileTask {
                try {val bytes=withContext(Dispatchers.IO){exportCatalogue(platform.load().shared,stock,selected)};platform.exportFile("FitEasy-${java.time.LocalDate.now()}.fiteasy-catalogue.json","application/json",bytes);message=label("Export ready")}
                catch(e:Exception){AppJournal.record("catalogue.export.failed",JournalLevel.ERROR,e);message=label("Catalogue operation failed")}
                finally {busy=false}
            }
        }){androidx.compose.material3.Text(label("Export"))}
        androidx.compose.material3.OutlinedButton(modifier=Modifier.fillMaxWidth(),enabled=!busy,onClick={platform.importFile {bytes->busy=true;platform.launchFileTask {
            try {val result=withContext(Dispatchers.IO){val parsed=readCatalogueDocument(bytes);parsed to effectiveCatalogue(platform.load().shared,stock).filter {it.kind in parsed.categories}};baseline=result.second;pending=result.first;mode=ImportMode.MERGE}
            catch(e:Exception){AppJournal.record("catalogue.import.read.failed",JournalLevel.ERROR,e);message=label("Catalogue operation failed")}
            finally {busy=false}
        }}}){androidx.compose.material3.Text(label("Import"))}
    }
    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    if(message.isNotEmpty())Text(message)
    pending?.let {document->AlertDialog(onDismissRequest={if(!busy)pending=null},title={Text(label("Import"))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(document.categories.joinToString {label(it)}+": ${document.foods.size}")
        ImportMode.entries.forEach {value->Row {RadioButton(mode==value,{mode=value},enabled=!busy);Text(label(value.name))}}
        Text(label(if(mode==ImportMode.REPLACE)"Replace warning" else "Other data preserved"))
    }},confirmButton={TextButton(enabled=!busy,onClick={
        val selectedMode=mode;busy=true
        platform.launchFileTask {
            try {withContext(Dispatchers.IO){platform.mutateSnapshot {current->
                if(selectedMode==ImportMode.REPLACE)require(effectiveCatalogue(current.shared,stock).filter {it.kind in document.categories}==baseline) {"Catalogue changed"}
                current.copy(shared=importCatalogue(current.shared,stock,document,selectedMode))
            }};pending=null;message=label("Import complete")}
            catch(e:Exception){AppJournal.record("catalogue.import.apply.failed",JournalLevel.ERROR,e);message=label("Catalogue operation failed")}
            finally {busy=false}
        }
    }){Text(label("Confirm"))}},dismissButton={TextButton(enabled=!busy,onClick={pending=null}){Text(label("Cancel"))}})}
}

private val exchangeLabels=mapOf(
    "Export" to listOf("Экспорт","Экспарт","Exporter","Exportieren","Exportar","Esporta","导出","エクスポート","내보내기"),
    "Import" to listOf("Импорт","Імпарт","Importer","Importieren","Importar","Importa","导入","インポート","가져오기"),
    "Confirm" to listOf("Подтвердить","Пацвердзіць","Confirmer","Bestätigen","Confirmar","Conferma","确认","確認","확인"),
    "Cancel" to listOf("Отмена","Адмена","Annuler","Abbrechen","Cancelar","Annulla","取消","キャンセル","취소"),
    "Export ready" to listOf("Экспорт подготовлен","Экспарт падрыхтаваны","Export préparé","Export vorbereitet","Exportación preparada","Esportazione preparata","导出已准备","エクスポート準備完了","내보내기 준비 완료"),
    "Import complete" to listOf("Импорт выполнен","Імпарт выкананы","Import terminé","Import abgeschlossen","Importación completada","Importazione completata","导入完成","インポート完了","가져오기 완료"),
    "Catalogue exchange" to listOf("Обмен справочниками","Абмен даведнікамі","Échange des catalogues","Katalogaustausch","Intercambio de catálogos","Scambio cataloghi","目录交换","カタログの交換","목록 교환"),
    "product" to listOf("Продукты","Прадукты","Produits","Lebensmittel","Productos","Prodotti","食材","食品","식품"),
    "dish" to listOf("Блюда","Стравы","Plats","Gerichte","Platos","Piatti","菜肴","料理","요리"),
    "MERGE" to listOf("Слияние","Зліццё","Fusionner","Zusammenführen","Combinar","Unisci","合并","統合","병합"),
    "REPLACE" to listOf("Замещение","Замяшчэнне","Remplacer","Ersetzen","Reemplazar","Sostituisci","替换","置換","교체"),
    "COPIES" to listOf("Добавление копиями","Даданне копіямі","Ajouter des copies","Kopien hinzufügen","Añadir copias","Aggiungi copie","添加副本","コピーを追加","사본 추가"),
    "Replace warning" to listOf("Подтвердите замещение выбранных справочников. Отсутствующие записи будут скрыты. Дневник и другие данные сохранятся.","Пацвердзіце замяшчэнне выбраных даведнікаў. Адсутныя запісы будуць схаваны. Дзённік і іншыя даныя захаваюцца.","Confirmez le remplacement des catalogues sélectionnés. Les éléments absents seront masqués. Le journal et les autres données sont conservés.","Ersetzen der ausgewählten Kataloge bestätigen. Fehlende Einträge werden ausgeblendet. Tagebuch und andere Daten bleiben erhalten.","Confirme el reemplazo de los catálogos seleccionados. Se ocultarán los registros ausentes. Se conservarán el diario y otros datos.","Conferma la sostituzione dei cataloghi selezionati. Le voci assenti saranno nascoste. Diario e altri dati saranno conservati.","确认替换所选目录。缺失记录将被隐藏。日记和其他数据将保留。","選択したカタログの置換を確認してください。含まれない項目は非表示になります。日記や他のデータは保持されます。","선택한 목록의 교체를 확인하세요. 없는 항목은 숨겨집니다. 일지와 다른 데이터는 유지됩니다."),
    "Other data preserved" to listOf("Дневник и другие данные сохранятся.","Дзённік і іншыя даныя захаваюцца.","Le journal et les autres données sont conservés.","Tagebuch und andere Daten bleiben erhalten.","Se conservarán el diario y otros datos.","Diario e altri dati saranno conservati.","日记和其他数据将保留。","日記や他のデータは保持されます。","일지와 다른 데이터는 유지됩니다."),
    "Catalogue operation failed" to listOf("Операция не выполнена. Проверьте файл; при замещении откройте импорт заново после изменения справочника.","Аперацыя не выканана. Праверце файл і паўтарыце імпарт.","Échec. Vérifiez le fichier et recommencez l'importation.","Fehlgeschlagen. Datei prüfen und Import erneut öffnen.","Error. Revise el archivo y repita la importación.","Operazione non riuscita. Controlla il file e ripeti l'importazione.","操作失败。请检查文件并重新导入。","操作に失敗しました。ファイルを確認し、再度インポートしてください。","작업에 실패했습니다. 파일을 확인하고 다시 가져오세요."))
fun catalogueTransferText(key:String,language:String):String {
    val index=listOf("ru","be","fr","de","es","it","zh","ja","ko").indexOf(language)
    return if(index>=0 && key in exchangeLabels)exchangeLabels.getValue(key)[index] else when(key) {
        "product"->"Products";"dish"->"Dishes";"MERGE"->"Merge";"REPLACE"->"Replace";"COPIES"->"Add copies"
        "Replace warning"->"Confirm replacement of selected catalogues. Missing records will be hidden. Diary and other data are preserved."
        "Other data preserved"->"Diary and other data are preserved."
        else->translate(key,language)
    }
}
