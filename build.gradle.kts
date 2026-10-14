import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    kotlin("multiplatform") version "2.4.20" apply false
    id("org.jetbrains.compose") version "1.12.1" apply false
}

val releaseInfo=Properties().apply {rootProject.file("version.properties").inputStream().use {load(it)}}
tasks.register("generateBuildInfo") {
    inputs.file("version.properties")
    val target=layout.buildDirectory.file("generated/buildInfo/kotlin/com/fiteasy/shared/BuildInfo.kt")
    outputs.file(target)
    doLast {
        val version=releaseInfo.getProperty("version")
        val number=releaseInfo.getProperty("build").toInt()
        val date=releaseInfo.getProperty("date")
        require(version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")) && number>0 && date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")))
        target.get().asFile.apply {parentFile.mkdirs();writeText("""
            package com.fiteasy.shared
            object BuildInfo {
                const val VERSION = "$version"
                const val NUMBER = $number
                const val DATE = "$date"
                const val CONTRIBUTOR = "vibic777"
            }
        """.trimIndent()+"\n")}
    }
}

