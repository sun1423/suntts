package com.brahmadeo.supertonic.tts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.ArrayList

/**
 * Activity that handles the CHECK_TTS_DATA intent.
 * This is required by some apps (like Tasker) to verify that the TTS engine is functional
 * and to discover which languages are supported.
 */
class CheckDataActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val availableVoices = ArrayList<String>()
        val unavailableVoices = ArrayList<String>()

        if (com.brahmadeo.supertonic.tts.utils.AssetManager.isVersionReady(this, "v1")) {
            availableVoices.add("eng-USA")
        } else {
            unavailableVoices.add("eng-USA")
        }

        val v2Languages = listOf("kor-KOR", "spa-ESP", "por-PRT", "fra-FRA")
        if (com.brahmadeo.supertonic.tts.utils.AssetManager.isVersionReady(this, "v2")) {
            availableVoices.addAll(v2Languages)
        } else {
            unavailableVoices.addAll(v2Languages)
        }

        val v3Languages = listOf(
            "jpn-JPN", "ara-ARA", "bul-BGR", "ces-CZE", "dan-DNK", "deu-DEU",
            "ell-GRC", "est-EST", "fin-FIN", "hin-IND", "hrv-HRV", "hun-HUN",
            "ind-IDN", "ita-ITA", "lit-LTU", "lav-LVA", "nld-NLD", "pol-POL",
            "ron-ROU", "rus-RUS", "slk-SVK", "slv-SVN", "swe-SWE", "tur-TUR",
            "ukr-UKR", "vie-VNM"
        )
        if (com.brahmadeo.supertonic.tts.utils.AssetManager.isVersionReady(this, "v3")) {
            availableVoices.addAll(v3Languages)
        } else {
            unavailableVoices.addAll(v3Languages)
        }

        val result = if (availableVoices.isNotEmpty()) {
            TextToSpeech.Engine.CHECK_VOICE_DATA_PASS
        } else {
            TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL
        }

        val returnIntent = Intent()
        returnIntent.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, availableVoices)
        returnIntent.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, unavailableVoices)
        
        setResult(result, returnIntent)
        finish()
    }
}
