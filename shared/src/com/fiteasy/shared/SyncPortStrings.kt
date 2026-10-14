package com.fiteasy.shared

/**
 * Texts of the port this device waits for a sync on (D132). Russian source text is the key; order: en, be, fr, de, es,
 * it, zh, ja, ko. {0} and {1} are port numbers.
 */
internal val syncPortRows:Map<String,Array<String>> = mapOf(
    "Порт этого устройства" to arrayOf("Port of this device","Порт гэтай прылады","Port de cet appareil","Port dieses Geräts","Puerto de este dispositivo","Porta di questo dispositivo","本设备的端口","このデバイスのポート","이 기기의 포트"),
    "Автоматически" to arrayOf("Automatically","Аўтаматычна","Automatiquement","Automatisch","Automáticamente","Automaticamente","自动","自動","자동"),
    "Вручную" to arrayOf("By hand","Уручную","Manuellement","Von Hand","A mano","A mano","手动","手動","직접 입력"),
    "Обычный порт {0}, а если он занят — любой свободный. Другое устройство находит порт само." to arrayOf(
        "The usual port {0}, or any free one when it is taken. The other device finds the port by itself.",
        "Звычайны порт {0}, а калі ён заняты — любы свабодны. Іншая прылада знаходзіць порт сама.",
        "Le port habituel {0} ou, s'il est occupé, n'importe quel port libre. L'autre appareil trouve le port tout seul.",
        "Der übliche Port {0} oder, wenn er belegt ist, ein beliebiger freier. Das andere Gerät findet den Port selbst.",
        "El puerto habitual {0} o, si está ocupado, cualquiera libre. El otro dispositivo encuentra el puerto por sí mismo.",
        "La porta abituale {0} oppure, se è occupata, una qualsiasi libera. L'altro dispositivo trova la porta da solo.",
        "使用常用端口 {0}，被占用时改用任意空闲端口。另一台设备会自行找到端口。",
        "通常のポート {0} を使い、使用中なら空いているポートを使います。相手のデバイスはポートを自動で見つけます。",
        "기본 포트 {0}을(를) 쓰고, 사용 중이면 비어 있는 포트를 씁니다. 다른 기기가 포트를 스스로 찾습니다."),
    "Свой порт нужен, когда в сети или в брандмауэре открыт только определённый порт. Если другое устройство подключается по адресу, введённому вручную, укажите на нём этот же порт." to arrayOf(
        "A port of your own is needed when the network or the firewall lets only a certain port through. If the other device connects by an address typed by hand, enter the same port there.",
        "Свой порт патрэбны, калі ў сетцы або ў брандмаўэры адкрыты толькі пэўны порт. Калі іншая прылада падключаецца па адрасе, уведзеным уручную, пакажыце на ёй гэты ж порт.",
        "Un port choisi est nécessaire quand le réseau ou le pare-feu ne laisse passer qu'un port précis. Si l'autre appareil se connecte par une adresse saisie à la main, indiquez-y le même port.",
        "Ein eigener Port ist nötig, wenn im Netz oder in der Firewall nur ein bestimmter Port offen ist. Verbindet sich das andere Gerät über eine von Hand eingegebene Adresse, geben Sie dort denselben Port an.",
        "Un puerto propio hace falta cuando la red o el firewall solo dejan pasar un puerto concreto. Si el otro dispositivo se conecta por una dirección escrita a mano, indique allí el mismo puerto.",
        "Una porta propria serve quando la rete o il firewall lasciano passare solo una certa porta. Se l'altro dispositivo si collega con un indirizzo digitato a mano, indicate lì la stessa porta.",
        "当网络或防火墙只开放特定端口时，需要自行设置端口。如果另一台设备通过手动输入的地址连接，请在那台设备上填写同一端口。",
        "ネットワークやファイアウォールで特定のポートだけが開いている場合は、ポートを自分で指定します。相手のデバイスが手入力のアドレスで接続する場合は、そちらにも同じポートを入力してください。",
        "네트워크나 방화벽에서 특정 포트만 열려 있을 때 포트를 직접 정합니다. 다른 기기가 직접 입력한 주소로 연결한다면 그 기기에도 같은 포트를 입력하세요."),
    "Порт {0} свободен." to arrayOf("Port {0} is free.","Порт {0} свабодны.","Le port {0} est libre.","Port {0} ist frei.","El puerto {0} está libre.","La porta {0} è libera.","端口 {0} 空闲。","ポート {0} は空いています。","포트 {0}은(는) 비어 있습니다."),
    "Порт {0} занят другой программой." to arrayOf("Port {0} is taken by another program.","Порт {0} заняты іншай праграмай.","Le port {0} est occupé par un autre programme.","Port {0} ist von einem anderen Programm belegt.","El puerto {0} está ocupado por otro programa.","La porta {0} è occupata da un altro programma.","端口 {0} 已被其他程序占用。","ポート {0} は別のプログラムが使用中です。","포트 {0}은(는) 다른 프로그램이 사용 중입니다."),
    "Порт {0} сейчас использует это приложение." to arrayOf("Port {0} is in use by this app now.","Порт {0} зараз выкарыстоўвае гэта праграма.","Le port {0} est utilisé en ce moment par cette application.","Port {0} wird gerade von dieser App verwendet.","El puerto {0} lo usa ahora esta aplicación.","La porta {0} è usata ora da questa app.","端口 {0} 目前由本应用使用。","ポート {0} は現在このアプリが使用しています。","포트 {0}은(는) 지금 이 앱이 사용하고 있습니다."),
    "Проверяется, свободен ли порт {0}…" to arrayOf("Checking whether port {0} is free…","Правяраецца, ці свабодны порт {0}…","Vérification : le port {0} est-il libre…","Es wird geprüft, ob Port {0} frei ist…","Comprobando si el puerto {0} está libre…","Verifica se la porta {0} è libera…","正在检查端口 {0} 是否空闲…","ポート {0} が空いているか確認しています…","포트 {0}이(가) 비어 있는지 확인하는 중…"),
    "Введите порт от 1024 до 65535." to arrayOf("Enter a port from 1024 to 65535.","Увядзіце порт ад 1024 да 65535.","Saisissez un port de 1024 à 65535.","Geben Sie einen Port von 1024 bis 65535 ein.","Introduzca un puerto de 1024 a 65535.","Inserite una porta da 1024 a 65535.","请输入 1024 到 65535 之间的端口。","1024〜65535 のポートを入力してください。","1024~65535 사이의 포트를 입력하세요."),
    "Порт: {0}, выбран автоматически." to arrayOf("Port: {0}, chosen automatically.","Порт: {0}, выбраны аўтаматычна.","Port : {0}, choisi automatiquement.","Port: {0}, automatisch gewählt.","Puerto: {0}, elegido automáticamente.","Porta: {0}, scelta automaticamente.","端口：{0}，自动选择。","ポート: {0}（自動選択）","포트: {0}, 자동 선택."),
    "Порт: {0}, задан вручную." to arrayOf("Port: {0}, set by hand.","Порт: {0}, зададзены ўручную.","Port : {0}, défini manuellement.","Port: {0}, von Hand festgelegt.","Puerto: {0}, fijado a mano.","Porta: {0}, impostata a mano.","端口：{0}，手动设置。","ポート: {0}（手動設定）","포트: {0}, 직접 설정."),
    "Обычный порт {0} занят другой программой, взят свободный {1}." to arrayOf(
        "The usual port {0} is taken by another program; the free port {1} is used.",
        "Звычайны порт {0} заняты іншай праграмай, узяты свабодны {1}.",
        "Le port habituel {0} est occupé par un autre programme ; le port libre {1} est utilisé.",
        "Der übliche Port {0} ist von einem anderen Programm belegt; der freie Port {1} wird verwendet.",
        "El puerto habitual {0} está ocupado por otro programa; se usa el puerto libre {1}.",
        "La porta abituale {0} è occupata da un altro programma; viene usata la porta libera {1}.",
        "常用端口 {0} 已被其他程序占用，改用空闲端口 {1}。",
        "通常のポート {0} は別のプログラムが使用中のため、空いているポート {1} を使います。",
        "기본 포트 {0}은(는) 다른 프로그램이 사용 중이어서 비어 있는 포트 {1}을(를) 씁니다."),
    "Порт {0} занят другой программой: это устройство не принимает синхронизацию. Выберите другой порт или «Автоматически»." to arrayOf(
        "Port {0} is taken by another program: this device does not accept a sync. Choose another port or Automatically.",
        "Порт {0} заняты іншай праграмай: гэта прылада не прымае сінхранізацыю. Выберыце іншы порт або «Аўтаматычна».",
        "Le port {0} est occupé par un autre programme : cet appareil n'accepte pas la synchronisation. Choisissez un autre port ou « Automatiquement ».",
        "Port {0} ist von einem anderen Programm belegt: Dieses Gerät nimmt keine Synchronisierung an. Wählen Sie einen anderen Port oder „Automatisch“.",
        "El puerto {0} está ocupado por otro programa: este dispositivo no acepta la sincronización. Elija otro puerto o «Automáticamente».",
        "La porta {0} è occupata da un altro programma: questo dispositivo non accetta la sincronizzazione. Scegliete un'altra porta oppure «Automaticamente».",
        "端口 {0} 已被其他程序占用：本设备无法接收同步。请选择其他端口或“自动”。",
        "ポート {0} は別のプログラムが使用中のため、このデバイスは同期を受け付けません。別のポートか「自動」を選んでください。",
        "포트 {0}은(는) 다른 프로그램이 사용 중이어서 이 기기는 동기화를 받지 못합니다. 다른 포트나 ‘자동’을 선택하세요."),
    // The paragraph of the help (Synchronization → Connections).
    "Порт, на котором устройство ждёт синхронизацию, выбирается автоматически: обычный 47821, а если он занят — любой свободный, и другое устройство находит его само. В блоке «Это устройство» кнопка «Порт этого устройства» позволяет задать порт вручную (от 1024 до 65535) и сразу показывает, свободен он или занят другой программой. Если заданный вручную порт занят, устройство не принимает синхронизацию и пишет об этом." to arrayOf(
        "The port a device waits for a sync on is chosen automatically: the usual 47821, or any free one when it is taken, and the other device finds it by itself. In the This device block the Port of this device button lets you set the port by hand (from 1024 to 65535) and shows at once whether it is free or taken by another program. If the port set by hand is taken, the device does not accept a sync and says so.",
        "Порт, на якім прылада чакае сінхранізацыю, выбіраецца аўтаматычна: звычайны 47821, а калі ён заняты — любы свабодны, і іншая прылада знаходзіць яго сама. У блоку «Гэта прылада» кнопка «Порт гэтай прылады» дазваляе задаць порт уручную (ад 1024 да 65535) і адразу паказвае, свабодны ён ці заняты іншай праграмай. Калі зададзены ўручную порт заняты, прылада не прымае сінхранізацыю і піша пра гэта.",
        "Le port sur lequel l'appareil attend la synchronisation est choisi automatiquement : le 47821 habituel ou, s'il est occupé, n'importe quel port libre, et l'autre appareil le trouve tout seul. Dans le bloc « Cet appareil », le bouton « Port de cet appareil » permet de définir le port manuellement (de 1024 à 65535) et indique aussitôt s'il est libre ou occupé par un autre programme. Si le port défini manuellement est occupé, l'appareil n'accepte pas la synchronisation et le signale.",
        "Der Port, auf dem das Gerät auf die Synchronisierung wartet, wird automatisch gewählt: der übliche 47821 oder, wenn er belegt ist, ein beliebiger freier – das andere Gerät findet ihn selbst. Im Block „Dieses Gerät“ lässt sich mit „Port dieses Geräts“ der Port von Hand festlegen (1024 bis 65535); dabei wird sofort angezeigt, ob er frei oder von einem anderen Programm belegt ist. Ist der von Hand festgelegte Port belegt, nimmt das Gerät keine Synchronisierung an und meldet das.",
        "El puerto en el que el dispositivo espera la sincronización se elige automáticamente: el habitual 47821 o, si está ocupado, cualquiera libre, y el otro dispositivo lo encuentra por sí mismo. En el bloque «Este dispositivo», el botón «Puerto de este dispositivo» permite fijar el puerto a mano (de 1024 a 65535) y muestra enseguida si está libre u ocupado por otro programa. Si el puerto fijado a mano está ocupado, el dispositivo no acepta la sincronización y lo indica.",
        "La porta su cui il dispositivo attende la sincronizzazione è scelta automaticamente: la 47821 abituale oppure, se è occupata, una qualsiasi libera, e l'altro dispositivo la trova da solo. Nel blocco «Questo dispositivo» il pulsante «Porta di questo dispositivo» permette di impostare la porta a mano (da 1024 a 65535) e mostra subito se è libera o occupata da un altro programma. Se la porta impostata a mano è occupata, il dispositivo non accetta la sincronizzazione e lo segnala.",
        "设备等待同步所用的端口会自动选择：常用的 47821，被占用时改用任意空闲端口，另一台设备会自行找到它。在“本设备”区块中，“本设备的端口”按钮可手动设置端口（1024 到 65535），并立即显示该端口空闲还是已被其他程序占用。如果手动设置的端口被占用，设备将不接收同步并给出提示。",
        "デバイスが同期を待ち受けるポートは自動で選ばれます。通常は 47821、使用中なら空いているポートを使い、相手のデバイスが自動で見つけます。「このデバイス」のブロックにある「このデバイスのポート」ボタンでポートを手動で指定でき（1024〜65535）、空いているか別のプログラムが使用中かがすぐに表示されます。手動で指定したポートが使用中の場合、デバイスは同期を受け付けず、その旨を表示します。",
        "기기가 동기화를 기다리는 포트는 자동으로 선택됩니다. 기본은 47821이고 사용 중이면 비어 있는 포트를 쓰며, 다른 기기가 스스로 찾습니다. ‘이 기기’ 블록의 ‘이 기기의 포트’ 버튼으로 포트를 직접 정할 수 있고(1024~65535), 비어 있는지 다른 프로그램이 사용 중인지 바로 보여 줍니다. 직접 정한 포트가 사용 중이면 기기는 동기화를 받지 않고 그 사실을 알립니다."))
