import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.util.Base64
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.compose")
}
val releaseInfo=Properties().apply {rootProject.file("version.properties").inputStream().use {load(it)}}
val releaseVersion=releaseInfo.getProperty("version")
kotlin {
    jvm()
    sourceSets {
        jvmTest { dependencies {implementation(kotlin("test"))} }
        jvmMain {
            kotlin.srcDir("../shared/src")
            kotlin.srcDir(rootProject.layout.buildDirectory.dir("generated/buildInfo/kotlin"))
            resources.srcDir("../shared/resources")
            dependencies {
                implementation("org.jmdns:jmdns:3.6.3")
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose.material3:material3:1.9.0")
                implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
                implementation(libs.kotlinx.serialization.json)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
                implementation("io.github.willena:sqlite-jdbc:3.53.4.0")
                implementation("net.java.dev.jna:jna-platform:5.19.1")
                implementation("org.jetbrains.runtime:jbr-api:1.10.1")
                // MP3 and OGG Vorbis melodies of reminders (D123): the MP3 decoder plugs into javax.sound, OGG is read with JOrbis (OggVorbis.kt). LGPL, kept as their own jars.
                implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") {exclude(group="junit",module="junit")}
                implementation("com.googlecode.soundlibs:jorbis:0.0.17.4")
                // QR code of the TOTP key (D124).
                implementation(libs.zxing.core)
            }
        }
    }
}
compose.desktop { application {
    // Plain Java first: it reports program files lost after an update instead of ending silently (D122).
    mainClass = "com.fiteasy.desktop.Launcher"
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    nativeDistributions {
        targetFormats(TargetFormat.Msi, TargetFormat.Exe)
        packageName = "FitEasy"
        packageVersion = releaseVersion
        vendor = "vibic777"
        modules("java.sql", "jdk.unsupported", "jdk.jfr")
        windows { menuGroup = "FitEasy"; upgradeUuid = "dd1d50b7-a496-4d69-8745-9483d419017a";iconFile.set(project.file("src/jvmMain/resources/icons/fiteasy.ico")) }
    }
} }

val prepareManagedInstaller = tasks.register("prepareManagedInstaller") {
    dependsOn(":unzipWix")
    val resources = layout.buildDirectory.dir("managed-installer-res")
    val wix = layout.buildDirectory.dir("managed-wix")
    inputs.dir("installer")
    inputs.property("packagingRuntime",System.getProperty("java.home"))
    outputs.dirs(resources,wix)
    doLast {
        val resourceDir=resources.get().asFile.apply {mkdirs()}
        val template=ZipFile(File(System.getProperty("java.home"),"jmods/jdk.jpackage.jmod")).use {zip->
            zip.getInputStream(zip.getEntry("classes/jdk/jpackage/internal/resources/main.wxs")).bufferedReader().use {it.readText()}
        }
        val script=project.file("installer/close-running.ps1").readText()
        val encoded=Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
        val hooks=project.file("installer/hooks.xml").readText().replace("@CLOSE_SCRIPT@",encoded)
        // Add actions to the existing UI sequence; WiX permits only one sequence element per Product.
        val actions=hooks.substringBefore("<InstallUISequence>")
        val sequence=hooks.substringAfter("<InstallUISequence>").substringBefore("</InstallUISequence>")
        File(resourceDir,"main.wxs").writeText(template.replace("    <InstallUISequence>",actions+"\n    <InstallUISequence>\n"+sequence))
        File(resourceDir,"overrides.wxi").writeText("<?xml version=\"1.0\"?><Include xmlns=\"http://schemas.microsoft.com/wix/2006/wi\" />")
        val archive=File(gradle.gradleUserHomeDir,"compose-jb/wix311.zip")
        ZipFile(archive).use {zip->zip.entries().asSequence().filter {!it.isDirectory}.forEach {entry->
            val target=File(wix.get().asFile,entry.name).canonicalFile
            check(target.toPath().startsWith(wix.get().asFile.canonicalFile.toPath()))
            target.parentFile.mkdirs();zip.getInputStream(entry).use {input->target.outputStream().use {input.copyTo(it)}}
        }}
    }
}
listOf("BaseMsi" to "msi").forEach {(suffix,type)->
    tasks.register<Exec>("packageManaged$suffix") {
        dependsOn("createDistributable",prepareManagedInstaller)
        val output=layout.buildDirectory.dir("managed-installer/base")
        doFirst {
            output.get().asFile.mkdirs()
            File(output.get().asFile,"FitEasy-$releaseVersion.$type").takeIf {it.exists()}?.let {check(it.delete())}
            val wixDir=layout.buildDirectory.dir("managed-wix").get().asFile
            val candle=wixDir.walkTopDown().first {it.name=="candle.exe"}.parentFile
            environment("PATH",candle.absolutePath+File.pathSeparator+System.getenv("PATH"))
            commandLine(File(System.getProperty("java.home"),"bin/jpackage.exe").absolutePath,
                "--type",type,"--app-image",layout.buildDirectory.dir("compose/binaries/main/app/FitEasy").get().asFile.absolutePath,
                "--name","FitEasy","--app-version",releaseVersion,"--vendor","vibic777",
                "--resource-dir",layout.buildDirectory.dir("managed-installer-res").get().asFile.absolutePath,
                "--win-menu","--win-menu-group","FitEasy","--win-dir-chooser",
                "--win-upgrade-uuid","dd1d50b7-a496-4d69-8745-9483d419017a",
                "--icon",project.file("src/jvmMain/resources/icons/fiteasy.ico").absolutePath,"--dest",output.get().asFile.absolutePath)
        }
    }
}
val managedMsi=tasks.register<Exec>("packageManagedMsi") {
    dependsOn("packageManagedBaseMsi")
    inputs.dir("installer")
    doFirst {
        val output=layout.buildDirectory.dir("managed-installer").get().asFile
        File(output,"msi").mkdirs();File(output,"exe").mkdirs()
        commandLine("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",project.file("installer/localize.ps1").absolutePath,
            "-BaseMsi",File(output,"base/FitEasy-$releaseVersion.msi").absolutePath,
            "-OutputMsi",File(output,"msi/FitEasy-$releaseVersion.msi").absolutePath,
            "-OutputExe",File(output,"exe/FitEasy-$releaseVersion.exe").absolutePath,
            "-WixDir",layout.buildDirectory.dir("managed-wix").get().asFile.absolutePath,
            "-JdkHome",System.getProperty("java.home"),"-SourceDir",project.file("installer").absolutePath,
            "-WorkDir",layout.buildDirectory.dir("installer-languages").get().asFile.absolutePath,
            "-Version",releaseVersion,"-BuildNumber",releaseInfo.getProperty("build"))
    }
}
tasks.register("packageManagedExe") {dependsOn(managedMsi)}
tasks.register<Exec>("verifyManagedInstallerLanguages") {
    dependsOn(managedMsi)
    doFirst {commandLine("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",project.file("installer/verify.ps1").absolutePath,
        "-Msi",layout.buildDirectory.file("managed-installer/msi/FitEasy-$releaseVersion.msi").get().asFile.absolutePath,
        "-Exe",layout.buildDirectory.file("managed-installer/exe/FitEasy-$releaseVersion.exe").get().asFile.absolutePath,
        "-WixDir",layout.buildDirectory.dir("managed-wix").get().asFile.absolutePath,
        "-WorkDir",layout.buildDirectory.dir("installer-languages").get().asFile.absolutePath)}
}
tasks.matching {it.name.startsWith("compile") && it.name.contains("Kotlin")}.configureEach {dependsOn(":generateBuildInfo")}

tasks.register<Zip>("packagePortable") {
    group="distribution"
    description="Windows portable archive with bundled runtime and separate data folder"
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app/FitEasy")) {into("FitEasy")}
    from("portable") {into("FitEasy")}
    from(rootProject.file("NOTICE.md")) {into("FitEasy")}
    from(rootProject.file("docs/licenses/Notio-MIT.txt")) {into("FitEasy/licenses")}
    from(rootProject.file("docs/licenses/WiX-MS-RL.txt")) {into("FitEasy/licenses")}
    archiveFileName.set("FitEasy-$releaseVersion-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("portable"))
}

tasks.register("packageDelivery") {
    group="distribution"
    description="All five required Android and Windows delivery formats"
    dependsOn(":app:assembleDebug",":app:assembleRelease","packageManagedExe","packageManagedMsi","packagePortable")
}

// The header of the app is the title bar of the window only on JetBrains Runtime (D082). A package built with another
// Java works, but Windows draws its own title strip over the app: 0.4.36 and 0.4.37 went out that way. As in Notio, the
// packaging refuses any other Java (D129); tests and compilation run on any.
tasks.matching {it.name=="createDistributable"}.configureEach {
    doFirst {
        check(System.getProperty("java.vendor").orEmpty().contains("JetBrains")) {
            "Packaging needs JetBrains Runtime SDK 25 (the window title bar, D082/D129): set JAVA_HOME to jbrsdk-25…, e.g. %USERPROFILE%\\.jdks\\jbrsdk-25.0.4.1-windows-x64-b610.67. This Java is ${System.getProperty("java.vendor")} ${System.getProperty("java.version")}."
        }
    }
}
// The database driver ships native libraries for every system; a Windows package needs only the Windows ones (D092).
tasks.matching {it.name=="createDistributable"}.configureEach {
    doLast {
        val app=layout.buildDirectory.dir("compose/binaries/main/app/FitEasy/app").get().asFile
        app.listFiles().orEmpty().filter {it.name.startsWith("sqlite-jdbc-") && it.extension=="jar"}.forEach {jar->
            val slim=File(jar.parentFile,jar.name+".slim")
            ZipFile(jar).use {zip->ZipOutputStream(slim.outputStream().buffered()).use {out->
                zip.entries().asSequence().filter {entry->!entry.name.startsWith("org/sqlite/native/") || entry.name=="org/sqlite/native/" || entry.name.startsWith("org/sqlite/native/Windows/")}.forEach {entry->
                    out.putNextEntry(ZipEntry(entry.name));if(!entry.isDirectory)zip.getInputStream(entry).use {it.copyTo(out)};out.closeEntry()
                }
            }}
            check(jar.delete() && slim.renameTo(jar))
        }
    }
}

