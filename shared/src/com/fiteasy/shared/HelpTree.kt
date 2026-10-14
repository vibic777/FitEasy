package com.fiteasy.shared

/**
 * The help as a tree (D117): categories as tabs, their sections as sub-tabs, and in every section the icons met there —
 * each drawn as on the screen, with its name and what it means. The texts of the sections are the help articles the app
 * already had in ten languages; the names of the icons are the very captions the buttons carry, so a hint leads to its place.
 */
data class HelpIcon(val icon:String,val label:String,val about:String,val filled:Boolean=false)
data class HelpSection(val title:String,val body:String,val icons:List<HelpIcon> = emptyList())
data class HelpCategory(val id:String,val icon:String,val title:String,val sections:List<HelpSection>,val filled:Boolean=false)
/** Where a hint was found: the category, the section in it and the name of the icon. */
data class HelpPlace(val category:Int,val section:Int,val label:String?)

/** What each drawing means wherever it is met; order of [iconMeaningRows]. The last one explains the small plus on an icon. */
private val meaningIcons="today|food|water|activity|progress|notes|calendar|cards|bell|loop|profile|save|close|edit|delete|copy|previous|add|search|help|product|dish|star|time|list|steps|dumbbell|play|pause|skip|skip2|stop|scale|target|energy|report|text|drawing|folder|tag|undo|link|lock|keypad|key|pattern|fingerprint|send|unlock|export|import|document|palette|language|units|sun|moon|settings|image|info|+".split('|')
internal val iconMeaningRows=mapOf(
"ru" to "Сводка дня|Еда и её состав|Вода и другие напитки|Движение и тренировки|Графики и замеры|Заметки, списки и рисунки|Выбор дня, периода или расписания|Что показывать и в каком порядке|Напоминания и уведомления|Синхронизация, повтор или обновление|Профиль и личные данные|Сохранить или подтвердить|Закрыть или отменить без сохранения|Изменить или открыть для правки|Удалить: запись уходит в корзину|Сделать копию|Назад или предыдущее|Добавить новое|Поиск|Справка или контрольный вопрос|Отдельный продукт из каталога|Готовое блюдо из каталога|Избранное|Время, ожидание или отложить|Список записей|Шаги|Тренировка или комплекс упражнений|Начать, продолжить или прослушать|Пауза или заглушить звук|Пропустить подход|Пропустить упражнение|Остановить, завершить или прервать|Вес и замеры тела|Цели|Оценка расхода энергии|Отчёт PDF или CSV|Текстовая заметка|Рисунок|Папки заметок или выбор файла|Теги|Отменить последнее действие|Соединить или объединить|Заблокировать приложение, защита|PIN-код|Пароль или ключ|Графический ключ|Вход по биометрии|Отправить код или файл|Снять блокировку|Экспорт, резервная копия|Импорт, восстановление|Документ или журнал приложения|Оформление и цвета|Язык|Единицы измерения|Светлая тема|Тёмная тема|Настройки или системный вариант|Фон раздела|Сведения о приложении|Плюс на значке: добавить запись этого вида",
"en" to "Summary of the day|Food and what it contains|Water and other drinks|Movement and workouts|Charts and measurements|Notes, lists and drawings|Choose a day, a period or a schedule|What to show and in which order|Reminders and notifications|Synchronize, repeat or refresh|Profile and personal details|Save or confirm|Close or cancel without saving|Edit or open for editing|Delete: the record goes to the trash|Make a copy|Back or previous|Add something new|Search|Help or a security question|A single product from the catalogue|A ready dish from the catalogue|Favorites|Time, waiting or snooze|A list of records|Steps|A workout or a set of exercises|Start, continue or listen|Pause or silence the sound|Skip the set|Skip the exercise|Stop, finish or interrupt|Weight and body measurements|Goals|Estimate of energy use|PDF or CSV report|A text note|A drawing|Note folders or choosing a file|Tags|Undo the last action|Connect or merge|Lock the app, protection|PIN code|Password or key|Pattern lock|Sign in with biometrics|Send a code or a file|Remove the lock|Export, backup|Import, restore|A document or the app journal|Appearance and colors|Language|Units of measurement|Light theme|Dark theme|Settings or the system option|Background of a section|About the app|A plus on an icon: add a record of that kind",
"be" to "Зводка дня|Ежа і яе склад|Вада і іншыя напоі|Рух і трэніроўкі|Графікі і замеры|Нататкі, спісы і малюнкі|Выбар дня, перыяду або раскладу|Што паказваць і ў якім парадку|Напаміны і апавяшчэнні|Сінхранізацыя, паўтор або абнаўленне|Профіль і асабістыя даныя|Захаваць або пацвердзіць|Закрыць або скасаваць без захавання|Змяніць або адкрыць для праўкі|Выдаліць: запіс трапляе ў кошык|Зрабіць копію|Назад або папярэдняе|Дадаць новае|Пошук|Даведка або кантрольнае пытанне|Асобны прадукт з каталога|Гатовая страва з каталога|Абранае|Час, чаканне або адкласці|Спіс запісаў|Крокі|Трэніроўка або комплекс практыкаванняў|Пачаць, працягнуць або праслухаць|Паўза або заглушыць гук|Прапусціць падыход|Прапусціць практыкаванне|Спыніць, завяршыць або перапыніць|Вага і замеры цела|Мэты|Ацэнка выдатку энергіі|Справаздача PDF або CSV|Тэкставая нататка|Малюнак|Папкі нататак або выбар файла|Тэгі|Скасаваць апошняе дзеянне|Злучыць або аб’яднаць|Заблакіраваць праграму, абарона|PIN-код|Пароль або ключ|Графічны ключ|Уваход па біяметрыі|Адправіць код або файл|Зняць блакіроўку|Экспарт, рэзервовая копія|Імпарт, аднаўленне|Дакумент або журнал праграмы|Афармленне і колеры|Мова|Адзінкі вымярэння|Светлая тэма|Цёмная тэма|Налады або сістэмны варыянт|Фон раздзела|Звесткі пра праграму|Плюс на значку: дадаць запіс гэтага віду",
"fr" to "Résumé de la journée|Les aliments et leur composition|Eau et autres boissons|Mouvement et entraînements|Graphiques et mesures|Notes, listes et dessins|Choisir un jour, une période ou un planning|Quoi afficher et dans quel ordre|Rappels et notifications|Synchroniser, répéter ou actualiser|Profil et données personnelles|Enregistrer ou confirmer|Fermer ou annuler sans enregistrer|Modifier ou ouvrir pour modification|Supprimer : l'entrée va dans la corbeille|Faire une copie|Retour ou précédent|Ajouter du nouveau|Recherche|Aide ou question de sécurité|Un aliment du catalogue|Un plat préparé du catalogue|Favoris|Heure, attente ou report|Une liste d'entrées|Pas|Un entraînement ou une série d'exercices|Démarrer, continuer ou écouter|Pause ou couper le son|Passer la série|Passer l'exercice|Arrêter, terminer ou interrompre|Poids et mensurations|Objectifs|Estimation de la dépense d'énergie|Rapport PDF ou CSV|Une note de texte|Un dessin|Dossiers de notes ou choix d'un fichier|Étiquettes|Annuler la dernière action|Relier ou fusionner|Verrouiller l'application, protection|Code PIN|Mot de passe ou clé|Schéma de verrouillage|Connexion par biométrie|Envoyer un code ou un fichier|Retirer le verrouillage|Exportation, sauvegarde|Importation, restauration|Un document ou le journal de l'application|Apparence et couleurs|Langue|Unités de mesure|Thème clair|Thème sombre|Paramètres ou option du système|Fond d'une rubrique|À propos de l'application|Un plus sur une icône : ajouter une entrée de ce type",
"de" to "Übersicht des Tages|Essen und seine Zusammensetzung|Wasser und andere Getränke|Bewegung und Training|Diagramme und Messungen|Notizen, Listen und Zeichnungen|Tag, Zeitraum oder Plan wählen|Was gezeigt wird und in welcher Reihenfolge|Erinnerungen und Benachrichtigungen|Synchronisieren, wiederholen oder aktualisieren|Profil und persönliche Angaben|Speichern oder bestätigen|Schließen oder ohne Speichern abbrechen|Bearbeiten oder zum Bearbeiten öffnen|Löschen: Der Eintrag kommt in den Papierkorb|Kopie anlegen|Zurück oder vorheriges|Neues hinzufügen|Suche|Hilfe oder Sicherheitsfrage|Ein einzelnes Lebensmittel aus dem Katalog|Ein fertiges Gericht aus dem Katalog|Favoriten|Zeit, Warten oder später erinnern|Eine Liste von Einträgen|Schritte|Ein Training oder eine Übungsfolge|Starten, fortsetzen oder anhören|Pause oder Ton stummschalten|Satz überspringen|Übung überspringen|Stoppen, beenden oder abbrechen|Gewicht und Körpermaße|Ziele|Schätzung des Energieverbrauchs|PDF- oder CSV-Bericht|Eine Textnotiz|Eine Zeichnung|Notizordner oder Dateiauswahl|Tags|Letzte Aktion rückgängig machen|Verbinden oder zusammenführen|App sperren, Schutz|PIN-Code|Passwort oder Schlüssel|Sperrmuster|Anmeldung per Biometrie|Code oder Datei senden|Sperre aufheben|Export, Sicherung|Import, Wiederherstellung|Ein Dokument oder das App-Protokoll|Darstellung und Farben|Sprache|Maßeinheiten|Helles Design|Dunkles Design|Einstellungen oder Systemvariante|Hintergrund eines Bereichs|Über die App|Ein Plus am Symbol: einen Eintrag dieser Art hinzufügen",
"es" to "Resumen del día|La comida y su composición|Agua y otras bebidas|Movimiento y entrenamientos|Gráficos y medidas|Notas, listas y dibujos|Elegir un día, un periodo o un horario|Qué mostrar y en qué orden|Recordatorios y notificaciones|Sincronizar, repetir o actualizar|Perfil y datos personales|Guardar o confirmar|Cerrar o cancelar sin guardar|Editar o abrir para editar|Eliminar: el registro va a la papelera|Hacer una copia|Atrás o anterior|Añadir algo nuevo|Búsqueda|Ayuda o pregunta de seguridad|Un alimento del catálogo|Un plato preparado del catálogo|Favoritos|Hora, espera o posponer|Una lista de registros|Pasos|Un entrenamiento o una serie de ejercicios|Empezar, continuar o escuchar|Pausa o silenciar el sonido|Saltar la serie|Saltar el ejercicio|Detener, terminar o interrumpir|Peso y medidas corporales|Objetivos|Estimación del gasto de energía|Informe PDF o CSV|Una nota de texto|Un dibujo|Carpetas de notas o elegir un archivo|Etiquetas|Deshacer la última acción|Conectar o combinar|Bloquear la aplicación, protección|Código PIN|Contraseña o clave|Patrón de bloqueo|Entrar con biometría|Enviar un código o un archivo|Quitar el bloqueo|Exportación, copia de seguridad|Importación, restauración|Un documento o el registro de la aplicación|Apariencia y colores|Idioma|Unidades de medida|Tema claro|Tema oscuro|Ajustes o la opción del sistema|Fondo de una sección|Acerca de la aplicación|Un más en un icono: añadir un registro de ese tipo",
"it" to "Riepilogo del giorno|Il cibo e la sua composizione|Acqua e altre bevande|Movimento e allenamenti|Grafici e misure|Note, elenchi e disegni|Scegliere un giorno, un periodo o un programma|Che cosa mostrare e in quale ordine|Promemoria e notifiche|Sincronizzare, ripetere o aggiornare|Profilo e dati personali|Salvare o confermare|Chiudere o annullare senza salvare|Modificare o aprire per la modifica|Eliminare: la voce va nel cestino|Fare una copia|Indietro o precedente|Aggiungere qualcosa di nuovo|Ricerca|Guida o domanda di sicurezza|Un alimento del catalogo|Un piatto pronto del catalogo|Preferiti|Ora, attesa o posticipo|Un elenco di voci|Passi|Un allenamento o una serie di esercizi|Avviare, continuare o ascoltare|Pausa o silenziare il suono|Saltare la serie|Saltare l'esercizio|Fermare, terminare o interrompere|Peso e misure del corpo|Obiettivi|Stima del consumo di energia|Rapporto PDF o CSV|Una nota di testo|Un disegno|Cartelle delle note o scelta di un file|Tag|Annullare l'ultima azione|Collegare o unire|Bloccare l'app, protezione|Codice PIN|Password o chiave|Sequenza di sblocco|Accesso con biometria|Inviare un codice o un file|Togliere il blocco|Esportazione, backup|Importazione, ripristino|Un documento o il registro dell'app|Aspetto e colori|Lingua|Unità di misura|Tema chiaro|Tema scuro|Impostazioni o l'opzione di sistema|Sfondo di una sezione|Informazioni sull'app|Un più su un'icona: aggiungere una voce di quel tipo",
"zh" to "当天概览|食物及其成分|水和其他饮品|运动与训练|图表与测量|笔记、清单和绘图|选择日期、时段或日程|显示哪些内容及其顺序|提醒和通知|同步、重复或刷新|个人资料和个人信息|保存或确认|关闭或不保存取消|编辑或打开以修改|删除：记录进入回收站|创建副本|返回或上一个|添加新内容|搜索|帮助或安全问题|目录中的单个食材|目录中的成品菜肴|收藏|时间、等待或稍后提醒|记录列表|步数|一次训练或一组动作|开始、继续或试听|暂停或静音|跳过本组|跳过该动作|停止、结束或中断|体重和身体测量|目标|能量消耗估算|PDF 或 CSV 报告|文字笔记|绘图|笔记文件夹或选择文件|标签|撤销上一步操作|连接或合并|锁定应用，保护|PIN 码|密码或密钥|图案锁|生物识别登录|发送验证码或文件|解除锁定|导出，备份|导入，恢复|文档或应用日志|外观和颜色|语言|计量单位|浅色主题|深色主题|设置或跟随系统|分区背景|关于应用|图标上的加号：添加该类记录",
"ja" to "その日のまとめ|食べ物とその内容|水とその他の飲み物|運動とトレーニング|グラフと測定|メモ、リスト、手書き|日、期間、スケジュールを選ぶ|何をどの順で表示するか|リマインダーと通知|同期、繰り返し、更新|プロフィールと個人情報|保存または確認|閉じる、保存せずにキャンセル|編集する、編集用に開く|削除：記録はごみ箱へ移動|コピーを作る|戻る、前へ|新しく追加|検索|ヘルプまたは秘密の質問|カタログの食材ひとつ|カタログの料理|お気に入り|時刻、待機、後で通知|記録の一覧|歩数|トレーニングまたはメニュー|開始、再開、試聴|一時停止または消音|セットをスキップ|種目をスキップ|停止、終了、中断|体重と体の測定|目標|消費エネルギーの推定|PDF または CSV レポート|テキストメモ|手書き|メモのフォルダーまたはファイル選択|タグ|直前の操作を取り消す|接続または統合|アプリをロック、保護|PIN コード|パスワードまたはキー|パターンロック|生体認証でログイン|コードやファイルを送信|ロックを解除|エクスポート、バックアップ|インポート、復元|文書またはアプリの記録|表示設定と色|言語|単位|ライトテーマ|ダークテーマ|設定またはシステムに合わせる|セクションの背景|アプリについて|アイコンのプラス：その種類の記録を追加",
"ko" to "하루 요약|음식과 그 성분|물과 다른 음료|움직임과 운동|그래프와 측정|메모, 목록, 그림|날짜, 기간 또는 일정 선택|무엇을 어떤 순서로 보여 줄지|알림과 통지|동기화, 반복 또는 새로 고침|프로필과 개인 정보|저장 또는 확인|닫기 또는 저장하지 않고 취소|편집하거나 편집용으로 열기|삭제: 기록이 휴지통으로 이동|사본 만들기|뒤로 또는 이전|새로 추가|검색|도움말 또는 보안 질문|카탈로그의 식재료 하나|카탈로그의 완성 요리|즐겨찾기|시간, 대기 또는 미루기|기록 목록|걸음 수|운동 또는 운동 묶음|시작, 계속 또는 들어 보기|일시 정지 또는 소리 끄기|세트 건너뛰기|운동 건너뛰기|정지, 종료 또는 중단|체중과 신체 측정|목표|에너지 소비 추정|PDF 또는 CSV 보고서|텍스트 메모|그림|메모 폴더 또는 파일 선택|태그|마지막 동작 취소|연결 또는 병합|앱 잠금, 보호|PIN 코드|비밀번호 또는 키|패턴 잠금|생체 인증 로그인|코드나 파일 보내기|잠금 해제|내보내기, 백업|가져오기, 복원|문서 또는 앱 기록|화면 설정과 색상|언어|측정 단위|밝은 테마|어두운 테마|설정 또는 시스템 설정 따르기|항목 배경|앱 정보|아이콘의 더하기: 그 종류의 기록 추가"
).mapValues {(_,row)->meaningIcons.zip(row.split('|')).toMap()}

/** What the drawing [icon] means, in [language]; an icon with the small plus says so too. */
fun iconMeaning(icon:String,language:String):String {
    val row=iconMeaningRows[language] ?: iconMeaningRows.getValue("en")
    val plus=icon.length>1 && icon.endsWith("+")
    val base=row[if(plus)icon.dropLast(1) else icon].orEmpty()
    return if(plus)listOf(base,row.getValue("+")).filter {it.isNotEmpty()}.joinToString(". ") else base
}

// A section's icons: caption as the button carries it (the Russian source text) to the name of its drawing.
private val topIcons=listOf("Напоминания" to "bell","Синхронизация" to "loop","Профиль" to "profile","Открыть день" to "calendar","Настроить карточки" to "cards")
private val commonIcons=listOf("Сохранить" to "save","Отмена" to "close","Закрыть" to "close","Изменить" to "edit","Удалить" to "delete","Копировать" to "copy","Назад" to "previous","Добавить" to "add","Поиск" to "search","Справка" to "help")
private val foodIcons=listOf("+ Еда" to "food+","Продукты" to "product","Блюда" to "dish","Избранное" to "star","Недавние" to "time","+ Своё" to "add","+ Рецепт" to "list+","Приёмы пищи" to "food","Продукты и блюда" to "search")
private val waterIcons=listOf("+ Напиток" to "water+","+ Другой напиток" to "water+","Записи напитков" to "list")
private val activityIcons=listOf("+ Активность" to "activity+","+ Шаги вручную" to "steps+","+ Свой комплекс" to "dumbbell+","Тренировка" to "dumbbell","Справочник упражнений" to "list","Начать" to "play","Пауза" to "pause","Пропустить подход" to "skip","Пропустить упражнение" to "skip2","Завершить" to "stop")
private val progressIcons=listOf("+ Вес / замер" to "scale+","Период и точка отсчёта" to "calendar","Вид для всех графиков" to "progress","Цели" to "target","Рассчитать ориентир энергии" to "energy","PDF / CSV отчёт" to "report")
private val noteIcons=listOf("Текст" to "text","Рисунок" to "drawing","Список" to "list","Все заметки" to "notes","По папкам" to "folder","Все теги" to "tag","Штампы времени" to "time","Отменить штрих" to "undo","В корзину" to "delete")
private val reminderIcons=listOf("+ Напоминание" to "bell+","Расписания" to "calendar","Календарь" to "calendar","Создать расписание" to "calendar+","Назначить напоминание" to "bell","Подтвердить" to "save","Отложить" to "time","Прервать" to "stop","Заглушить" to "pause","Прослушать" to "play")
private val syncIcons=listOf("Ждать устройство" to "time","Найти устройство" to "search","Синхронизировать…" to "loop","Слияние" to "link","Добавить копиями" to "copy","Замещение" to "loop")
private val securityIcons=listOf("Заблокировать" to "lock","PIN-код" to "keypad","Пароль" to "key","Графический ключ" to "pattern","Биометрия" to "fingerprint","Контрольный вопрос" to "help","TOTP" to "time","Email" to "send","Отключить блокировку" to "unlock")
private val dataIcons=listOf("Экспорт всех данных" to "export","Импорт всех данных" to "import","Экспорт базы" to "export","Импорт базы" to "import","Корзина" to "delete","Создать демонстрацию" to "add","Обнулить базу" to "delete","App journal" to "document")
private val rightsIcons=listOf("Разрешить" to "save","Отозвать" to "stop","Открыть в системе" to "settings")
private val settingIconList=listOf("Личные данные" to "profile","Оформление" to "palette","Язык" to "language","Единицы" to "units","Светлая" to "sun","Тёмная" to "moon","Системная" to "settings","Accent / RGB" to "palette","Section backgrounds" to "image","О программе" to "info")

/** The whole help in [language]. */
fun helpTree(language:String):List<HelpCategory> {
    fun t(text:String)=translate(text,language)
    fun icons(list:List<Pair<String,String>>)=list.map {(caption,icon)->HelpIcon(icon,t(caption),iconMeaning(icon,language))}
    fun section(article:HelpArticle,list:List<Pair<String,String>> = emptyList())=HelpSection(article.title,article.body,icons(list))
    // What 0.4.36 added is told in the section it belongs to (D120).
    fun more(s:HelpSection,index:Int)=s.copy(body=s.body+"\n\n"+helpAddition(index,language))
    val base=baseHelp(language);val delivery=deliveryHelp(language);val settings=settingsHelp(language);val period=periodNoteHelp(language)
    val navigation=mainSections.map {(id,label)->HelpIcon(id,t(label),iconMeaning(id,language),filled=true)}
    val notice=legalNotice(language)
    // The synchronization pages are told in the words of the screen itself, so the help cannot drift from it (D105, D113, D116).
    val syncModes=listOf("Одинаковые записи сопоставляются, у каждой побеждает более новое изменение; недостающее добавляется.",nt("sync_mode_copies_hint"),"В выбранных разделах принимающее устройство становится точной копией отправляющего: его записи этих разделов сначала удаляются. Остальные разделы не меняются.","Замещение возможно только в одну сторону.")
    val syncRules=listOf("Заданы все три: синхронизация начинается сразу и идёт без подтверждения на другом устройстве.","Пока хотя бы один выбор делается при синхронизации, другое устройство обязательно спрашивает подтверждение.","Пока приложение запущено, в том числе в трее, это устройство принимает синхронизацию без открытой страницы.","Пока приложение на экране, это устройство принимает синхронизацию без открытой страницы. В фоне телефон не ждёт: так бережётся батарея.")
    return listOf(
        HelpCategory("today","today",t("Сегодня"),listOf(section(base[0],emptyList()).copy(icons=navigation+icons(topIcons)),HelpSection(t("Общие действия"),t(helpIntro),icons(commonIcons))),filled=true),
        HelpCategory("food","food",t("Питание"),listOf(section(base[1],foodIcons),section(base[2],foodIcons.filter {it.first in setOf("Блюда","+ Рецепт","+ Своё","Избранное")})),filled=true),
        HelpCategory("water","water",t("Вода"),listOf(section(base[3],waterIcons)),filled=true),
        HelpCategory("activity","activity",t("Активность"),listOf(section(base[4],activityIcons.take(5)),section(workoutHelp(language),activityIcons.drop(3))),filled=true),
        HelpCategory("progress","progress",t("Прогресс"),listOf(section(base[5],progressIcons),section(period[0],progressIcons.filter {it.second=="calendar"}),section(period[1],listOf("Календарь" to "calendar"))),filled=true),
        HelpCategory("notes","notes",t("Заметки"),listOf(section(period[2],noteIcons)),filled=true),
        HelpCategory("reminders","bell",t("Напоминания"),listOf(more(section(delivery[0],reminderIcons),1),section(plannerHelp(language),reminderIcons.take(5)))),
        HelpCategory("sync","loop",t("Синхронизация"),listOf(
            HelpSection(t("Синхронизация"),listOf("settings_sync_hint","sync_roles_hint").joinToString("\n\n") {t(nt(it))},icons(syncIcons.take(3))),
            HelpSection(t(nt("sync_mode")),syncModes.joinToString("\n\n") {t(it)},icons(syncIcons.drop(3))),
            HelpSection(t(nt("sync_connections")),(syncRules+syncPortHelp).joinToString("\n\n") {t(it)},icons(syncIcons.drop(2).take(1))))),
        HelpCategory("security","lock",t("Защита"),listOf(more(more(section(base[6],securityIcons),0),2),section(delivery[1],securityIcons.take(1)))),
        HelpCategory("data","database",t("Данные"),listOf(section(base[8],dataIcons.take(2)),section(transferHelp(language),dataIcons.drop(2).take(2)),section(settings[1],dataIcons.filter {it.first=="Обнулить базу"}),section(settings[2],dataIcons.filter {it.first=="Создать демонстрацию"}),section(journalHelp(language),dataIcons.takeLast(1)))),
        HelpCategory("settings","profile",t("Профиль и настройки"),listOf(section(settings[0],settingIconList.take(4)),section(base[9],settingIconList.drop(4)),section(appearanceHelp(language),settingIconList.filter {it.second in setOf("palette","image")}),HelpSection(t("Права"),rightsHelp(language),icons(rightsIcons)),section(delivery[3]))),
        HelpCategory("about","info",t(nt("about_title")),listOf(HelpSection(notice.title,notice.body,icons(listOf("О программе" to "info","Справка" to "help"))))))
}

/** How the hints and the help work together; shown with the common actions. */
internal const val helpIntro="Удержите палец на значке или наведите на него мышь: на пять секунд появится подсказка с его названием. Нажмите на подсказку — откроется это место справки."

/** The category of the help that belongs to where the person is in the app. */
fun helpCategoryFor(screen:String,settingsSection:String):String = when {
    screen!="profile"->screen
    settingsSection in setOf("reminders","sync","security")->settingsSection
    settingsSection in setOf("data","trash")->"data"
    settingsSection=="help"->"about"
    else->"settings"
}

/** The place of the icon a hint named: looked for in the [prefer]red category first, then everywhere. */
fun findHelp(tree:List<HelpCategory>,hint:String,prefer:String?=null):HelpPlace? {
    val wanted=hint.trim()
    fun within(index:Int):HelpPlace? {
        tree[index].sections.forEachIndexed {s,section->section.icons.firstOrNull {it.label.equals(wanted,ignoreCase=true)}?.let {return HelpPlace(index,s,it.label)}}
        return null
    }
    val first=tree.indexOfFirst {it.id==prefer}
    if(first>=0)within(first)?.let {return it}
    tree.indices.forEach {index->within(index)?.let {return it}}
    return null
}

/** One found place with the words around what was found: [text] is the fragment, [from]..[to] the match inside it. */
data class HelpHit(val place:HelpPlace,val text:String,val from:Int,val to:Int)

/** Where [query] stands in [text], ignoring case; every occurrence, none overlapping. */
fun helpMatches(text:String,query:String):List<IntRange> {
    val q=query.trim();if(q.isEmpty())return emptyList()
    val found=ArrayList<IntRange>();var at=text.indexOf(q,0,ignoreCase=true)
    while(at>=0 && found.size<200){found+=at until at+q.length;at=text.indexOf(q,at+q.length,ignoreCase=true)}
    return found
}

/**
 * The search of the help in context (D118): every place that mentions [query] with the words around it — a section by its
 * title, an icon by its name or meaning, and the text of a section, up to two fragments of each. The category the person
 * came from ([prefer]) is listed first.
 */
fun searchHelp(tree:List<HelpCategory>,query:String,prefer:String?=null):List<HelpHit> {
    val q=query.trim();if(q.isEmpty())return emptyList()
    fun hit(place:HelpPlace,text:String):HelpHit? = helpMatches(text,q).firstOrNull()?.let {HelpHit(place,text,it.first,it.last+1)}
    fun fragments(place:HelpPlace,body:String):List<HelpHit> = helpMatches(body,q).take(2).map {match->
        // Cut at word boundaries about fifty letters either side, so the match is read with its sentence.
        var a=(match.first-50).coerceAtLeast(0);var b=(match.last+1+70).coerceAtMost(body.length)
        while(a>0 && !body[a-1].isWhitespace() && match.first-a<70)a--
        while(b<body.length && !body[b].isWhitespace() && b-match.last<90)b++
        val head=if(a>0)"… " else "";val tail=if(b<body.length)" …" else ""
        val cut=body.substring(a,b).replace('\n',' ')
        HelpHit(place,head+cut+tail,head.length+match.first-a,head.length+match.last+1-a)
    }
    val found=ArrayList<HelpHit>()
    tree.forEachIndexed {c,category->category.sections.forEachIndexed {s,section->
        hit(HelpPlace(c,s,null),section.title)?.let {found+=it}
        section.icons.forEach {icon->hit(HelpPlace(c,s,icon.label),icon.label+" — "+icon.about)?.let {found+=it}}
        found+=fragments(HelpPlace(c,s,null),section.body)
    }}
    val first=tree.indexOfFirst {it.id==prefer}
    return found.sortedBy {if(it.place.category==first)0 else 1}.take(80)
}
