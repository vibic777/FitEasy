package com.fiteasy.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.fiteasy.shared.*
import com.fiteasy.shared.lock.*
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class SettingsPagesTest {
    private fun <T> withPlatform(block:(DesktopPlatform)->T):T {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-pages-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform()
        try {return block(platform)}finally {platform.close();if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun fourWaysToSignInAndOldPatternSettingsStillWork() {
        withPlatform {platform->
            val lock=DeviceLock(platform)
            lock.configure(lock.password("4821",kind="pin"));assertEquals("pin",lock.settings.mode)
            assertTrue(lock.unlock("4821"));assertFalse(lock.unlock("4822"))
            assertFails {lock.password("12a4",kind="pin")};assertFails {lock.password("123",kind="pin")};assertFails {lock.password("1".repeat(13),kind="pin")}
            lock.configure(lock.password("anna"+LOGIN_SEPARATOR+"secret1",kind="login"));assertEquals("login",lock.settings.mode)
            assertTrue(lock.unlock("anna"+LOGIN_SEPARATOR+"secret1"));assertFalse(lock.unlock("Anna"+LOGIN_SEPARATOR+"secret1"));assertFalse(lock.unlock("secret1"))
            assertFails {lock.password(LOGIN_SEPARATOR+"secret1",kind="login")};assertFails {lock.password("anna"+LOGIN_SEPARATOR+"abc",kind="login")}
            lock.configure(lock.password("0,1,2,5",pattern=true));assertEquals("pattern",lock.settings.mode);assertTrue(lock.unlock("0,1,2,5"))
            lock.configure(lock.password("plainpass"));assertEquals("password",lock.settings.mode);assertTrue(lock.unlock("plainpass"))
            // Settings saved before the kind existed.
            assertEquals("pattern",LockSettings(pattern=true).mode);assertEquals("password",LockSettings().mode)
            assertTrue(DeviceLock(platform).settings.enabled,"Stored with the device secrets and read back")
        }
    }
    @Test fun settingsTitlesAndFullScreenPageRender() {
        assertEquals("Профиль и настройки",settingsTitle(""));assertEquals("Теги",settingsTitle("tags"))
        listOf(400 to 800,1000 to 700).forEach {(width,height)->
            val scene=ImageComposeScene(width,height,Density(1f)){CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {
                SettingsPage(onDismissRequest={},title={Text("Защита и восстановление")},text={androidx.compose.foundation.layout.Column {SettingsGroup("Способ входа");Text("PIN-код");SettingsGroup("Восстановление доступа");Text("Контрольный вопрос")}},confirmButton={androidx.compose.material3.Button(onClick={}){Text("Сохранить")}},dismissButton={androidx.compose.material3.TextButton(onClick={}){Text("Отмена")}})
            }}}
            try {scene.render().close();val image=scene.render();java.io.File("build/review/settings-page-$width.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}finally {scene.close()}
        }
    }
    @Test fun settingsMenuRowsAndPinPadRender() {
        assertEquals(settingIcons.keys,setOf("personal","appearance","general","reminders","permissions","tags","security","trash","sync","data","help"))
        var typed="12"
        val scene=ImageComposeScene(400,900,Density(1f)){CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {androidx.compose.material3.Surface {androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(12.dp),verticalArrangement=androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            SettingsMenu(""){}
            PinPad(typed,{typed=it})
        }}}}}
        try {scene.render().close();val image=scene.render();java.io.File("build/review/settings-menu.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}finally {scene.close()}
    }
    @Test fun pinSubmitsItselfAtItsLengthAndLockScreensRender() {
        withPlatform {platform->
            val lock=DeviceLock(platform)
            assertEquals(0,lock.password("plainpass").pinLength)
            lock.configure(lock.password("4821",kind="pin"));assertEquals(4,lock.settings.pinLength)
            fun completions(value:String,expected:Int?):Int {var done=0
                val scene=ImageComposeScene(360,520,Density(1f)){MaterialTheme {PinPad(value,{},expectedLength=expected){done++}}}
                try {scene.render().close();scene.render(20_000_000).close()}finally {scene.close()};return done}
            assertEquals(1,completions("4821",4),"The last digit submits");assertEquals(0,completions("482",4));assertEquals(0,completions("4821",null),"Unknown length waits for Enter")
            fun shot(name:String,height:Int,content:@androidx.compose.runtime.Composable ()->Unit) {
                val scene=ImageComposeScene(360,height,Density(1f)){CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {content()}}}
                try {scene.render().close();val image=scene.render(20_000_000);java.io.File("build/review/$name.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}finally {scene.close()}
            }
            shot("lock-gate",720){LockGate(lock){}}
            shot("lock-settings-on",720){LockConfiguration(lock){}}
            lock.configure(lock.settings.copy(enabled=false))
            shot("lock-settings-off",900){LockConfiguration(lock){}}
        }
    }
}
