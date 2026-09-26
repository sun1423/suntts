package com.brahmadeo.supertonic.tts.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupertonicTextToSpeechServiceTest {

    @Test
    fun parseVoiceFileName_sanitizesAndAppendsJson() {
        assertEquals("F3.json", SupertonicTextToSpeechService.parseVoiceFileName("en-supertonic-F3"))
        assertEquals("evil.json", SupertonicTextToSpeechService.parseVoiceFileName("en-supertonic-../../evil"))
    }

    @Test
    fun parseVoiceFileName_returnsNullForInvalidInput() {
        assertNull(SupertonicTextToSpeechService.parseVoiceFileName(null))
        assertNull(SupertonicTextToSpeechService.parseVoiceFileName("invalid"))
    }

    @Test
    fun resolveLanguageForRequest_prefersVoicePrefix() {
        assertEquals("fr", SupertonicTextToSpeechService.resolveLanguageForRequest("en", "fr-supertonic-F1"))
        assertEquals("en", SupertonicTextToSpeechService.resolveLanguageForRequest("eng", null))
    }
}
