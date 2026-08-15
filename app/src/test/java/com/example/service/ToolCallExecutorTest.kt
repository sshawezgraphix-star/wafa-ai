package com.example.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppSettingsManager
import com.example.model.AssistantState
import com.example.model.ToolCategory
import com.example.model.VoiceMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ToolCallExecutorTest {

    private lateinit var context: Context
    private lateinit var settingsManager: AppSettingsManager
    private lateinit var executor: ToolCallExecutor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = AppSettingsManager(context)
        executor = ToolCallExecutor(context, settingsManager)
    }

    @Test
    fun testSearchGoogleExecution() {
        val result = executor.executeTool("searchGoogle", "{\"query\":\"artificial intelligence trends\"}")
        assertTrue(result.contains("Searched Google for 'artificial intelligence trends'"))
    }

    @Test
    fun testResearchTopicExecution() {
        val result = executor.executeTool("researchTopic", "{\"topic\":\"quantum computing\"}")
        assertTrue(result.contains("Conducted deep web research on 'quantum computing'"))
    }

    @Test
    fun testOpenWebsiteExecution() {
        val result = executor.executeTool("openWebsite", "{\"url\":\"https://gemini.google.com\"}")
        assertTrue(result.contains("Successfully opened https://gemini.google.com"))
    }

    @Test
    fun testOpenYouTubeExecution() {
        val result = executor.executeTool("openYouTube", "{\"query\":\"Iron Man Jarvis HUD\"}")
        assertTrue(result.contains("Searching YouTube for 'Iron Man Jarvis HUD'"))
    }

    @Test
    fun testMakePhoneCallExecution() {
        val result = executor.executeTool("makePhoneCall", "{\"phoneNumber\":\"+1234567890\", \"contactName\":\"Boss\"}")
        assertTrue(result.contains("Calling +1234567890 (Boss)"))
    }

    @Test
    fun testSendWhatsAppMessageExecution() {
        val result = executor.executeTool(
            "sendWhatsAppMessage",
            "{\"phoneNumber\":\"919876543210\", \"message\":\"Hello Shawez, Maya AI is online!\"}"
        )
        assertTrue(result.contains("Opened WhatsApp with message for +919876543210"))
    }

    @Test
    fun testSendSmsExecution() {
        val result = executor.executeTool(
            "sendSms",
            "{\"phoneNumber\":\"12345\", \"message\":\"Meeting in 10 mins\"}"
        )
        assertTrue(result.contains("Prepared SMS to '12345'"))
    }

    @Test
    fun testAlarmAndTimerExecution() {
        val alarmResult = executor.executeTool(
            "setAlarm",
            "{\"hour\":7, \"minutes\":30, \"message\":\"Morning Routine\"}"
        )
        assertTrue(alarmResult.contains("Alarm set for 07:30"))

        val timerResult = executor.executeTool(
            "setTimer",
            "{\"seconds\":180, \"message\":\"Boil Eggs\"}"
        )
        assertTrue(timerResult.contains("Timer started for 3 min 0 sec"))
    }

    @Test
    fun testAppAndMusicExecution() {
        val appResult = executor.executeTool("openApp", "{\"appName\":\"Camera\"}")
        assertTrue(appResult.contains("Camera"))

        val musicResult = executor.executeTool("playMusic", "{\"query\":\"Interstellar Theme\"}")
        assertTrue(musicResult.contains("Interstellar Theme"))
    }

    @Test
    fun testMapsAndNotesExecution() {
        val mapsResult = executor.executeTool("openMaps", "{\"location\":\"Taj Mahal\"}")
        assertTrue(mapsResult.contains("Taj Mahal"))

        val noteResult = executor.executeTool("takeNote", "{\"content\":\"Buy fresh coffee beans\"}")
        assertTrue(noteResult.contains("Saved note: \"Buy fresh coffee beans\""))
        val notes = settingsManager.getNotes()
        assertTrue(notes.any { it.text == "Buy fresh coffee beans" })
    }

    @Test
    fun testDeviceTimeAndDateExecution() {
        val dateResult = executor.executeTool("getDeviceTimeAndDate", "{}")
        assertTrue(dateResult.contains("Current Device Time:"))
    }

    @Test
    fun testVoiceSettingsPersistence() {
        settingsManager.setApiKey("AIzaSyTestKey12345")
        assertEquals("AIzaSyTestKey12345", settingsManager.getApiKey())

        settingsManager.setSelectedVoice("Aoede")
        assertEquals("Aoede", settingsManager.getSelectedVoice())

        settingsManager.setAssistantName("Maya")
        assertEquals("Maya", settingsManager.getAssistantName())
    }

    @Test
    fun testVoiceMessageModel() {
        val msg = VoiceMessage(
            sender = VoiceMessage.Sender.ASSISTANT,
            text = "Haan Shawez, Maya AI ready hai!"
        )
        assertEquals(VoiceMessage.Sender.ASSISTANT, msg.sender)
        assertEquals("Haan Shawez, Maya AI ready hai!", msg.text)
        assertNotNull(msg.id)
    }
}
