package com.fiteasy.shared

data class ExerciseCard(val id:String,val technique:String,val category:String,val equipment:String,val index:Int,val source:String){
 fun exercise(language:String)=Exercise(name=exerciseNames(language)[index],instructions=exerciseInstructions(language)[index],seconds=30,catalogueId=id,technique=technique,rig=rigTemplate(technique))
}
// The seven after "toeraise" are gentle movements for ordinary days (D126): no load, a chair where balance needs one.
val exerciseCatalogue=listOf("plank","pushup","squat","raise","curl","pullup","heelraise","toeraise","armsup","twist","march","sidelegraise","necktilt","sitstand","kneeext").mapIndexed {i,k->ExerciseCard("fiteasy-rig-v1-$k",k,
    when(k){"heelraise","toeraise","armsup","twist","necktilt","kneeext"->"Mobility";"march"->"Walking";else->"Strength"},
    when(k){"raise","curl"->"Dumbbells";"pullup"->"Pull-up bar";"sidelegraise","sitstand","kneeext"->"Chair / support";else->"No equipment"},i,"FitEasy original editable example")}
fun searchExercises(query:String,category:String?,equipment:String?,language:String)=exerciseCatalogue.filter {c->(category==null || c.category==category) && (equipment==null || c.equipment==equipment) && (query.isBlank() || (c.exercise(language).name+" "+c.exercise(language).instructions).contains(query.trim(),true))}
private val names=mapOf(
"en" to "Static plank|Push-up|Squat|Dumbbell lateral raise|Dumbbell biceps curl|Overhand pull-up|Heel raise|Toe raise",
"ru" to "Планка — статика|Отжимание|Приседание|Разведение рук с гантелями|Сгибание рук с гантелями|Подтягивание прямым хватом|Подъём на носки|Подъём носков",
"be" to "Планка — статыка|Адцісканне|Прысяданне|Развядзенне рук з гантэлямі|Згінанне рук з гантэлямі|Падцягванне прамым хватам|Пад'ём на наскі|Пад'ём наскоў",
"fr" to "Planche statique|Pompes|Squat|Elevation laterale avec halteres|Flexion des biceps avec halteres|Traction en pronation|Montée sur la pointe des pieds|Relevé des orteils",
"de" to "Statischer Plank|Liegestutz|Kniebeuge|Seitheben mit Kurzhanteln|Bizepscurl mit Kurzhanteln|Klimmzug im Obergriff|Fersenheben|Zehenheben",
"es" to "Plancha estatica|Flexion|Sentadilla|Elevacion lateral con mancuernas|Curl de biceps con mancuernas|Dominada con agarre prono|Elevación de talones|Elevación de puntas",
"it" to "Plank statico|Piegamento|Squat|Alzata laterale con manubri|Curl con manubri|Trazione con presa prona|Sollevamento dei talloni|Sollevamento delle punte",
"zh" to "静态平板支撑|俯卧撑|深蹲|哑铃侧平举|哑铃弯举|正握引体向上|提踵|勾脚尖",
"ja" to "静止プランク|腕立て伏せ|スクワット|ダンベルサイドレイズ|ダンベルカール|順手懸垂|かかと上げ|つま先上げ",
"ko" to "정적 플랭크|팔굽혀펴기|스쿼트|덤벨 사이드 레이즈|덤벨 컬|오버핸드 풀업|뒤꿈치 들기|발끝 들기")
fun exerciseNames(language:String)=(names[language] ?: names.getValue("en")).split('|')+(gentleNames[language] ?: gentleNames.getValue("en")).split('|')
private val footInstructions=mapOf(
"en" to "Stand next to a wall or chair and hold it lightly. Rise slowly onto the balls of your feet, pause, then lower the heels with control. Stop if it hurts.|Stand with a support, heels on the floor. Lift the front of both feet, pause, then lower slowly. Stop if it hurts.",
"ru" to "Встаньте рядом со стеной или стулом и слегка придерживайтесь. Медленно поднимитесь на носки, задержитесь и плавно опустите пятки. При боли остановитесь.|Встаньте с опорой, пятки на полу. Поднимите носки обеих стоп, задержитесь и медленно опустите. При боли остановитесь.",
"be" to "Станьце побач са сцяной або крэслам і злёгку прытрымлівайцеся. Павольна падыміцеся на наскі, затрымайцеся і плаўна апусціце пяткі. Пры болі спыніцеся.|Станьце з апорай, пяткі на падлозе. Падыміце наскі абедзвюх ступняў, затрымайцеся і павольна апусціце. Пры болі спыніцеся.",
"fr" to "Tenez-vous près d'un mur ou d'une chaise et appuyez-vous légèrement. Montez lentement sur la pointe des pieds, marquez une pause, puis redescendez les talons en contrôlant. Arrêtez en cas de douleur.|Debout avec un appui, talons au sol. Relevez l'avant des deux pieds, marquez une pause, puis redescendez lentement. Arrêtez en cas de douleur.",
"de" to "Stellen Sie sich neben eine Wand oder einen Stuhl und halten Sie sich leicht fest. Heben Sie die Fersen langsam, kurz halten, dann kontrolliert absenken. Bei Schmerzen aufhören.|Mit Halt stehen, Fersen am Boden. Beide Fußspitzen anheben, kurz halten, dann langsam absenken. Bei Schmerzen aufhören.",
"es" to "Colóquese junto a una pared o silla y sujétese ligeramente. Suba despacio a las puntas de los pies, haga una pausa y baje los talones con control. Pare si duele.|De pie con apoyo, talones en el suelo. Levante la parte delantera de ambos pies, haga una pausa y baje despacio. Pare si duele.",
"it" to "Mettetevi accanto a una parete o a una sedia e tenetevi leggermente. Salite lentamente sugli avampiedi, fate una pausa, poi abbassate i talloni con controllo. Fermatevi se fa male.|In piedi con un appoggio, talloni a terra. Sollevate la parte anteriore di entrambi i piedi, fate una pausa, poi abbassate lentamente. Fermatevi se fa male.",
"zh" to "站在墙或椅子旁并轻轻扶住。慢慢踮起脚尖，稍作停顿，再有控制地放下脚跟。如有疼痛请停止。|扶住支撑物站立，脚跟着地。抬起双脚前掌，稍作停顿，再慢慢放下。如有疼痛请停止。",
"ja" to "壁や椅子のそばに立ち、軽くつかまります。ゆっくりつま先立ちになり、少し止めてから、かかとをゆっくり下ろします。痛みがあれば中止してください。|支えにつかまって立ち、かかとを床につけます。両足のつま先を上げ、少し止めてからゆっくり下ろします。痛みがあれば中止してください。",
"ko" to "벽이나 의자 옆에 서서 가볍게 잡습니다. 천천히 발끝으로 올라서 잠시 멈춘 뒤 뒤꿈치를 천천히 내립니다. 아프면 중단하세요.|지지물을 잡고 서서 뒤꿈치를 바닥에 둡니다. 양발 앞부분을 들어 올려 잠시 멈춘 뒤 천천히 내립니다. 아프면 중단하세요."
)
/** How each card is done, in the order of [exerciseCatalogue]: the first six, the two for the feet, the seven gentle ones. */
fun exerciseInstructions(language:String)=listOf(firstInstructions,footInstructions,gentleInstructions).flatMap {(it[language] ?: it.getValue("en")).split('|')}
