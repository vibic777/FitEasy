package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlin.test.*

class TrayMenuTest {
    @Test fun tenLanguagesAndEveryDestination() {
        languageNames.keys.forEach {language->trayActions.forEach {action->assertTrue(trayLabel(action,language).isNotBlank())}}
        assertEquals("Настройки",trayLabel("settings","ru"))
        assertEquals(trayLabel("help","en"),trayLabel("help","unknown"))
        trayActions.filter {it !in setOf("open","active","exit","silence")}.forEach {assertNotNull(appDestinations[it])}
        assertEquals("notes",appDestinations.getValue("notes").screen)
        assertEquals("reminder-status",appDestinations.getValue("reminders").dialog)
        assertEquals("help",appDestinations.getValue("help").dialog)
        assertEquals("about",appDestinations.getValue("about").dialog)
    }
    @Test fun repeatedRequestsHaveSeparateTokens() {
        val first=AppNavigation(destination="water");val next=AppNavigation(destination="water")
        assertNotEquals(first.token,next.token)
        assertEquals(first.destination,next.destination)
    }
    @Test fun activeEpisodesExcludeCompletedAndInterrupted() {
        val states=listOf("awaiting","delivered","snoozed","completed","interrupted").associateWith {ReminderDelivery(status=it)}
        assertEquals(listOf("awaiting","delivered","snoozed"),activeTrayReminders(states))
    }
}
