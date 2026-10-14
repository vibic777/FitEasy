package com.fiteasy.shared

const val LEGAL_NOTICE_VERSION=1
data class LegalNotice(val title:String,val body:String,val acknowledge:String,val saveError:String)
fun needsLegalNotice(local:LocalData)=local.legalNoticeVersion!=LEGAL_NOTICE_VERSION
fun acknowledgeLegalNotice(snapshot:Snapshot)=snapshot.copy(local=snapshot.local.copy(legalNoticeVersion=LEGAL_NOTICE_VERSION))
fun legalNotice(language:String)=legalNotices[language] ?: legalNotices.getValue("en")

val legalNotices=mapOf(
    "ru" to LegalNotice("Назначение приложения и ответственность", """
        FitEasy — личный дневник питания, воды и активности для удобства учёта. Приложение не предоставляет медицинские консультации, диагностику, лечение или индивидуальные назначения упражнений и не заменяет помощь специалиста.

        Калорийность, расход энергии, цели, показатели, справочники и иллюстрации упражнений носят информационный характер. Значения приблизительны, могут содержать ошибки и не учитывают все особенности вашего здоровья. Безопасность конкретной нагрузки и достижение результата не гарантируются.

        Решения о питании, питьевом режиме и нагрузках вы принимаете самостоятельно с учётом консультации врача, квалифицированного специалиста по питанию и фитнес-тренера. Не меняйте назначенное лечение и не откладывайте медицинскую помощь из-за информации в приложении. При боли или ухудшении самочувствия прекратите занятие и обратитесь за медицинской помощью.

        В пределах, допускаемых применимым законодательством, разработчики и участники проекта не отвечают за ваши самостоятельные решения о здоровье и последствия использования справочной информации вопреки рекомендациям специалистов. Это уведомление не исключает ответственность, которую нельзя исключить по закону, и не ограничивает ваши обязательные законные права.

        Кнопка «Я ознакомился» подтверждает прочтение уведомления. Текст всегда доступен в справке.
    """.trimIndent(),"Я ознакомился","Не удалось сохранить ознакомление. Попробуйте ещё раз."),
    "en" to LegalNotice("App purpose and responsibility", """
        FitEasy is a personal food, water and activity diary for convenient record keeping. It does not provide medical advice, diagnosis, treatment or individual exercise prescriptions and does not replace professional care.

        Calories, energy expenditure, goals, metrics, catalogues and exercise illustrations are informational. Values are estimates, may contain errors and do not account for every aspect of your health. The safety of a particular activity and any outcome are not guaranteed.

        You make your own decisions about food, hydration and activity, informed by consultation with a physician, a qualified nutrition professional and a fitness trainer. Do not change prescribed treatment or delay medical care because of information in the app. Stop exercising and seek medical help if you experience pain or feel unwell.

        To the extent permitted by applicable law, developers and project contributors are not responsible for your independent health decisions or consequences of using reference information contrary to professional advice. This notice does not exclude liability that cannot legally be excluded or limit your mandatory legal rights.

        “I have read this” acknowledges reading this notice. The text is always available in Help.
    """.trimIndent(),"I have read this","Could not save your acknowledgement. Please try again."),
    "be" to LegalNotice("Прызначэнне праграмы і адказнасць", """
        FitEasy — асабісты дзённік харчавання, вады і актыўнасці для зручнага ўліку. Праграма не дае медыцынскіх кансультацый, дыягностыкі, лячэння або індывідуальных прызначэнняў практыкаванняў і не замяняе дапамогу спецыяліста.

        Каларыйнасць, расход энергіі, мэты, паказчыкі, даведнікі і ілюстрацыі практыкаванняў маюць інфармацыйны характар. Значэнні прыблізныя, могуць утрымліваць памылкі і не ўлічваюць усіх асаблівасцей вашага здароўя. Бяспека канкрэтнай нагрузкі і дасягненне выніку не гарантуюцца.

        Рашэнні пра харчаванне, пітны рэжым і нагрузкі вы прымаеце самастойна з улікам кансультацыі ўрача, кваліфікаванага спецыяліста па харчаванні і фітнес-трэнера. Не змяняйце прызначанае лячэнне і не адкладайце медыцынскую дапамогу праз інфармацыю ў праграме. Пры болю або пагаршэнні самаадчування спыніце занятак і звярніцеся па медыцынскую дапамогу.

        У межах, дазволеных прымяняльным заканадаўствам, распрацоўшчыкі і ўдзельнікі праекта не адказваюць за вашы самастойныя рашэнні пра здароўе і наступствы выкарыстання даведачнай інфармацыі насуперак парадам спецыялістаў. Гэта паведамленне не выключае адказнасць, якую нельга выключыць паводле закона, і не абмяжоўвае вашы абавязковыя законныя правы.

        Кнопка «Я азнаёміўся» пацвярджае прачытанне паведамлення. Тэкст заўсёды даступны ў даведцы.
    """.trimIndent(),"Я азнаёміўся","Не ўдалося захаваць пацвярджэнне. Паспрабуйце яшчэ раз."),
    "fr" to LegalNotice("Objet de l’application et responsabilité", """
        FitEasy est un journal personnel d’alimentation, d’eau et d’activité destiné à faciliter le suivi. Il ne fournit aucun conseil médical, diagnostic, traitement ni prescription individuelle d’exercice et ne remplace pas les soins d’un professionnel.

        Les calories, dépenses énergétiques, objectifs, indicateurs, catalogues et illustrations d’exercices sont informatifs. Les valeurs sont des estimations, peuvent comporter des erreurs et ne tiennent pas compte de toutes les particularités de votre santé. La sécurité d’une activité donnée et les résultats ne sont pas garantis.

        Vous prenez vos décisions concernant l’alimentation, l’hydratation et l’activité en tenant compte des conseils d’un médecin, d’un professionnel qualifié de la nutrition et d’un entraîneur sportif. Ne modifiez pas un traitement prescrit et ne retardez pas des soins à cause des informations de l’application. En cas de douleur ou de malaise, arrêtez l’exercice et consultez un professionnel de santé.

        Dans les limites permises par la loi applicable, les développeurs et contributeurs ne sont pas responsables de vos décisions personnelles de santé ni des conséquences d’une utilisation des informations contraire aux conseils professionnels. Cet avis n’exclut aucune responsabilité qui ne peut légalement être exclue et ne limite pas vos droits impératifs.

        « J’ai lu cet avis » confirme la lecture. Le texte reste disponible dans l’aide.
    """.trimIndent(),"J’ai lu cet avis","Impossible d’enregistrer la confirmation. Réessayez."),
    "de" to LegalNotice("Zweck der App und Verantwortung", """
        FitEasy ist ein persönliches Tagebuch für Ernährung, Wasser und Aktivität zur einfachen Dokumentation. Die App bietet keine medizinische Beratung, Diagnose, Behandlung oder individuelle Trainingsverordnung und ersetzt keine professionelle Betreuung.

        Kalorien, Energieverbrauch, Ziele, Kennzahlen, Kataloge und Übungsabbildungen dienen der Information. Werte sind Schätzungen, können Fehler enthalten und berücksichtigen nicht alle Besonderheiten Ihrer Gesundheit. Die Sicherheit einer bestimmten Belastung und Ergebnisse werden nicht garantiert.

        Sie entscheiden selbst über Ernährung, Flüssigkeitszufuhr und Bewegung unter Berücksichtigung der Beratung durch einen Arzt, eine qualifizierte Ernährungsfachkraft und einen Fitnesstrainer. Ändern Sie keine verordnete Behandlung und verzögern Sie keine medizinische Hilfe wegen Angaben in der App. Beenden Sie bei Schmerzen oder Unwohlsein die Übung und suchen Sie medizinische Hilfe.

        Soweit das anwendbare Recht es zulässt, haften Entwickler und Projektmitwirkende nicht für Ihre eigenständigen Gesundheitsentscheidungen oder Folgen der Nutzung von Informationen entgegen fachlichem Rat. Gesetzlich zwingende Haftung wird nicht ausgeschlossen; Ihre zwingenden gesetzlichen Rechte bleiben unberührt.

        „Ich habe den Hinweis gelesen“ bestätigt die Kenntnisnahme. Der Text ist jederzeit in der Hilfe verfügbar.
    """.trimIndent(),"Ich habe den Hinweis gelesen","Bestätigung konnte nicht gespeichert werden. Bitte erneut versuchen."),
    "es" to LegalNotice("Finalidad de la aplicación y responsabilidad", """
        FitEasy es un diario personal de alimentación, agua y actividad para facilitar el registro. No ofrece asesoramiento médico, diagnóstico, tratamiento ni prescripciones individuales de ejercicio y no sustituye la atención profesional.

        Las calorías, el gasto energético, los objetivos, indicadores, catálogos e ilustraciones de ejercicios son informativos. Los valores son estimaciones, pueden contener errores y no contemplan todas las particularidades de su salud. No se garantiza la seguridad de una actividad concreta ni ningún resultado.

        Usted decide sobre alimentación, hidratación y actividad teniendo en cuenta la consulta con un médico, un profesional cualificado de nutrición y un entrenador. No cambie tratamientos prescritos ni retrase la atención médica por información de la aplicación. Si siente dolor o malestar, detenga el ejercicio y solicite ayuda médica.

        En la medida permitida por la legislación aplicable, los desarrolladores y colaboradores no responden de sus decisiones independientes de salud ni de las consecuencias del uso de información contrario al consejo profesional. Este aviso no excluye responsabilidades que legalmente no puedan excluirse ni limita sus derechos legales imperativos.

        «He leído este aviso» confirma su lectura. El texto está siempre disponible en la ayuda.
    """.trimIndent(),"He leído este aviso","No se pudo guardar la confirmación. Inténtelo de nuevo."),
    "it" to LegalNotice("Scopo dell’app e responsabilità", """
        FitEasy è un diario personale di alimentazione, acqua e attività per facilitare la registrazione. Non fornisce consulenza medica, diagnosi, trattamenti o prescrizioni individuali di esercizi e non sostituisce l’assistenza professionale.

        Calorie, consumo energetico, obiettivi, indicatori, cataloghi e illustrazioni degli esercizi sono informativi. I valori sono stime, possono contenere errori e non considerano tutte le particolarità della salute. Non sono garantiti la sicurezza di una specifica attività o i risultati.

        Le decisioni su alimentazione, idratazione e attività spettano a lei, tenendo conto del parere di un medico, di un professionista qualificato della nutrizione e di un istruttore. Non modifichi trattamenti prescritti né ritardi cure mediche a causa delle informazioni nell’app. In caso di dolore o malessere interrompa l’esercizio e cerchi assistenza medica.

        Nei limiti consentiti dalla legge applicabile, sviluppatori e collaboratori non rispondono delle sue decisioni autonome sulla salute o delle conseguenze dell’uso di informazioni contrario al parere professionale. Questo avviso non esclude responsabilità che non possono essere escluse per legge e non limita i suoi diritti inderogabili.

        «Ho letto questo avviso» conferma la lettura. Il testo è sempre disponibile nella guida.
    """.trimIndent(),"Ho letto questo avviso","Impossibile salvare la conferma. Riprovi."),
    "zh" to LegalNotice("应用用途与责任说明", """
        FitEasy 是用于方便记录饮食、饮水和活动的个人日记。本应用不提供医疗建议、诊断、治疗或个性化运动处方，也不能替代专业照护。

        热量、能量消耗、目标、指标、资料库及运动示意图仅供参考。数值为估算值，可能有误，无法涵盖您健康状况的所有特点。不保证某项活动的安全性或任何结果。

        您应参考医生、合格营养专业人员和健身教练的意见，自行决定饮食、饮水和运动安排。请勿因应用中的信息更改医嘱或延误就医。出现疼痛或身体不适时，请停止运动并寻求医疗帮助。

        在适用法律允许的范围内，开发者和项目贡献者不对您的自主健康决定或违背专业建议使用参考信息的后果承担责任。本说明不排除法律规定不得排除的责任，也不限制您的法定强制性权利。

        点击“我已阅读”表示您已阅读本说明。可随时在帮助中查看全文。
    """.trimIndent(),"我已阅读","无法保存阅读确认，请重试。"),
    "ja" to LegalNotice("アプリの目的と責任について", """
        FitEasy は食事、水分、活動を手軽に記録するための個人用日記です。医療上の助言、診断、治療、個別の運動処方を提供するものではなく、専門家によるケアに代わるものではありません。

        カロリー、エネルギー消費、目標、指標、資料集、運動の図は参考情報です。数値は推定であり、誤りを含む場合があり、健康状態のすべての個別事情を考慮するものではありません。特定の活動の安全性や結果を保証しません。

        食事、水分補給、運動については、医師、資格を持つ栄養の専門家、フィットネストレーナーへの相談を踏まえて、ご自身で判断してください。アプリの情報を理由に処方された治療を変更したり、受診を遅らせたりしないでください。痛みや体調不良がある場合は運動を中止し、医療機関に相談してください。

        適用法が認める範囲で、開発者とプロジェクトの貢献者は、ご自身の健康に関する判断や、専門家の助言に反して参考情報を使用した結果について責任を負いません。本通知は、法律上免除できない責任を免除せず、法定の強行的な権利を制限しません。

        「確認しました」は本通知を読んだことの確認です。全文はいつでもヘルプで確認できます。
    """.trimIndent(),"確認しました","確認を保存できませんでした。再試行してください。"),
    "ko" to LegalNotice("앱의 목적과 책임 안내", """
        FitEasy는 음식, 수분 섭취 및 활동을 편리하게 기록하는 개인 일지입니다. 의학적 조언, 진단, 치료 또는 개인별 운동 처방을 제공하지 않으며 전문가의 진료와 지도를 대신하지 않습니다.

        칼로리, 에너지 소비량, 목표, 지표, 자료 목록 및 운동 그림은 참고 정보입니다. 값은 추정치이며 오류가 있을 수 있고 건강 상태의 모든 개인적 특성을 반영하지 않습니다. 특정 활동의 안전성이나 결과를 보장하지 않습니다.

        음식, 수분 섭취 및 운동에 대한 결정은 의사, 자격을 갖춘 영양 전문가 및 피트니스 트레이너와의 상담을 참고하여 스스로 내리세요. 앱의 정보 때문에 처방된 치료를 변경하거나 진료를 미루지 마세요. 통증이나 몸의 이상을 느끼면 운동을 중단하고 의료 도움을 받으세요.

        관련 법률이 허용하는 범위에서 개발자와 프로젝트 기여자는 사용자의 독립적인 건강 결정이나 전문가의 조언에 반하여 참고 정보를 사용한 결과에 책임을 지지 않습니다. 이 안내는 법적으로 배제할 수 없는 책임을 배제하거나 강행적인 법적 권리를 제한하지 않습니다.

        “읽었습니다”는 이 안내를 읽었음을 확인합니다. 전체 내용은 도움말에서 언제든지 확인할 수 있습니다.
    """.trimIndent(),"읽었습니다","확인을 저장하지 못했습니다. 다시 시도하세요.")
)
