package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private val libraryKeys="Справочник упражнений|Поиск упражнений|Все категории|Все оборудование|Добавить в комплекс|Нажмите на фигуру: показать / остановить|Анимации отключены в оформлении|Мобильность|Сила|Ходьба|Отдых|Без оборудования|Стул / опора|Гантели|Стена|Источник|Включить анимации".split('|')
private val libraryRows=mapOf(
"en" to "Exercise library|Search exercises|All categories|All equipment|Add to routine|Tap the figure to play / stop|Animations are disabled in Appearance|Mobility|Strength|Walking|Rest|No equipment|Chair / support|Dumbbells|Wall|Source|Enable animations",
"be" to "Даведнік практыкаванняў|Пошук практыкаванняў|Усе катэгорыі|Усё абсталяванне|Дадаць у комплекс|Націсніце на фігуру: паказаць / спыніць|Анімацыі выключаны ў афармленні|Мабільнасць|Сіла|Хадзьба|Адпачынак|Без абсталявання|Крэсла / апора|Гантэлі|Сцяна|Крыніца|Уключыць анімацыі",
"fr" to "Bibliothèque d’exercices|Rechercher des exercices|Toutes les catégories|Tout le matériel|Ajouter au programme|Touchez la silhouette : lire / arrêter|Animations désactivées dans Apparence|Mobilité|Force|Marche|Repos|Sans matériel|Chaise / appui|Haltères|Mur|Source|Activer les animations",
"de" to "Übungsbibliothek|Übungen suchen|Alle Kategorien|Alle Geräte|Zum Programm hinzufügen|Figur antippen: abspielen / stoppen|Animationen im Erscheinungsbild deaktiviert|Mobilität|Kraft|Gehen|Ruhe|Ohne Geräte|Stuhl / Halt|Hanteln|Wand|Quelle|Animationen aktivieren",
"es" to "Biblioteca de ejercicios|Buscar ejercicios|Todas las categorías|Todo el equipo|Añadir al programa|Toca la figura: reproducir / parar|Animaciones desactivadas en Apariencia|Movilidad|Fuerza|Caminar|Descanso|Sin equipo|Silla / apoyo|Mancuernas|Pared|Fuente|Activar animaciones",
"it" to "Catalogo degli esercizi|Cerca esercizi|Tutte le categorie|Tutta l’attrezzatura|Aggiungi al programma|Tocca la figura: riproduci / ferma|Animazioni disattivate in Aspetto|Mobilità|Forza|Camminata|Riposo|Senza attrezzi|Sedia / appoggio|Manubri|Muro|Fonte|Attiva animazioni",
"zh" to "练习目录|搜索练习|所有类别|所有器械|添加到计划|点击轮廓：播放/停止|外观设置中已关闭动画|活动度|力量|步行|休息|无需器械|椅子/支撑|哑铃|墙壁|来源|启用动画",
"ja" to "運動ライブラリ|運動を検索|すべての分類|すべての器具|プログラムに追加|図をタップ：再生/停止|外観設定でアニメーションが無効です|可動性|筋力|歩行|休憩|器具なし|椅子/支え|ダンベル|壁|出典|アニメーションを有効にする",
"ko" to "운동 라이브러리|운동 검색|모든 분류|모든 기구|프로그램에 추가|그림을 누르세요: 재생/정지|꾸미기에서 애니메이션이 꺼져 있음|가동성|근력|걷기|휴식|기구 없음|의자/지지대|덤벨|벽|출처|애니메이션 켜기"
)
fun libraryText(key:String,language:String):String {
    // Catalogue categories and equipment are stored by their English names; show them through the same dictionary.
    val index=libraryKeys.indexOf(key).let {if(it>=0)it else libraryRows.getValue("en").split('|').indexOf(key)}
    if(index>=0 && language=="ru")return libraryKeys[index]
    return if(index<0)rigText(key,language)else if(language=="ru")translate(key,language)else (libraryRows[language] ?: libraryRows.getValue("en")).split('|')[index]
}
/** One page of the library: a built-in card or an exercise the user made in the rig editor. */
private class LibraryEntry(val id:String,val exercise:Exercise,val category:String?,val equipment:String?,val asset:Boolean)
/** The figure takes all the room it is given. */
@Composable private fun LibraryFigure(e:Exercise,running:Boolean,female:Boolean,modifier:Modifier,onClick:()->Unit) {
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val clip=e.rig
        if(clip==null)Text(rigText("No rig scene",LocalLanguage.current))else RigCanvas(clip,running=running,female=female,side=minOf(maxWidth,maxHeight),onClick=onClick)
    }
}
@Composable fun ExerciseLibrary(platform:Platform,onDismiss:()->Unit,onSelect:((Exercise)->Unit)?=null,onEnableAnimations:()->Unit={},onOpenAppearance:(()->Unit)?=null,onOpenEconomy:(()->Unit)?=null){
    FullPage(onDismiss){ExerciseLibraryScreen(platform,onDismiss,onSelect,onEnableAnimations,onOpenAppearance,onOpenEconomy=onOpenEconomy)}
}
/**
 * The library fills the screen and the exercise is its main content (D094): one row of icon tools that can be hidden,
 * arrows to flip through the chosen category, and "show exercise", which leaves only the moving figure and Back.
 */
@Composable fun ExerciseLibraryScreen(platform:Platform,onDismiss:()->Unit,onSelect:((Exercise)->Unit)?=null,onEnableAnimations:()->Unit={},onOpenAppearance:(()->Unit)?=null,startFull:Boolean=false,onOpenEconomy:(()->Unit)?=null){
    val language=LocalLanguage.current
    var query by remember {mutableStateOf("")}
    var category by remember {mutableStateOf<String?>(null)}
    var equipment by remember {mutableStateOf<String?>(null)}
    var running by remember {mutableStateOf(startFull)}
    var full by remember {mutableStateOf(startFull)}
    var searching by remember {mutableStateOf(false)}
    var tools by remember {mutableStateOf(true)}
    var details by remember {mutableStateOf(true)}
    var assets by remember {mutableStateOf(emptyList<ExerciseAsset>())}
    var designing by remember {mutableStateOf<Exercise?>(null)}
    var edited by remember {mutableStateOf<Exercise?>(null)}
    var editingAssetId by remember {mutableStateOf<String?>(null)}
    var assetError by remember {mutableStateOf("")}
    val scope=rememberCoroutineScope()
    LaunchedEffect(platform){suspend fun refresh(){withContext(Dispatchers.IO){platform.load().shared.exerciseAssets}.let {assets=it}};refresh();platform.dataChanged.collect {refresh()}}
    val (avatars,setAvatar)=rememberExerciseAvatars(platform)
    val foreground by platform.foreground.collectAsState(initial=true)
    val entries=remember(query,category,equipment,language,assets){
        searchExercises(query,category,equipment,language).map {LibraryEntry(it.id,it.exercise(language),it.category,it.equipment,false)}+
            assets.filter {it.deleted==null && (it.exercise.name+it.exercise.instructions).contains(query,true)}.map {LibraryEntry(it.id,it.exercise,null,null,true)}
    }
    // One exercise at a time; the arrows flip through the chosen category.
    var shown by remember(query,category,equipment){mutableIntStateOf(0)}
    val entry=entries.getOrNull(shown.coerceIn(0,(entries.size-1).coerceAtLeast(0)))
    val flip={step:Int->if(entries.isNotEmpty()){shown=(shown.coerceIn(entries.indices)+step+entries.size)%entries.size;running=full}}
    val back=translate("Назад",language)
    if(full && entry!=null)Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            RigAction("previous",back,{full=false;running=false},translated=false)
            androidx.compose.material3.Text(entry.exercise.name,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        LibraryFigure(entry.exercise,foreground && running,avatars[exerciseAvatarKey(entry.exercise)] ?: true,Modifier.weight(1f).fillMaxWidth()){running=!running}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            RigAction("previous","Previous exercise",{flip(-1)},enabled=entries.size>1)
            RigAction(if(running)"pause" else "play",translate(if(running)"Пауза" else "Начать",language),{running=!running},translated=false)
            RigAction("next","Next exercise",{flip(1)},enabled=entries.size>1)
        }
    }
    else Column(Modifier.fillMaxSize().padding(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            RigAction("previous",back,onDismiss,translated=false)
            androidx.compose.material3.Text(libraryText("Справочник упражнений",language),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
            RigAction("search",translate("Поиск",language),{searching=!searching},selected=searching,translated=false)
            RigAction("joint+",translate("Новое упражнение",language),{editingAssetId=null;designing=Exercise(name=rigText("Rig editor",language),rig=RigClip(frames=listOf(RigFrame(time=0f,pose=rigBase()))))},translated=false)
            RigAction(if(tools)"hide" else "tools",if(tools)"Hide panel" else "Show panel",{tools=!tools})
        }
        if(searching)OutlinedTextField(query,{query=it},label={Text(libraryText("Поиск упражнений",language))},singleLine=true,modifier=Modifier.fillMaxWidth())
        // Categories and equipment share one row of icons; it scrolls sideways and hides with the panel button.
        if(tools)Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
            FilterChip(selected=category==null,onClick={category=null;running=false},label={Text(libraryText("Все категории",language))})
            exerciseCatalogue.map {it.category}.distinct().forEach {c->FilterChip(selected=category==c,onClick={category=c;running=false},label={Text(libraryText(c,language))})}
            ToolGap()
            FilterChip(selected=equipment==null,onClick={equipment=null;running=false},label={Text(libraryText("Все оборудование",language))})
            exerciseCatalogue.map {it.equipment}.distinct().forEach {e->FilterChip(selected=equipment==e,onClick={equipment=e;running=false},label={Text(libraryText(e,language))})}
        }
        // Shown only while the Appearance switch is off; the button leads to that switch.
        if(!LocalExerciseMotion.current)Row(verticalAlignment=Alignment.CenterVertically){Text(libraryText("Анимации отключены в оформлении",language),Modifier.weight(1f));TextButton(onClick=onOpenAppearance ?: onEnableAnimations){Text(libraryText("Включить анимации",language))}}
        // Battery economy stops the animation of sections and icons; the screen names that setting as the reason (D098).
        if(LocalBatteryEconomy.current)Row(verticalAlignment=Alignment.CenterVertically){Text(economyNotice,Modifier.weight(1f));if(onOpenEconomy!=null)TextButton(onClick=onOpenEconomy){Text("Режим экономии батареи")}}
        if(entry==null)Text("Ничего не найдено")
        else {
            val e=entry.exercise;val female=avatars[exerciseAvatarKey(e)] ?: true
            Row(verticalAlignment=Alignment.CenterVertically) {
                RigAction("previous","Previous exercise",{flip(-1)},enabled=entries.size>1)
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    androidx.compose.material3.Text(e.name,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                    androidx.compose.material3.Text("${shown.coerceIn(entries.indices)+1} / ${entries.size}"+if(entry.category!=null && entry.equipment!=null)" · "+libraryText(entry.category,language)+" · "+libraryText(entry.equipment,language) else "",style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                RigAction("next","Next exercise",{flip(1)},enabled=entries.size>1)
            }
            LibraryFigure(e,foreground && running,female,Modifier.weight(1f).fillMaxWidth()){running=!running}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                RigAction("expand",translate("Показать упражнение",language),{full=true;running=true},translated=false)
                RigAction(if(running)"pause" else "play",translate(if(running)"Пауза" else "Начать",language),{running=!running},translated=false)
                RigAction("woman",translate("Женский силуэт",language),{setAvatar(e,true)},selected=female,translated=false)
                RigAction("man",translate("Мужской силуэт",language),{setAvatar(e,false)},selected=!female,translated=false)
                RigAction("joint","Rig editor",{editingAssetId=if(entry.asset)entry.id else null;designing=e})
                RigAction("info",translate("Описание",language),{details=!details},selected=details,translated=false)
                if(onSelect!=null)RigAction("add",libraryText("Добавить в комплекс",language),{onSelect(if(entry.asset)e.copy(catalogueId=entry.id)else e);onDismiss()},translated=false)
            }
            if(details)Column(Modifier.fillMaxWidth().heightIn(max=96.dp).verticalScroll(rememberScrollState())) {
                Text(e.instructions)
                Text("Не выполняйте движения через боль. Можно пропустить упражнение или остановиться.",style=MaterialTheme.typography.labelSmall)
            }
        }
    }
    designing?.let {e->RigEditor(platform,e.rig,onDismiss={designing=null},onSave={edited=e.copy(rig=it)},onEnableAnimations=onEnableAnimations)}
    edited?.let {e->AlertDialog(onDismissRequest={edited=null},title={Text(libraryText("Справочник упражнений",language))},text={Column {
        OutlinedTextField(e.name,{edited=e.copy(name=it)},label={Text(translate("Название",language))})
        OutlinedTextField(e.instructions,{edited=e.copy(instructions=it)},label={Text(translate("Описание",language))})
        if(assetError.isNotBlank())Text(assetError)
    }},confirmButton={TextButton(onClick={val id=editingAssetId ?: newId();scope.launch {runCatching {withContext(Dispatchers.IO){platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(exerciseAssets=s.shared.exerciseAssets.filterNot {it.id==id}+ExerciseAsset(id=id,exercise=e.copy(catalogueId=id)))).also {validate(it.shared)}}}}.onSuccess {assets=it.shared.exerciseAssets;edited=null;assetError=""}.onFailure {assetError=rigText("Invalid rig file",language);AppJournal.record("rig.asset.save.failed",JournalLevel.ERROR,it)}}}){Text(rigText("Save",language))}},dismissButton={TextButton(onClick={edited=null}){Text(rigText("Cancel",language))}})}
}
