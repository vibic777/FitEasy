package com.fiteasy.desktop;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * First class of the Windows program (D122, after Notio Windows D91). Plain Java with no Kotlin in it, so it still
 * starts when libraries are gone from the app folder: an update over a running program can leave the folder without
 * the files Windows had postponed deleting until the restart. It says so in the language of the system, writes a line
 * into the journal and ends with code 3 instead of ending silently with code 1.
 */
public final class Launcher {
    static final int MISSING_FILES = 3;
    private static final Map<String, String> MESSAGES = Map.of(
        "en", "FitEasy program files are missing or damaged. Install FitEasy again: your data stays in place.",
        "ru", "Файлы программы FitEasy отсутствуют или повреждены. Установите FitEasy заново: ваши данные останутся на месте.",
        "be", "Файлы праграмы FitEasy адсутнічаюць або пашкоджаны. Усталюйце FitEasy нанова: вашы даныя застануцца на месцы.",
        "fr", "Des fichiers du programme FitEasy sont absents ou endommagés. Réinstallez FitEasy : vos données restent en place.",
        "de", "Programmdateien von FitEasy fehlen oder sind beschädigt. Installieren Sie FitEasy erneut: Ihre Daten bleiben erhalten.",
        "es", "Faltan archivos del programa FitEasy o están dañados. Instale FitEasy de nuevo: sus datos se conservan.",
        "it", "I file del programma FitEasy mancano o sono danneggiati. Reinstallate FitEasy: i vostri dati restano al loro posto.",
        "zh", "FitEasy 程序文件缺失或已损坏。请重新安装 FitEasy：您的数据会保留。",
        "ja", "FitEasy のプログラムファイルが見つからないか、破損しています。FitEasy を再インストールしてください。データはそのまま残ります。",
        "ko", "FitEasy 프로그램 파일이 없거나 손상되었습니다. FitEasy를 다시 설치하세요. 데이터는 그대로 유지됩니다.");

    private Launcher() {}

    public static void main(String[] args) throws Throwable {
        List<String> missing = missing(System.getProperty("java.class.path", ""));
        if (!missing.isEmpty()) {
            journal(missing.size());
            try {
                javax.swing.JOptionPane.showMessageDialog(null, message(Locale.getDefault().getLanguage()) + "\n\n" + String.join("\n", missing.subList(0, Math.min(8, missing.size()))), "FitEasy", javax.swing.JOptionPane.ERROR_MESSAGE);
            } catch (Throwable ignored) {
                System.err.println(message(Locale.getDefault().getLanguage()));
            }
            System.exit(MISSING_FILES);
        }
        try {
            Class.forName("com.fiteasy.desktop.MainKt").getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }
    }

    /** Names of the class path files that are not on the disk. */
    static List<String> missing(String classPath) {
        List<String> result = new ArrayList<>();
        for (String entry : classPath.split(File.pathSeparator)) {
            if (entry.isBlank() || entry.endsWith("*")) continue;
            File file = new File(entry);
            if (!file.exists() || (file.isFile() && file.length() == 0)) result.add(file.getName());
        }
        return result;
    }

    static String message(String language) {
        return MESSAGES.getOrDefault(language, MESSAGES.get("en"));
    }

    /** The data folder is found the way Main.kt finds it; the line has the form of the app journal. */
    private static void journal(int count) {
        try {
            File data;
            String explicit = System.getProperty("fiteasy.home");
            File jar = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File root = jar.getParentFile() == null ? null : jar.getParentFile().getParentFile();
            if (explicit != null) data = new File(explicit);
            else if (root != null && new File(root, "portable.txt").isFile()) data = new File(root, "data");
            else data = new File((System.getenv("LOCALAPPDATA") != null ? System.getenv("LOCALAPPDATA") : System.getProperty("user.home")) + "/FitEasy");
            File logs = new File(data, "logs");
            if (!logs.isDirectory() && !logs.mkdirs()) return;
            Files.writeString(new File(logs, "app.log").toPath(), Instant.now() + " FATAL launcher.files.missing." + count + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
            // The message on the screen matters more than the line in the journal.
        }
    }
}
