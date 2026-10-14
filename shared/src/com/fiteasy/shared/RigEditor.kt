package com.fiteasy.shared

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scene on top, everything else behind icons; every icon carries a translated hover / long-press hint (D081). */
@Composable fun RigEditor(platform:Platform,initial:RigClip?,onDismiss:()->Unit,onSave:(RigClip)->Unit,onEnableAnimations:()->Unit={}) {
    val original=remember {initial ?: RigClip(frames=listOf(RigFrame(time=0f,pose=rigBase())))}
    var clip by remember {mutableStateOf(original)};var frame by remember {mutableIntStateOf(0)}
    var joint by remember {mutableStateOf("rw")};var propId by remember {mutableStateOf<String?>(null)}
    var panel by remember {mutableStateOf("joint")};var tip by remember {mutableStateOf(false)}
    // The scene takes the whole screen; tools float over it and the detail sheet opens only on demand.
    var dock by remember {mutableStateOf(platform.readSecret("tool-dock")?.takeIf {it in toolDocks} ?: "bottom")};var hidden by remember {mutableStateOf(false)};var details by remember {mutableStateOf(false)}
    var female by remember {mutableStateOf<Boolean?>(null)};var playing by remember {mutableStateOf(false)}
    var undo by remember {mutableStateOf(emptyList<RigClip>())};var redo by remember {mutableStateOf(emptyList<RigClip>())};var error by remember {mutableStateOf("")}
    var discard by remember {mutableStateOf(false)}
    val language=LocalLanguage.current;fun text(key:String)=rigText(key,language)
    val foreground by platform.foreground.collectAsState(initial=true)
    val objects=panel=="objects"
    fun checkpoint(){undo=(undo+clip).takeLast(50);redo=emptyList()}
    fun change(value:RigClip,history:Boolean=true){if(history)checkpoint();clip=value;playing=false;error=""}
    fun restore(value:RigClip){clip=value;frame=frame.coerceAtMost(value.frames.lastIndex);playing=false;if(value.props.none {it.id==propId})propId=null}
    val current=clip.frames[frame.coerceIn(clip.frames.indices)].pose
    val reference=clip.frames.first().pose
    val displayed=if(clip.lockLengths)rigConstrain(reference,current)else rigAttached(reference,current)
    fun pose(value:RigPose,history:Boolean=true){change(clip.copy(frames=clip.frames.mapIndexed {i,f->if(i==frame)f.copy(pose=value)else f}),history)}
    fun editJoint(value:RigPoint){pose(rigEditJoint(reference,displayed,joint,value,clip.lockLengths),false)}
    fun close(){if(clip!=original)discard=true else onDismiss()}
    fun save(){runCatching {val saved=clip.copy(frames=clip.frames.map {it.copy(pose=rigAttached(reference,it.pose))});validateRig(saved);onSave(saved);onDismiss()}.onFailure {error=text("Invalid rig file")}}
    fun addFrame(){
        val gap=(clip.frames.map {it.time}+clip.duration).zipWithNext().filter {it.second-it.first>.01f}.maxByOrNull {it.second-it.first} ?: return
        val added=RigFrame(time=(gap.first+gap.second)/2,pose=displayed);change(clip.copy(frames=(clip.frames+added).sortedBy {it.time}));frame=clip.frames.indexOfFirst {it.id==added.id}
    }
    val latestClip by rememberUpdatedState(clip);val latestFrame by rememberUpdatedState(frame);val latestPose by rememberUpdatedState(displayed)
    val latestPropId by rememberUpdatedState(propId)
    fun select(hit:RigSelection?){if(hit!=null){hit.joint?.let {joint=it};propId=hit.propId;tip=hit.tip!=null}}
    LaunchedEffect(foreground){if(!foreground)playing=false}
    FullPage(::close){androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize()) {
        run {
            Row(verticalAlignment=Alignment.CenterVertically){
                RigAction("close","Close",::close)
                androidx.compose.material3.Text(if(error.isBlank())text("Rig editor")else error,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=1,color=if(error.isBlank())MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
                RigAction("undo","Undo",{redo=redo+clip;val last=undo.last();undo=undo.dropLast(1);restore(last)},enabled=undo.isNotEmpty())
                RigAction("redo","Redo",{undo=undo+clip;val next=redo.last();redo=redo.dropLast(1);restore(next)},enabled=redo.isNotEmpty())
                RigAction("save","Save",::save)
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
            val scene=minOf(maxWidth,maxHeight)
            RigCanvas(clip,running=playing && foreground,female=female,pose=if(playing)null else displayed,handles=!playing && !objects,selected=if(objects)"prop:$propId"else if(tip)"tip:$joint" else joint,side=scene,modifier=Modifier.align(Alignment.Center)
                .pointerInput(playing,objects){if(!playing)detectTapGestures {at->select(rigPick(latestClip,latestPose,at.x/size.width*100,at.y/size.height*100,objects,preferredPropId=latestPropId))}}
                .pointerInput(playing,objects){if(!playing){var dragging:RigSelection?=null
                    detectDragGestures(onDragStart={at->dragging=rigPick(latestClip,latestPose,at.x/size.width*100,at.y/size.height*100,objects,preferredPropId=latestPropId);if(dragging!=null){checkpoint();select(dragging)}},onDragEnd={dragging=null},onDragCancel={dragging=null}) {event,delta->
                        val selected=dragging
                        if(selected!=null){event.consume();val c=latestClip;val index=latestFrame;val p=latestPose
                            selected.propId?.let {id->clip=c.copy(props=c.props.map {if(it.id==id)rigDragProp(it,p,delta.x/size.width*100,delta.y/size.height*100)else it})}
                            if(selected.tip!=null){val key=selected.tip;clip=c.copy(frames=c.frames.mapIndexed {i,f->if(i==index)f.copy(pose=rigAim(f.pose,key,event.position.x/size.width*100,event.position.y/size.height*100))else f})}
                            else selected.joint?.let {key->val desired=rigDrag(p,key,event.position.x/size.width*100,event.position.y/size.height*100).points.getValue(key)
                                clip=c.copy(frames=c.frames.mapIndexed {i,f->if(i==index)f.copy(pose=rigEditJoint(c.frames.first().pose,p,key,desired,c.lockLengths))else f})}
                        }
                    }
                }})
            // Look, view, playback, key frames and tool tabs in one movable panel.
            ToolDock(dock,{dock=it;runCatching {platform.writeSecret("tool-dock",it)}},hidden,{hidden=it}){
                listOf(Triple(null,"joint","Skeleton"),Triple(true,"woman","Woman"),Triple(false,"man","Man")).forEach {(value,icon,label)->RigAction(icon,label,{female=value},selected=female==value)}
                RigDivider()
                // Front, both sides and the back turn the figure; the top view tips the scene and keeps the turn (D119).
                listOf(Triple(0f,"front","Front view"),Triple(90f,"side","Side view"),Triple(-90f,"side2","Other side view"),Triple(180f,"back","Back view")).forEach {(yaw,icon,label)->RigAction(icon,label,{pose(current.copy(yaw=yaw,pitch=0f))},selected=current.pitch==0f && current.yaw==yaw)}
                RigAction("top","Top view",{pose(current.copy(pitch=if(current.pitch==90f)0f else 90f))},selected=current.pitch==90f)
                RigDivider()
                RigAction(if(playing)"stop" else "play",if(playing)"Stop" else "Preview",{playing=!playing},enabled=LocalExerciseMotion.current)
                RigDivider()
                RigAction("previous","Previous frame",{frame--;playing=false},enabled=frame>0)
                Box(Modifier.size(40.dp),contentAlignment=Alignment.Center){androidx.compose.material3.Text("${frame+1}/${clip.frames.size}",style=MaterialTheme.typography.labelLarge)}
                RigAction("next","Next frame",{frame++;playing=false},enabled=frame<clip.frames.lastIndex)
                RigAction("add","Add frame",::addFrame,enabled=clip.frames.size<32)
                RigAction("delete","Delete frame",{val frames=clip.frames.filterIndexed {i,_->i!=frame}.toMutableList();frames[0]=frames[0].copy(time=0f);change(clip.copy(frames=frames));frame=0},enabled=clip.frames.size>1)
                RigAction("loop","Loop",{change(clip.copy(loop=!clip.loop))},selected=clip.loop)
                RigDivider()
                listOf("joint" to "Joint","objects" to "Objects","pose" to "Pose","timing" to "Timing","files" to "Files").forEach {(id,label)->RigAction(when(id){"objects"->"inventory";"pose"->"standing";"timing"->"time";"files"->"folder";else->"edit"},label,{if(panel==id)details=!details else {panel=id;details=true}},selected=panel==id && details)}
            }
            if(details)Surface(Modifier.align(if(dock=="bottom")Alignment.TopCenter else Alignment.BottomCenter).widthIn(max=720.dp).fillMaxWidth().heightIn(max=maxHeight*.45f).padding(6.dp),shape=MaterialTheme.shapes.medium,tonalElevation=6.dp,shadowElevation=3.dp,color=MaterialTheme.colorScheme.surface.copy(alpha=.95f)){Column(Modifier.padding(horizontal=8.dp,vertical=2.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){Spacer(Modifier.weight(1f));RigAction("close","Hide panel",{details=false})}
                when(panel){
                    "joint"->{
                        val point=displayed.points.getValue(joint)
                        Text(text(joint),style=MaterialTheme.typography.titleSmall)
                        Text(text("Drag a joint; depth sets front/back"),style=MaterialTheme.typography.bodySmall)
                        RigSlider("depth","Depth",point.z,-100f..100f,{editJoint(point.copy(z=it))},::checkpoint)
                        RigSlider("horizontal","Horizontal position",point.x,0f..100f,{editJoint(point.copy(x=it))},::checkpoint)
                        RigSlider("vertical","Vertical position",point.y,0f..100f,{editJoint(point.copy(y=it))},::checkpoint)
                        if(joint in setOf("lw","rw","la","ra"))RigSlider("angle","Hand / foot angle",current.angles[joint] ?: ((rigTipAngle(displayed,joint) ?: 0f)*180f/kotlin.math.PI.toFloat()),-180f..180f,{pose(current.copy(angles=current.angles+(joint to it)),false)},::checkpoint)
                        if(joint in setOf("la","ra"))RigSlider("toes","Toe bend",current.bends[joint] ?: 0f,-60f..60f,{pose(current.copy(bends=current.bends+(joint to it)),false)},::checkpoint)
                        if(joint in setOf("head","neck"))RigSlider("headturn","Head turn",current.headYaw,-90f..90f,{pose(current.copy(headYaw=it),false)},::checkpoint)
                        Row(Modifier.horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
                            if(joint in setOf("lw","rw","la","ra")){RigAction("pin","Pin contact",{pose(current.copy(contacts=if(joint in current.contacts)current.contacts-joint else current.contacts+(joint to rigWorldPoint(displayed,joint))))},selected=joint in current.contacts);RigDivider()}
                            if(joint in setOf("lw","rw")){listOf("open","pronated","supinated","neutral").forEach {grip->RigAction("grip-$grip",grip,{pose(current.copy(grips=current.grips+(joint to grip)))},selected=(current.grips[joint] ?: "open")==grip)};RigDivider()}
                            listOf("auto","back","front").forEach {layer->RigAction("layer-$layer",layer,{pose(current.copy(layers=current.layers+(joint to layer)))},selected=(current.layers[joint] ?: "auto")==layer)}
                        }
                    }
                    "objects"->{
                        // Tap an icon to add that object; it is selected at once and can be dragged on the scene.
                        FlowRow {rigEquipment.forEach {kind->RigAction(kind,kind,{val p=RigProp(kind=kind,width=if(kind in setOf("bar","mat","wall"))65f else 16f,height=if(kind=="chair")20f else 4f);change(clip.copy(props=clip.props+p));propId=p.id},enabled=clip.props.size<16)}}
                        if(clip.props.isNotEmpty()){HorizontalDivider();Row(Modifier.horizontalScroll(rememberScrollState())){clip.props.forEach {p->RigAction(p.kind,p.name.ifBlank {p.kind},{propId=p.id},selected=p.id==propId,translated=p.name.isBlank())}}}
                        clip.props.firstOrNull {it.id==propId}?.let {p->
                            fun update(value:RigProp,history:Boolean=false){change(clip.copy(props=clip.props.map {if(it.id==p.id)value else it}),history)}
                            Row(Modifier.horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
                                RigAction("delete","Remove object",{change(clip.copy(props=clip.props.filterNot {it.id==p.id}));propId=null})
                                RigAction("copy","Copy object",{val copied=p.copy(id=newId());change(clip.copy(props=clip.props+copied));propId=copied.id},enabled=clip.props.size<16)
                                RigDivider()
                                listOf("auto","back","front").forEach {layer->RigAction("layer-$layer",layer,{update(p.copy(layer=layer),true)},selected=p.layer==layer)}
                                RigDivider()
                                RigMenu("anchor","Anchor",p.anchor,listOf("scene","hands","lw","rw","la","ra","pelvis","chest","head"),::text){update(p.copy(anchor=it),true)}
                                if(p.kind=="bar")RigMenu("bar","bar",p.variant,listOf("straight","parallel","angled"),::text){update(p.copy(variant=it),true)}
                            }
                            OutlinedTextField(p.name,{update(p.copy(name=it.take(100)))},label={Text(text("Caption"))},leadingIcon={RigHint("label","Caption")},singleLine=true,modifier=Modifier.fillMaxWidth())
                            RigSlider("horizontal","Horizontal position",p.point.x,0f..100f,{update(p.copy(point=p.point.copy(x=it)))},::checkpoint)
                            RigSlider("vertical","Vertical position",p.point.y,0f..100f,{update(p.copy(point=p.point.copy(y=it)))},::checkpoint)
                            RigSlider("depth","Depth",p.point.z,-100f..100f,{update(p.copy(point=p.point.copy(z=it)))},::checkpoint)
                            RigSlider("width","Width",p.width,1f..100f,{update(p.copy(width=it))},::checkpoint)
                            RigSlider("height","Height",p.height,1f..100f,{update(p.copy(height=it))},::checkpoint)
                            RigSlider("angle","Rotation",p.angle,-180f..180f,{update(p.copy(angle=it))},::checkpoint)
                            RigSlider("rotation","Rotation per second",p.spin,-180f..180f,{update(p.copy(spin=it))},::checkpoint)
                        }
                    }
                    "pose"->{
                        Row(Modifier.horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
                            listOf("standing","seated","lying","plank","hanging").forEach {kind->RigAction(kind,kind,{pose(rigBase(kind))})}
                            RigDivider()
                            RigAction("mirror","Mirror left / right",{pose(rigMirror(current))})
                            RigAction("lock","Fixed bone lengths",{change(clip.copy(lockLengths=!clip.lockLengths))},selected=clip.lockLengths)
                        }
                        RigSlider("rotation","Figure rotation",current.yaw,-180f..180f,{pose(current.copy(yaw=it),false)},::checkpoint)
                        RigSlider("torso","Torso rotation",current.torsoYaw,-180f..180f,{pose(current.copy(torsoYaw=it),false)},::checkpoint)
                        if(!LocalExerciseMotion.current)TextButton(onClick=onEnableAnimations){Text(text("Enable animations in Appearance for preview"))}
                        if(LocalBatteryEconomy.current)Text(economyNotice,style=MaterialTheme.typography.bodySmall)
                    }
                    "timing"->{
                        if(frame>0){val low=clip.frames[frame-1].time+.001f;val high=if(frame==clip.frames.lastIndex)clip.duration else clip.frames[frame+1].time-.001f;if(high>low)RigSlider("time","Frame time",clip.frames[frame].time,low..high,{v->clip=clip.copy(frames=clip.frames.mapIndexed {i,f->if(i==frame)f.copy(time=v)else f})},::checkpoint,"s")}
                        RigSlider("duration","Duration",clip.duration,.5f..30f,{d->val ratio=d/clip.duration;clip=clip.copy(duration=d,frames=clip.frames.map {it.copy(time=it.time*ratio)})},::checkpoint,"s")
                    }
                    "files"->Row {
                        RigAction("export","Export JSON",{val c=clip;platform.launchFileTask {val bytes=withContext(Dispatchers.IO){exportRig(c)};platform.exportFile("pose.fiteasy-rig.json","application/json",bytes)}})
                        RigAction("svg","Export SVG",{val c=clip;val p=displayed;platform.launchFileTask {val bytes=withContext(Dispatchers.IO){exportRigSvg(c,p)};platform.exportFile("pose.svg","image/svg+xml",bytes)}})
                        RigAction("import","Import JSON",{platform.importFile {bytes->platform.launchFileTask {runCatching {withContext(Dispatchers.IO){importRig(bytes)}}.onSuccess {change(it);frame=0;propId=null}.onFailure {error=text("Invalid rig file");AppJournal.record("rig.import.failed",JournalLevel.WARN,it)}}}})
                    }
                }
            }}
            }
        }
    }}}
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text(text("Discard changes?"))},confirmButton={TextButton(onClick=onDismiss){Text(text("Discard"))}},dismissButton={TextButton(onClick={discard=false}){Text(text("Cancel"))}})
}
@Composable private fun RigDivider(){ToolGap()}
@Composable private fun RigMenu(icon:String,label:String,selected:String,options:List<String>,name:(String)->String,onSelect:(String)->Unit){
    var expanded by remember {mutableStateOf(false)}
    Box {Row(verticalAlignment=Alignment.CenterVertically){RigAction(icon,label,{expanded=true});Text(name(selected),style=MaterialTheme.typography.labelMedium)}
        DropdownMenu(expanded,{expanded=false}){options.forEach {value->DropdownMenuItem(text={Text(name(value))},onClick={onSelect(value);expanded=false})}}}
}
@Composable private fun RigSlider(icon:String,label:String,value:Float,range:ClosedFloatingPointRange<Float>,change:(Float)->Unit,checkpoint:()->Unit,unit:String=""){
    var active by remember {mutableStateOf(false)}
    Row(verticalAlignment=Alignment.CenterVertically){
        RigHint(icon,label)
        Slider(value.coerceIn(range.start,range.endInclusive),{if(!active){checkpoint();active=true};change(it)},valueRange=range,onValueChangeFinished={active=false},modifier=Modifier.weight(1f).padding(horizontal=6.dp))
        Text("%.1f".format(java.util.Locale.ROOT,value)+unit,Modifier.width(52.dp),style=MaterialTheme.typography.labelMedium)
    }
}
