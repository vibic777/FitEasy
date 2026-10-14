package com.fiteasy.shared

internal val appearanceKeys="Спокойная|Цветочная|Воздушная|Геометрическая|Разноцветная|Accent / RGB|Animations|Section backgrounds|Фоны разделов|Все разделы|Цвет фона|Применить цвет|Системный фон|Применить прозрачность|Прозрачность фона|Выбрать фон|Удалить фон|Не удалось сохранить оформление|Не удалось загрузить фон|Retry|Розовая|Фиолетовая".split('|')
internal val appearanceRows=mapOf(
"ru" to "Спокойная|Цветочная|Воздушная|Геометрическая|Разноцветная|Акцент / RGB|Анимации|Фоны разделов|Фоны разделов|Все разделы|Цвет фона|Применить цвет|Системный фон|Применить прозрачность|Прозрачность фона|Выбрать фон|Удалить фон|Не удалось сохранить оформление|Не удалось загрузить фон|Повторить|Розовая|Фиолетовая",
"en" to "Calm|Blossom|Air|Geometry|Rainbow|Accent / RGB|Animations|Section backgrounds|Section backgrounds|All sections|Background color|Apply color|System background|Apply opacity|Background opacity|Choose background|Remove background|Could not save appearance|Could not load background|Retry|Pink|Violet",
"be" to "Спакойная|Кветкавая|Паветраная|Геаметрычная|Рознакаляровая|Акцэнт / RGB|Анімацыі|Фоны раздзелаў|Фоны раздзелаў|Усе раздзелы|Колер фону|Ужыць колер|Сістэмны фон|Ужыць празрыстасць|Празрыстасць фону|Выбраць фон|Выдаліць фон|Не ўдалося захаваць афармленне|Не ўдалося загрузіць фон|Паўтарыць|Ружовая|Фіялетавая",
"fr" to "Calme|Florale|Aérienne|Géométrique|Multicolore|Accent / RGB|Animations|Fonds des sections|Fonds des sections|Toutes les sections|Couleur du fond|Appliquer la couleur|Fond du système|Appliquer l’opacité|Opacité du fond|Choisir un fond|Supprimer le fond|Impossible d’enregistrer l’apparence|Impossible de charger le fond|Réessayer|Rose|Violette",
"de" to "Ruhig|Blüten|Luftig|Geometrisch|Bunt|Akzent / RGB|Animationen|Bereichshintergründe|Bereichshintergründe|Alle Bereiche|Hintergrundfarbe|Farbe anwenden|Systemhintergrund|Deckkraft anwenden|Hintergrunddeckkraft|Hintergrund wählen|Hintergrund entfernen|Darstellung konnte nicht gespeichert werden|Hintergrund konnte nicht geladen werden|Erneut versuchen|Rosa|Violett",
"es" to "Serena|Floral|Aérea|Geométrica|Multicolor|Acento / RGB|Animaciones|Fondos por sección|Fondos por sección|Todas las secciones|Color de fondo|Aplicar color|Fondo del sistema|Aplicar opacidad|Opacidad del fondo|Elegir fondo|Eliminar fondo|No se pudo guardar la apariencia|No se pudo cargar el fondo|Reintentar|Rosa|Violeta",
"it" to "Calma|Floreale|Aria|Geometrica|Multicolore|Accento / RGB|Animazioni|Sfondi delle sezioni|Sfondi delle sezioni|Tutte le sezioni|Colore di sfondo|Applica colore|Sfondo di sistema|Applica opacità|Opacità dello sfondo|Scegli sfondo|Rimuovi sfondo|Impossibile salvare l’aspetto|Impossibile caricare lo sfondo|Riprova|Rosa|Viola",
"zh" to "宁静|花朵|轻盈|几何|缤纷|强调色 / RGB|动画|分区背景|分区背景|所有分区|背景颜色|应用颜色|系统背景|应用不透明度|背景不透明度|选择背景|移除背景|无法保存外观|无法加载背景|重试|粉色|紫色",
"ja" to "落ち着き|花|軽やか|幾何学|カラフル|アクセント / RGB|アニメーション|セクションの背景|セクションの背景|すべてのセクション|背景色|色を適用|システムの背景|不透明度を適用|背景の不透明度|背景を選択|背景を削除|表示を保存できませんでした|背景を読み込めませんでした|再試行|ピンク|バイオレット",
"ko" to "차분함|꽃|가벼움|기하학|다채로움|강조색 / RGB|애니메이션|섹션 배경|섹션 배경|모든 섹션|배경색|색 적용|시스템 배경|불투명도 적용|배경 불투명도|배경 선택|배경 제거|모양을 저장할 수 없습니다|배경을 불러올 수 없습니다|다시 시도|핑크|바이올렛")
private val appearanceDictionary=appearanceRows.mapValues {(_,row)->appearanceKeys.zip(row.split('|')).toMap()}
private val refreshErrors=mapOf("ru" to "Не удалось обновить данные","en" to "Data could not be refreshed","be" to "Не ўдалося абнавіць даныя","fr" to "Impossible d’actualiser les données","de" to "Daten konnten nicht aktualisiert werden","es" to "No se pudieron actualizar los datos","it" to "Impossibile aggiornare i dati","zh" to "无法刷新数据","ja" to "データを更新できませんでした","ko" to "데이터를 새로 고칠 수 없습니다")
private val renderingRows=mapOf(
"ru" to "Отрисовка Windows|Совместимость|Перезапустите FitEasy для применения",
"en" to "Window rendering|Compatibility|Restart FitEasy to apply",
"be" to "Адмалёўка Windows|Сумяшчальнасць|Перазапусціце FitEasy для ўжывання",
"fr" to "Rendu Windows|Compatibilité|Redémarrez FitEasy pour appliquer",
"de" to "Windows-Darstellung|Kompatibilität|FitEasy zum Anwenden neu starten",
"es" to "Renderizado de Windows|Compatibilidad|Reinicie FitEasy para aplicar",
"it" to "Rendering Windows|Compatibilità|Riavvia FitEasy per applicare",
"zh" to "Windows渲染|兼容模式|重启FitEasy以应用",
"ja" to "Windowsの描画|互換性|適用するにはFitEasyを再起動",
"ko" to "Windows 렌더링|호환성|적용하려면 FitEasy를 다시 시작하세요")
private val renderingDictionary=renderingRows.mapValues {(_,row)->listOf("Window rendering","Compatibility","Restart FitEasy to apply").zip(row.split('|')).toMap()}
fun appearanceTranslation(text:String,language:String)=if(text=="Data could not be refreshed")refreshErrors[language]else renderingDictionary[language]?.get(text) ?: appearanceDictionary[language]?.get(text)
fun appearanceHelp(language:String)=HelpArticle(translate("Оформление",language),listOf("Спокойная","Цветочная","Воздушная","Геометрическая","Разноцветная","Accent / RGB","Animations","Section backgrounds","Все разделы","Цвет фона","Применить цвет","Выбрать фон","Удалить фон","Применить прозрачность").joinToString(" · "){translate(it,language)}+"\nRGB: R, G, B = 0–255; RRGGBB. 1 MiB / image; 8 MiB total.")
