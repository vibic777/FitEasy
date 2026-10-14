package com.fiteasy.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// The trash, the help and "about" repeat Notio's screens (D107). What lies in FitEasy's trash differs — diary records,
// products and dishes, notes and their folders, schedules, own exercises — the page and its actions are the same.

/** Notio's icon button: a Material outlined icon with its name on hover or long press. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NotioIconAction(icon:ImageVector,label:String,onClick:()->Unit,enabled:Boolean=true,tint:Color=LocalContentColor.current) {
    val shown=translate(label,LocalLanguage.current)
    HintBox(shown) {
        IconButton(onClick=onClick,enabled=enabled) {Icon(icon,contentDescription=shown,tint=if(enabled)tint else tint.copy(alpha=.38f))}
    }
}

/** How long the trash keeps things: 0 = until the person empties it. Nothing is removed silently unless chosen (Notio D140). */
val trashRetentionOptions=listOf(7,14,30,60,90,0)
/** One thing in the trash: what it is, its name, when it went there, and how to bring it back or remove it for good. */
class TrashItem(val key:String,val kind:String,val title:String,val deletedAt:Long,val restore:(SharedData,Long)->SharedData,val purge:(SharedData)->SharedData)
private val entryKinds=mapOf("food" to "Еда","drink" to "Напиток","activity" to "Активность","steps" to "Шаги","measurement" to "Замер")
fun trashItems(data:SharedData):List<TrashItem> = (
    data.noteFolders.filter {it.deleted!=null}.map {f->TrashItem("folder:"+f.id,nt("trash_folder_label"),f.name,f.deleted!!,{d,now->d.copy(noteFolders=d.noteFolders.map {if(it.id==f.id)it.copy(deleted=null,updated=now)else it})},{d->d.copy(noteFolders=d.noteFolders.filterNot {it.id==f.id})})}+
    data.notes.filter {it.deleted!=null}.map {n->TrashItem("note:"+n.id,"Заметка",n.title,n.deleted!!,{d,now->d.copy(notes=d.notes.map {if(it.id==n.id)it.copy(deleted=null,updated=now)else it})},{d->d.copy(notes=d.notes.filterNot {it.id==n.id})})}+
    data.schedules.filter {it.deleted!=null}.map {s->TrashItem("schedule:"+s.id,"Расписание",s.name,s.deleted!!,{d,now->d.copy(schedules=d.schedules.map {if(it.id==s.id)it.copy(deleted=null,updated=now)else it})},{d->d.copy(schedules=d.schedules.filterNot {it.id==s.id})})}+
    data.entries.filter {it.deleted!=null}.map {e->TrashItem("entry:"+e.id,entryKinds[e.type] ?: "Замер",e.name+" · "+e.date,e.deleted!!,{d,now->d.copy(entries=d.entries.map {if(it.id==e.id)it.copy(deleted=null,updated=now)else it})},{d->d.copy(entries=d.entries.filterNot {it.id==e.id})})}+
    (data.products+data.dishes).filter {it.deleted!=null}.map {f->TrashItem("food:"+f.id,if(f.kind=="dish")"Блюдо" else "Продукт",f.name,f.deleted!!,
        {d,now->d.copy(products=d.products.map {if(it.id==f.id)it.copy(deleted=null,updated=now)else it},dishes=d.dishes.map {if(it.id==f.id)it.copy(deleted=null,updated=now)else it})},
        {d->d.copy(products=d.products.filterNot {it.id==f.id},dishes=d.dishes.filterNot {it.id==f.id})})}+
    data.exerciseAssets.filter {it.deleted!=null}.map {a->TrashItem("exercise:"+a.id,"Упражнение",a.exercise.name,a.deleted!!,{d,now->d.copy(exerciseAssets=d.exerciseAssets.map {if(it.id==a.id)it.copy(deleted=null,updated=now)else it})},{d->d.copy(exerciseAssets=d.exerciseAssets.filterNot {it.id==a.id})})}
).sortedByDescending {it.deletedAt}
/** Removes for good what has been in the trash since before [olderThan]; null empties the trash. */
fun purgeTrash(data:SharedData,olderThan:Long?=null):SharedData = trashItems(data).filter {olderThan==null || it.deletedAt<olderThan}.fold(data) {d,item->item.purge(d)}
/** The trash empties itself only after the days the person chose. */
fun purgeExpiredTrash(snapshot:Snapshot,now:Long=System.currentTimeMillis()):Snapshot {
    val days=snapshot.local.trashRetentionDays;if(days<=0)return snapshot
    val shared=purgeTrash(snapshot.shared,now-days*86_400_000L)
    return if(shared==snapshot.shared)snapshot else snapshot.copy(shared=shared)
}
private val trashStamp=java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/** The trash as in Notio: what waits here, Restore and Delete for good as icons on every card, Empty the trash on top. */
@Composable fun TrashPage(snapshot:Snapshot,mutate:((Snapshot)->Snapshot)->Boolean,onDismiss:()->Unit) {
    val language=LocalLanguage.current
    val items=remember(snapshot.shared){trashItems(snapshot.shared)}
    var purging by remember {mutableStateOf<TrashItem?>(null)}
    var emptying by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf("")}
    fun text(name:String,value:Any?=null)=translate(nt(name),language).let {if(value==null)it else it.replace("{0}",value.toString())}
    val hint="Удалённые записи ждут здесь, пока вы не восстановите их или не удалите навсегда."
    SettingsPage(onDismissRequest=onDismiss,title={Text(nt("trash_title"))},text={
        if(items.isEmpty())Column(Modifier.fillMaxSize().padding(top=48.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(nt("trash_empty_state"),style=MaterialTheme.typography.titleMedium)
            androidx.compose.material3.Text(translate(hint,language),style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center)
            if(notice.isNotEmpty())androidx.compose.material3.Text(notice,color=MaterialTheme.colorScheme.primary)
        } else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item(key="top") {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.weight(1f)){Hint(hint)}
                androidx.compose.material3.TextButton(onClick={emptying=true}){androidx.compose.material3.Text(text("trash_empty_action"),color=MaterialTheme.colorScheme.error)}
            }}
            if(notice.isNotEmpty())item(key="notice"){androidx.compose.material3.Text(notice,color=MaterialTheme.colorScheme.primary)}
            items(items,key={it.key}) {item->
                Card(Modifier.fillMaxWidth()) {Row(Modifier.padding(start=16.dp,end=8.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        androidx.compose.material3.Text(translate(item.kind,language),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        androidx.compose.material3.Text(item.title,style=MaterialTheme.typography.titleMedium)
                        androidx.compose.material3.Text(text("trash_deleted_on",trashStamp.format(java.time.Instant.ofEpochMilli(item.deletedAt).atZone(java.time.ZoneId.systemDefault()))),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    NotioIconAction(NotioIcons.RestoreFromTrash,nt("trash_restore"),{if(mutate {s->s.copy(shared=item.restore(s.shared,System.currentTimeMillis()))})notice=text("trash_restored",item.title)})
                    NotioIconAction(NotioIcons.DeleteForever,nt("trash_delete_forever"),{purging=item},tint=MaterialTheme.colorScheme.error)
                }}
            }
        }
    },confirmButton={})
    purging?.let {item->AlertDialog(onDismissRequest={purging=null},title={Text("Удалить навсегда?")},text={androidx.compose.material3.Text(translate("«{0}» будет удалено навсегда. Это нельзя отменить.",language).replace("{0}",item.title))},
        confirmButton={androidx.compose.material3.TextButton(onClick={purging=null;if(mutate {s->s.copy(shared=item.purge(s.shared))})notice=text("trash_purged",item.title)}){androidx.compose.material3.Text(text("trash_delete_forever"),color=MaterialTheme.colorScheme.error)}},
        dismissButton={androidx.compose.material3.TextButton(onClick={purging=null}){androidx.compose.material3.Text(text("action_cancel"))}})}
    if(emptying)AlertDialog(onDismissRequest={emptying=false},title={Text(nt("trash_empty_confirm_title"))},text={androidx.compose.material3.Text(translate("Элементов корзины: {0}. Они будут удалены навсегда. Это нельзя отменить.",language).replace("{0}",items.size.toString()))},
        confirmButton={androidx.compose.material3.TextButton(onClick={emptying=false;if(mutate {s->s.copy(shared=purgeTrash(s.shared))})notice=text("trash_emptied")}){androidx.compose.material3.Text(text("trash_empty_action"),color=MaterialTheme.colorScheme.error)}},
        dismissButton={androidx.compose.material3.TextButton(onClick={emptying=false}){androidx.compose.material3.Text(text("action_cancel"))}})
}

/** Settings → Trash, as in Notio: open it, and how long it keeps things. */
@Composable fun TrashSettings(snapshot:Snapshot,mutate:((Snapshot)->Snapshot)->Boolean,onOpen:()->Unit) {
    val language=LocalLanguage.current
    SettingBlock(nt("settings_trash")) {
        Hint("Как долго хранить удалённые записи, прежде чем удалить их навсегда.")
        WideButton(nt("settings_trash_open"),onClick=onOpen)
        trashRetentionOptions.forEach {days->
            ModeRow(if(days==0)translate(nt("settings_trash_never"),language) else translate(nt("settings_trash_days"),language).replace("{0}",days.toString()),null,snapshot.local.trashRetentionDays==days){mutate {s->s.copy(local=s.local.copy(trashRetentionDays=days))}}
        }
    }
}

/**
 * The help (D117): categories as tabs, the sections of the chosen one as sub-tabs. The first sub-tab is the overview — every
 * section with what it is about, a tap goes there. A section tells its text and then its icons: the drawing as on the
 * screen, its name, what it means. Opened from a hint ([focus]), the page starts at the icon the hint named and shows it
 * first; [prefer] is the category of the place the person came from. The search is in context (D118): each result shows
 * the found words inside their sentence, marked; the section it leads to opens with the same words marked.
 */
@Composable fun HelpPage(language:String,onDismiss:()->Unit,focus:String?=null,prefer:String?=null) {
    val tree=remember(language){helpTree(language)}
    val found=remember(tree,focus,prefer){focus?.let {findHelp(tree,it,prefer)}}
    var category by remember(focus){mutableStateOf(found?.category ?: tree.indexOfFirst {it.id==prefer}.coerceAtLeast(0))}
    // -1 is the overview of the category.
    var section by remember(focus){mutableStateOf(found?.section ?: -1)}
    var marked by remember(focus){mutableStateOf(found?.label)}
    var query by remember(focus){mutableStateOf(if(focus!=null && found==null)focus else "")}
    // What was searched for stays marked in the section a result led to.
    var sought by remember(focus){mutableStateOf("")}
    val mark=androidx.compose.ui.text.SpanStyle(background=MaterialTheme.colorScheme.tertiaryContainer,color=MaterialTheme.colorScheme.onTertiaryContainer,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
    fun marked(text:String,ranges:List<IntRange>)=androidx.compose.ui.text.buildAnnotatedString {append(text);ranges.forEach {addStyle(mark,it.first,it.last+1)}}
    @Composable fun glyph(icon:String,filled:Boolean){if(filled)FitIcon(icon) else RigSymbol(icon)}
    @Composable fun iconRow(icon:HelpIcon,highlighted:Boolean) {
        Surface(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,color=if(highlighted)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(40.dp),contentAlignment=Alignment.Center){glyph(icon.icon,icon.filled)}
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(icon.label,style=MaterialTheme.typography.titleSmall)
                    if(icon.about.isNotEmpty())androidx.compose.material3.Text(icon.about,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    @Composable fun link(title:String,about:androidx.compose.ui.text.AnnotatedString,onClick:()->Unit) {
        Surface(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(horizontal=14.dp,vertical=10.dp)) {
                androidx.compose.material3.Text(title,style=MaterialTheme.typography.titleSmall)
                if(about.isNotEmpty())androidx.compose.material3.Text(about,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
    fun lead(body:String)=body.lineSequence().firstOrNull {it.isNotBlank()}.orEmpty().let {line->Regex("^.{20,180}?[.!?。！？](?=\\s|$)").find(line)?.value ?: line.take(180)}
    SettingsPage(onDismissRequest=onDismiss,title={Text(nt("settings_help"))},text={
        Column(Modifier.fillMaxSize().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query,{query=it},label={Text("Поиск в справке")},singleLine=true,modifier=Modifier.fillMaxWidth())
            val results=remember(tree,query,prefer){searchHelp(tree,query,prefer)}
            if(query.isNotBlank() && results.isNotEmpty())Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.Text(textWith("Найдено: {0}",language,results.size),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                results.forEach {hit->
                    val c=tree[hit.place.category];val s=c.sections[hit.place.section]
                    link(c.title+" › "+s.title,marked(hit.text,listOf(hit.from until hit.to))){category=hit.place.category;section=hit.place.section;marked=hit.place.label;sought=query.trim();query=""}
                }
            } else {
                if(query.isNotBlank())androidx.compose.material3.Text(if(focus!=null && query==focus)textWith("Для «{0}» отдельного описания нет: показана вся справка.",language,focus) else translate("Ничего не найдено",language),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ScrollableTabRow(selectedTabIndex=category,edgePadding=0.dp) {
                    tree.forEachIndexed {index,c->LeadingIconTab(selected=index==category,onClick={category=index;section=-1;marked=null;sought=""},text={androidx.compose.material3.Text(c.title,maxLines=1)},icon={glyph(c.icon,c.filled)})}
                }
                val current=tree[category]
                ScrollableTabRow(selectedTabIndex=section+1,edgePadding=0.dp,containerColor=MaterialTheme.colorScheme.surfaceContainerLow) {
                    Tab(selected=section<0,onClick={section=-1;marked=null;sought=""},text={Text("Обзор")})
                    current.sections.forEachIndexed {index,s->Tab(selected=index==section,onClick={section=index;marked=null;sought=""},text={androidx.compose.material3.Text(s.title,maxLines=1)})}
                }
                key(category,section) {Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    val chosen=current.sections.getOrNull(section)
                    if(chosen==null) {
                        // The menu of the category: each section and what it is about; a tap goes there.
                        androidx.compose.material3.Text(current.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.semantics {heading()})
                        current.sections.forEachIndexed {index,s->link(s.title,androidx.compose.ui.text.AnnotatedString(lead(s.body))){section=index}}
                    } else {
                        // The icon the hint named comes first, so it is found without looking for it.
                        chosen.icons.firstOrNull {it.label==marked}?.let {iconRow(it,true)}
                        androidx.compose.material3.Text(chosen.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.semantics {heading()})
                        androidx.compose.material3.Text(marked(chosen.body,helpMatches(chosen.body,sought)),style=MaterialTheme.typography.bodyMedium)
                        if(chosen.icons.isNotEmpty()) {
                            Text("Значки",style=MaterialTheme.typography.titleSmall,modifier=Modifier.semantics {heading()})
                            chosen.icons.forEach {iconRow(it,it.label==marked)}
                        }
                    }
                }}
            }
        }
    },confirmButton={})
}

/** Settings → Help and about, as in Notio: the help, what this app is, the app journal. */
@Composable fun AboutSettings(platformName:String,onHelp:()->Unit,onAbout:()->Unit,onJournal:()->Unit) {
    val language=LocalLanguage.current
    SettingBlock(nt("settings_help")) {WideButton("Открыть справку",onClick=onHelp)}
    SettingBlock(nt("about_title")) {
        androidx.compose.material3.Text(translate(nt("about_version"),language).replace("{0}",BuildInfo.VERSION).replace("{1}",BuildInfo.NUMBER.toString()).replace("vibic777","Vibic777"),style=MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Text(translate("Дата сборки",language)+": "+BuildInfo.DATE+" · "+platformName,style=MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Text(translate("Контрибьютер",language)+": "+BuildInfo.CONTRIBUTOR,style=MaterialTheme.typography.bodyLarge)
        WideButton("О программе",onClick=onAbout)
    }
    SettingBlock(nt("app_log")) {
        Hint("Ошибки и то, что делало приложение при записи, копировании и синхронизации. Хранится только на этом устройстве; без содержимого записей.")
        WideButton(nt("app_log"),onClick=onJournal)
    }
}
