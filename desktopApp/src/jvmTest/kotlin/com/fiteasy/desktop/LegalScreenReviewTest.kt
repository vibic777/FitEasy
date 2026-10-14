package com.fiteasy.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Density
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import com.fiteasy.shared.*
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class LegalScreenReviewTest {
    @Test fun dualNavigationRoutesPointerToSameSelection(){
        listOf(360,960).forEach {width->
            val selected=mutableStateOf("today")
            val scene=ImageComposeScene(width,640,Density(1f)){
                CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {
                    Column(Modifier.fillMaxSize()){
                        MainNavigationIcons(selected.value,onSelect={selected.value=it})
                        Row(Modifier.weight(1f)){
                            Spacer(Modifier.weight(1f))
                            MainNavigationIcons(selected.value,vertical=true,onSelect={selected.value=it})
                        }
                    }
                }}
            }
            try {
                scene.render().close()
                val point=Offset(width-24f,332f)
                scene.sendPointerEvent(PointerEventType.Press,point)
                scene.sendPointerEvent(PointerEventType.Release,point)
                assertEquals("notes",selected.value)
                val image=scene.render();val file=java.io.File("build/review/navigation-$width.png");file.parentFile.mkdirs();file.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()
            }finally {scene.close()}
        }
    }
    @Test fun renderAllTenLegalScreensAtPhoneWidth(){
        languageNames.keys.forEach {language->
            val scene=ImageComposeScene(360,640,Density(1f)){
                CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {LegalNoticeScreen(language,{}, {true})}}
            }
            try {
                val image=scene.render()
                assertEquals(360,image.width);assertEquals(640,image.height)
                if(language in setOf("ru","en","zh")){
                    val file=java.io.File("build/review/legal-$language.png");file.parentFile.mkdirs()
                    file.writeBytes(requireNotNull(image.encodeToData()).bytes)
                }
                image.close()
            }finally {scene.close()}
        }
    }
}
